#!/usr/bin/env python3
"""Additive, live-preserving merge for Firebase Remote Config templates.

Merges a version-controlled repository template into a live Firebase Remote
Config template so that:
- New parameters and conditions from git are added automatically.
- Existing parameters preserve their live ``defaultValue`` and
  ``conditionalValues`` while receiving updated ``description``,
  ``valueType``, and ``parameterGroups`` placement from git.
- Existing live ``conditions`` and live-only parameters are preserved.
"""

from __future__ import annotations

import argparse
import copy
from dataclasses import dataclass
import json
from pathlib import Path
import sys
from typing import Any

DEFAULT_REPO_TEMPLATE = Path("firebase/remoteconfig.template.json")
EPHEMERAL_KEYS = ("version", "etag")


@dataclass(frozen=True)
class MergeStats:
    added_parameters: tuple[str, ...]
    updated_parameters: tuple[str, ...]
    preserved_live_only_parameters: tuple[str, ...]
    added_conditions: tuple[str, ...]
    preserved_conditions: tuple[str, ...]
    used_empty_live_fallback: bool


@dataclass(frozen=True)
class MergeResult:
    template: dict[str, Any]
    stats: MergeStats


def _strip_ephemeral_keys(template: dict[str, Any]) -> dict[str, Any]:
    cleaned = copy.deepcopy(template)
    for key in EPHEMERAL_KEYS:
        cleaned.pop(key, None)
    return cleaned


def is_empty_template(template: dict[str, Any] | None) -> bool:
    if template is None:
        return True
    conditions = template.get("conditions")
    parameters = template.get("parameters")
    parameter_groups = template.get("parameterGroups")
    return not conditions and not parameters and not parameter_groups


def _index_parameters(
    template: dict[str, Any],
    source_label: str,
) -> dict[str, tuple[str | None, dict[str, Any]]]:
    index: dict[str, tuple[str | None, dict[str, Any]]] = {}

    root_params = template.get("parameters")
    if root_params is None:
        root_params = {}
    if not isinstance(root_params, dict):
        raise ValueError(f"{source_label} 'parameters' must be a JSON object.")

    for param_key, param_obj in root_params.items():
        if not isinstance(param_obj, dict):
            raise ValueError(
                f"{source_label} root parameter '{param_key}' must be a JSON object."
            )
        index[param_key] = (None, param_obj)

    param_groups = template.get("parameterGroups")
    if param_groups is None:
        param_groups = {}
    if not isinstance(param_groups, dict):
        raise ValueError(f"{source_label} 'parameterGroups' must be a JSON object.")

    for group_name, group_obj in param_groups.items():
        if not isinstance(group_obj, dict):
            raise ValueError(
                f"{source_label} parameter group '{group_name}' must be a JSON object."
            )
        group_params = group_obj.get("parameters")
        if group_params is None:
            group_params = {}
        if not isinstance(group_params, dict):
            raise ValueError(
                f"{source_label} group '{group_name}' 'parameters' must be a JSON object."
            )
        for param_key, param_obj in group_params.items():
            if not isinstance(param_obj, dict):
                raise ValueError(
                    f"{source_label} parameter '{param_key}' in group '{group_name}' "
                    "must be a JSON object."
                )
            if param_key in index:
                raise ValueError(
                    f"{source_label} defines duplicate parameter key '{param_key}'."
                )
            index[param_key] = (group_name, param_obj)

    return index


def _merge_conditions(
    live_conditions: list[dict[str, Any]],
    repo_conditions: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], tuple[str, ...], tuple[str, ...]]:
    if not isinstance(live_conditions, list):
        raise ValueError("live 'conditions' must be a JSON array.")
    if not isinstance(repo_conditions, list):
        raise ValueError("repo 'conditions' must be a JSON array.")

    merged: list[dict[str, Any]] = []
    seen_names: set[str] = set()
    preserved_names: list[str] = []
    added_names: list[str] = []

    for cond in live_conditions:
        if not isinstance(cond, dict) or not isinstance(cond.get("name"), str):
            raise ValueError("Each live condition must be a JSON object with a string 'name'.")
        name = cond["name"]
        merged.append(copy.deepcopy(cond))
        seen_names.add(name)
        preserved_names.append(name)

    for cond in repo_conditions:
        if not isinstance(cond, dict) or not isinstance(cond.get("name"), str):
            raise ValueError("Each repo condition must be a JSON object with a string 'name'.")
        name = cond["name"]
        if name not in seen_names:
            merged.append(copy.deepcopy(cond))
            seen_names.add(name)
            added_names.append(name)

    return merged, tuple(added_names), tuple(preserved_names)


