#!/usr/bin/env python3
"""
Remote Config Consistency Validator for SplitTrip.

Validates that:
  1. All keys in remote_config_defaults.xml exist in firebase/remoteconfig.template.json.
  2. All keys in firebase/remoteconfig.template.json exist in remote_config_defaults.xml.
  3. Default values match between XML and template JSON.
  4. Every parameter specifies a valid valueType (STRING, BOOLEAN, NUMBER, JSON) and non-empty description.
  5. Every parameterGroup specifies a non-empty description.
  6. Parameter values conform to declared valueType.
  7. Top-level conditions (when present) have unique non-empty names, non-empty expressions, and valid optional tagColors.
  8. Parameter conditionalValues reference declared conditions and conform to their parameter's valueType.

Usage:
    python3 scripts/validate_remote_config.py
"""

import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List, Set, Tuple

RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[1;33m"
CYAN = "\033[0;36m"
NC = "\033[0m"

VALID_VALUE_TYPES = {"STRING", "BOOLEAN", "NUMBER", "JSON"}


def resolve_paths() -> Tuple[Path, Path]:
    script_dir = Path(__file__).resolve().parent
    root_dir = script_dir.parent
    xml_path = root_dir / "data/firebase/src/main/res/xml/remote_config_defaults.xml"
    json_path = root_dir / "firebase/remoteconfig.template.json"
    return xml_path, json_path


def parse_xml_defaults(xml_path: Path) -> Dict[str, str]:
    if not xml_path.exists():
        raise FileNotFoundError(f"XML defaults file not found: {xml_path}")
    tree = ET.parse(xml_path)
    root = tree.getroot()
    defaults: Dict[str, str] = {}
    for entry in root.findall("entry"):
        key_elem = entry.find("key")
        val_elem = entry.find("value")
        if key_elem is not None and key_elem.text:
            key = key_elem.text.strip()
            val = val_elem.text if val_elem is not None and val_elem.text is not None else ""
            defaults[key] = val
    return defaults


def parse_conditions(data: Dict[str, Any]) -> Tuple[Set[str], List[str]]:
    condition_names: Set[str] = set()
    errors: List[str] = []

    if "conditions" not in data:
        return condition_names, errors

    raw_conditions = data.get("conditions")
    if not isinstance(raw_conditions, list):
        errors.append("Top-level 'conditions' must be a list.")
        return condition_names, errors

    for idx, cond in enumerate(raw_conditions):
        if not isinstance(cond, dict):
            errors.append(f"Condition at index {idx} must be an object.")
            continue

        name = cond.get("name")
        if not isinstance(name, str) or not name.strip():
            errors.append(f"Condition at index {idx} is missing a non-empty string 'name'.")
        elif name in condition_names:
            errors.append(f"Duplicate condition name '{name}' at index {idx}.")
        else:
            condition_names.add(name)

        expression = cond.get("expression")
        if not isinstance(expression, str) or not expression.strip():
            cond_label = f"'{name}'" if isinstance(name, str) and name.strip() else f"at index {idx}"
            errors.append(f"Condition {cond_label} is missing a non-empty string 'expression'.")

        if "tagColor" in cond:
            tag_color = cond.get("tagColor")
            if not isinstance(tag_color, str) or not tag_color.strip():
                cond_label = f"'{name}'" if isinstance(name, str) and name.strip() else f"at index {idx}"
                errors.append(f"Condition {cond_label} has invalid 'tagColor': must be a non-empty string.")

    return condition_names, errors


def parse_json_template(
    json_path: Path,
) -> Tuple[Dict[str, Dict[str, Any]], Dict[str, str], Set[str], List[str]]:
    if not json_path.exists():
        raise FileNotFoundError(f"Remote Config template file not found: {json_path}")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    parameters: Dict[str, Dict[str, Any]] = {}
    group_descriptions: Dict[str, str] = {}
    condition_names, condition_errors = parse_conditions(data)

    # Root-level parameters (if any)
    root_params = data.get("parameters", {})
    if isinstance(root_params, dict):
        for k, v in root_params.items():
            parameters[k] = v

    # Group-level parameters
    groups = data.get("parameterGroups", {})
    if isinstance(groups, dict):
        for group_name, group_data in groups.items():
            if isinstance(group_data, dict):
                group_descriptions[group_name] = group_data.get("description", "")
                group_params = group_data.get("parameters", {})
                if isinstance(group_params, dict):
                    for k, v in group_params.items():
                        parameters[k] = v

    return parameters, group_descriptions, condition_names, condition_errors


def validate_value_conformance(key: str, value_type: str, val_str: str) -> List[str]:
    errors: List[str] = []
    if value_type == "BOOLEAN":
        if val_str.lower() not in {"true", "false"}:
            errors.append(f"Parameter '{key}' has valueType BOOLEAN but value is not 'true' or 'false': '{val_str}'")
    elif value_type == "NUMBER":
        try:
            float(val_str)
        except ValueError:
            errors.append(f"Parameter '{key}' has valueType NUMBER but value is not numeric: '{val_str}'")
    elif value_type == "JSON":
        try:
            json.loads(val_str)
        except Exception as e:
            errors.append(f"Parameter '{key}' has valueType JSON but value is not valid JSON: {e}")
    elif value_type == "STRING":
        if not isinstance(val_str, str):
            errors.append(f"Parameter '{key}' has valueType STRING but value is not a string.")
    return errors


def validate_consistency() -> int:
    xml_path, json_path = resolve_paths()
    errors: List[str] = []

    print(f"{CYAN}🔍 Validating Remote Config consistency...{NC}")
    print(f"   XML:  {xml_path}")
    print(f"   JSON: {json_path}")

    try:
        xml_defaults = parse_xml_defaults(xml_path)
    except Exception as e:
        print(f"{RED}❌ Error reading XML defaults: {e}{NC}")
        return 1

    try:
        template_params, group_descriptions, declared_condition_names, condition_errors = parse_json_template(
            json_path
        )
    except Exception as e:
        print(f"{RED}❌ Error reading JSON template: {e}{NC}")
        return 1

    errors.extend(condition_errors)

    xml_keys: Set[str] = set(xml_defaults.keys())
    template_keys: Set[str] = set(template_params.keys())

    # 1. Parameter Groups validation
    if not group_descriptions:
        errors.append("No parameter groups found in JSON template.")
    for group_name, desc in group_descriptions.items():
        if not desc or not desc.strip():
            errors.append(f"Parameter group '{group_name}' is missing a non-empty description.")

    # 2. Key parity validation
    missing_in_template = xml_keys - template_keys
    if missing_in_template:
        errors.append(f"Keys present in XML defaults but missing in JSON template: {sorted(missing_in_template)}")

    extra_in_template = template_keys - xml_keys
    if extra_in_template:
        errors.append(f"Keys present in JSON template but missing in XML defaults: {sorted(extra_in_template)}")

    # 3. Parameter metadata and value validation
    for key, param_data in template_params.items():
        if not isinstance(param_data, dict):
            errors.append(f"Parameter '{key}' definition must be an object.")
            continue

        desc = param_data.get("description", "")
        if not desc or not desc.strip():
            errors.append(f"Parameter '{key}' is missing a non-empty description.")

        val_type = param_data.get("valueType", "")
        if val_type not in VALID_VALUE_TYPES:
            errors.append(
                f"Parameter '{key}' has invalid valueType '{val_type}'. Must be one of: {sorted(VALID_VALUE_TYPES)}"
            )

        default_val_obj = param_data.get("defaultValue")
        if not isinstance(default_val_obj, dict) or "value" not in default_val_obj:
            errors.append(f"Parameter '{key}' is missing defaultValue.value.")
        else:
            tmpl_val_str = str(default_val_obj["value"])
            errors.extend(validate_value_conformance(key, val_type, tmpl_val_str))

            # Value parity with XML
            if key in xml_defaults:
                xml_val_str = xml_defaults[key]
                if val_type == "JSON":
                    try:
                        xml_json = json.loads(xml_val_str)
                        tmpl_json = json.loads(tmpl_val_str)
                        if xml_json != tmpl_json:
                            errors.append(f"Value mismatch for JSON parameter '{key}'.")
                    except Exception as e:
                        errors.append(f"Failed to compare JSON for parameter '{key}': {e}")
                else:
                    if xml_val_str.strip() != tmpl_val_str.strip():
                        errors.append(
                            f"Value mismatch for parameter '{key}':\n"
                            f"   XML:      '{xml_val_str.strip()}'\n"
                            f"   Template: '{tmpl_val_str.strip()}'"
                        )

        # Conditional values validation
        if "conditionalValues" in param_data:
            cond_values = param_data.get("conditionalValues")
            if not isinstance(cond_values, dict):
                errors.append(f"Parameter '{key}' conditionalValues must be an object.")
            else:
                for condition_name, cond_val_obj in cond_values.items():
                    if condition_name not in declared_condition_names:
                        errors.append(
                            f"Parameter '{key}' conditionalValues references undeclared condition '{condition_name}'."
                        )
                    if (
                        not isinstance(cond_val_obj, dict)
                        or "value" not in cond_val_obj
                        or cond_val_obj["value"] is None
                    ):
                        errors.append(
                            f"Parameter '{key}' [condition '{condition_name}'] is missing non-null 'value'."
                        )
                    else:
                        errors.extend(
                            validate_value_conformance(
                                f"{key} [condition '{condition_name}']",
                                val_type,
                                str(cond_val_obj["value"]),
                            )
                        )

    if errors:
        print(f"\n{RED}❌ Remote Config consistency validation FAILED with {len(errors)} error(s):{NC}")
        for err in errors:
            print(f"   {RED}• {err}{NC}")
        return 1

    param_count = len(template_keys)
    group_count = len(group_descriptions)
    condition_count = len(declared_condition_names)
    print(
        f"\n{GREEN}✅ Remote Config consistency validation PASSED: "
        f"{param_count} parameters across {group_count} groups and {condition_count} conditions are synchronized.{NC}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(validate_consistency())