def _merge_existing_parameter(
    live_param: dict[str, Any],
    repo_param: dict[str, Any],
) -> dict[str, Any]:
    merged_param: dict[str, Any] = {}

    if "defaultValue" in live_param:
        merged_param["defaultValue"] = copy.deepcopy(live_param["defaultValue"])
    elif "defaultValue" in repo_param:
        merged_param["defaultValue"] = copy.deepcopy(repo_param["defaultValue"])

    # Only carry over live conditionalValues; if Console removed them, do not re-inject repo's.
    live_cond_values = live_param.get("conditionalValues")
    if isinstance(live_cond_values, dict) and live_cond_values:
        merged_param["conditionalValues"] = copy.deepcopy(live_cond_values)

    if "description" in repo_param:
        merged_param["description"] = repo_param["description"]
    elif "description" in live_param:
        merged_param["description"] = live_param["description"]

    if "valueType" in repo_param:
        merged_param["valueType"] = repo_param["valueType"]
    elif "valueType" in live_param:
        merged_param["valueType"] = live_param["valueType"]

    return merged_param


def merge_templates(
    live_template: dict[str, Any] | None,
    repo_template: dict[str, Any],
) -> MergeResult:
    repo_clean = _strip_ephemeral_keys(repo_template)
    repo_index = _index_parameters(repo_clean, "repo")
    repo_conditions_raw = repo_clean.get("conditions") or []

    if is_empty_template(live_template):
        if not isinstance(repo_conditions_raw, list):
            raise ValueError("repo 'conditions' must be a JSON array.")
        repo_cond_names = tuple(
            cond["name"]
            for cond in repo_conditions_raw
            if isinstance(cond, dict) and isinstance(cond.get("name"), str)
        )
        repo_clean.setdefault("conditions", [])
        repo_clean.setdefault("parameters", {})
        repo_clean.setdefault("parameterGroups", {})
        return MergeResult(
            template=repo_clean,
            stats=MergeStats(
                added_parameters=tuple(repo_index.keys()),
                updated_parameters=(),
                preserved_live_only_parameters=(),
                added_conditions=repo_cond_names,
                preserved_conditions=(),
                used_empty_live_fallback=True,
            ),
        )

    assert live_template is not None
    live_clean = _strip_ephemeral_keys(live_template)
    live_index = _index_parameters(live_clean, "live")

    live_conditions_raw = live_clean.get("conditions") or []
    merged_conditions, added_conditions, preserved_conditions = _merge_conditions(
        live_conditions_raw,
        repo_conditions_raw,
    )

    merged_root_params: dict[str, dict[str, Any]] = {}
    merged_groups: dict[str, dict[str, Any]] = {}
    added_parameters: list[str] = []
    updated_parameters: list[str] = []
    preserved_live_only: list[str] = []

    repo_root_params = repo_clean.get("parameters") or {}
    for param_key, repo_param in repo_root_params.items():
        if param_key in live_index:
            _, live_param = live_index[param_key]
            merged_root_params[param_key] = _merge_existing_parameter(live_param, repo_param)
            updated_parameters.append(param_key)
        else:
            merged_root_params[param_key] = copy.deepcopy(repo_param)
            added_parameters.append(param_key)

    live_groups = live_clean.get("parameterGroups") or {}
    repo_groups = repo_clean.get("parameterGroups") or {}

    for group_name, repo_group in repo_groups.items():
        live_group = live_groups.get(group_name) if isinstance(live_groups.get(group_name), dict) else {}
        description = repo_group.get("description")
        if not description:
            description = live_group.get("description", "")

        group_params_out: dict[str, dict[str, Any]] = {}
        repo_group_params = repo_group.get("parameters") or {}
        for param_key, repo_param in repo_group_params.items():
            if param_key in live_index:
                _, live_param = live_index[param_key]
                group_params_out[param_key] = _merge_existing_parameter(live_param, repo_param)
                updated_parameters.append(param_key)
            else:
                group_params_out[param_key] = copy.deepcopy(repo_param)
                added_parameters.append(param_key)

        merged_groups[group_name] = {
            "description": description,
            "parameters": group_params_out,
        }

    # Preserve live-only parameters in their original live locations.
    for param_key, (live_group_name, live_param) in live_index.items():
        if param_key in repo_index:
            continue
        preserved_live_only.append(param_key)
        if live_group_name is None:
            merged_root_params[param_key] = copy.deepcopy(live_param)
        else:
            if live_group_name not in merged_groups:
                live_group_obj = live_groups.get(live_group_name, {})
                merged_groups[live_group_name] = {
                    "description": live_group_obj.get("description", ""),
                    "parameters": {},
                }
            merged_groups[live_group_name]["parameters"][param_key] = copy.deepcopy(live_param)

    merged_template: dict[str, Any] = {
        "conditions": merged_conditions,
        "parameters": merged_root_params,
        "parameterGroups": merged_groups,
    }

    return MergeResult(
        template=merged_template,
        stats=MergeStats(
            added_parameters=tuple(added_parameters),
            updated_parameters=tuple(updated_parameters),
            preserved_live_only_parameters=tuple(preserved_live_only),
            added_conditions=added_conditions,
            preserved_conditions=preserved_conditions,
            used_empty_live_fallback=False,
        ),
    )


def load_json_file(path: Path, *, allow_empty_file: bool = False) -> dict[str, Any] | None:
    if not path.exists():
        if allow_empty_file:
            return None
        raise FileNotFoundError(f"Template file not found: {path}")

    raw_text = path.read_text(encoding="utf-8")
    if not raw_text.strip():
        if allow_empty_file:
            return None
        raise ValueError(f"Template file is empty: {path}")

    parsed = json.loads(raw_text)
    if not isinstance(parsed, dict):
        raise ValueError(f"Template file must contain a JSON object: {path}")
    return parsed


def _parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Merge git Remote Config template into live Firebase Remote Config template."
    )
    parser.add_argument(
        "--live",
        type=Path,
        required=True,
        help="Path to the fetched live Remote Config template JSON.",
    )
    parser.add_argument(
        "--repo",
        type=Path,
        default=DEFAULT_REPO_TEMPLATE,
        help="Path to the repository Remote Config template JSON.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        required=True,
        help="Path to write the merged Remote Config template JSON.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = _parse_args(argv)

    try:
        live_template = load_json_file(args.live, allow_empty_file=True)
        repo_template = load_json_file(args.repo, allow_empty_file=False)
        assert repo_template is not None
        result = merge_templates(live_template, repo_template)
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        print(f"Error merging Remote Config templates: {exc}", file=sys.stderr)
        return 1

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(result.template, indent=2) + "\n",
        encoding="utf-8",
    )

    stats = result.stats
    if stats.used_empty_live_fallback:
        print("Live Remote Config template is empty/uninitialized; using repository template.")
    print(
        "Remote Config merge summary: "
        f"added_params={len(stats.added_parameters)}, "
        f"updated_params={len(stats.updated_parameters)}, "
        f"preserved_live_only_params={len(stats.preserved_live_only_parameters)}, "
        f"added_conditions={len(stats.added_conditions)}, "
        f"preserved_conditions={len(stats.preserved_conditions)}"
    )
    if stats.preserved_live_only_parameters:
        preserved_list = ", ".join(stats.preserved_live_only_parameters)
        print(f"Notice: preserved live-only parameters not in git template: {preserved_list}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
