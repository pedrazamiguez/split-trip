#!/usr/bin/env python3
"""Unit tests for scripts/merge_remote_config.py."""

from __future__ import annotations

import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))

from merge_remote_config import main, merge_templates


class TestMergeRemoteConfig(unittest.TestCase):
    def test_empty_live_template_falls_back_to_repo(self) -> None:
        repo = {
            "version": {"versionNumber": "12"},
            "etag": "etag-repo",
            "conditions": [
                {
                    "name": "Pre-1.0.0 Closed Test",
                    "expression": "app.build.<(['1000000'])",
                    "tagColor": "INDIGO",
                }
            ],
            "parameters": {"root_flag": {"defaultValue": {"value": "false"}, "valueType": "BOOLEAN"}},
            "parameterGroups": {
                "Subscriptions": {
                    "description": "Sub settings",
                    "parameters": {
                        "subscription_gating_enabled": {
                            "defaultValue": {"value": "true"},
                            "valueType": "BOOLEAN",
                        }
                    },
                }
            },
        }

        result = merge_templates({}, repo)

        self.assertTrue(result.stats.used_empty_live_fallback)
        self.assertEqual(
            set(result.stats.added_parameters),
            {"root_flag", "subscription_gating_enabled"},
        )
        self.assertEqual(result.stats.added_conditions, ("Pre-1.0.0 Closed Test",))
        self.assertNotIn("version", result.template)
        self.assertNotIn("etag", result.template)
        self.assertEqual(
            result.template["parameterGroups"]["Subscriptions"]["parameters"][
                "subscription_gating_enabled"
            ]["defaultValue"]["value"],
            "true",
        )

    def test_none_live_template_falls_back_to_repo(self) -> None:
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "General": {
                    "description": "General",
                    "parameters": {
                        "support_email": {
                            "defaultValue": {"value": "support@splittrip.eu"},
                            "valueType": "STRING",
                        }
                    },
                }
            },
        }

        result = merge_templates(None, repo)

        self.assertTrue(result.stats.used_empty_live_fallback)
        self.assertEqual(result.stats.added_parameters, ("support_email",))
        self.assertEqual(
            result.template["parameterGroups"]["General"]["parameters"]["support_email"][
                "defaultValue"
            ]["value"],
            "support@splittrip.eu",
        )

    def test_new_parameter_added_under_group(self) -> None:
        live = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Subscriptions": {
                    "description": "Old desc",
                    "parameters": {
                        "existing_flag": {
                            "defaultValue": {"value": "false"},
                            "description": "Existing",
                            "valueType": "BOOLEAN",
                        }
                    },
                }
            },
        }
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Subscriptions": {
                    "description": "New desc",
                    "parameters": {
                        "existing_flag": {
                            "defaultValue": {"value": "true"},
                            "description": "Existing updated",
                            "valueType": "BOOLEAN",
                        },
                        "new_flag": {
                            "defaultValue": {"value": "true"},
                            "conditionalValues": {"Closed Test": {"value": "false"}},
                            "description": "Brand new flag",
                            "valueType": "BOOLEAN",
                        },
                    },
                }
            },
        }

        result = merge_templates(live, repo)

        self.assertFalse(result.stats.used_empty_live_fallback)
        self.assertEqual(result.stats.added_parameters, ("new_flag",))
        self.assertEqual(result.stats.updated_parameters, ("existing_flag",))
        sub_params = result.template["parameterGroups"]["Subscriptions"]["parameters"]
        self.assertIn("new_flag", sub_params)
        self.assertEqual(sub_params["new_flag"]["defaultValue"]["value"], "true")
        self.assertEqual(
            sub_params["new_flag"]["conditionalValues"]["Closed Test"]["value"],
            "false",
        )

    def test_new_root_parameter_added(self) -> None:
        live = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {},
        }
        repo = {
            "conditions": [],
            "parameters": {
                "root_flag": {
                    "defaultValue": {"value": "42"},
                    "description": "Root numeric param",
                    "valueType": "NUMBER",
                }
            },
            "parameterGroups": {},
        }

        result = merge_templates(live, repo)

        self.assertEqual(result.stats.added_parameters, ("root_flag",))
        self.assertIn("root_flag", result.template["parameters"])
        self.assertEqual(result.template["parameters"]["root_flag"]["defaultValue"]["value"], "42")

    def test_existing_parameter_preserves_live_values_and_updates_metadata(self) -> None:
        live = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Advertising": {
                    "description": "Live ads desc",
                    "parameters": {
                        "ads_enabled": {
                            "defaultValue": {"value": "false"},
                            "conditionalValues": {"Pre-1.0.0 Closed Test": {"value": "false"}},
                            "description": "Old description",
                            "valueType": "BOOLEAN",
                        }
                    },
                }
            },
        }
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Advertising": {
                    "description": "Repo ads desc",
                    "parameters": {
                        "ads_enabled": {
                            "defaultValue": {"value": "true"},
                            "conditionalValues": {"Pre-1.0.0 Closed Test": {"value": "true"}},
                            "description": "Updated description from git",
                            "valueType": "BOOLEAN",
                        }
                    },
                }
            },
        }

        result = merge_templates(live, repo)

        ads_param = result.template["parameterGroups"]["Advertising"]["parameters"]["ads_enabled"]
        self.assertEqual(ads_param["defaultValue"]["value"], "false")
        self.assertEqual(
            ads_param["conditionalValues"]["Pre-1.0.0 Closed Test"]["value"],
            "false",
        )
        self.assertEqual(ads_param["description"], "Updated description from git")
        self.assertEqual(ads_param["valueType"], "BOOLEAN")

    def test_existing_parameter_does_not_reinject_removed_conditional_values(self) -> None:
        live = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Advertising": {
                    "description": "Ads",
                    "parameters": {
                        "ads_enabled": {
                            "defaultValue": {"value": "true"},
                            "description": "Old",
                            "valueType": "BOOLEAN",
                        }
                    },
                }
            },
        }
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Advertising": {
                    "description": "Ads",
                    "parameters": {
                        "ads_enabled": {
                            "defaultValue": {"value": "true"},
                            "conditionalValues": {"Pre-1.0.0 Closed Test": {"value": "false"}},
                            "description": "New",
                            "valueType": "BOOLEAN",
                        }
                    },
                }
            },
        }

        result = merge_templates(live, repo)

        ads_param = result.template["parameterGroups"]["Advertising"]["parameters"]["ads_enabled"]
        self.assertNotIn("conditionalValues", ads_param)

    def test_existing_parameter_moved_between_groups_without_duplication(self) -> None:
        live = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "OldGroup": {
                    "description": "Old group",
                    "parameters": {
                        "moved_param": {
                            "defaultValue": {"value": "custom_live"},
                            "description": "Old desc",
                            "valueType": "STRING",
                        }
                    },
                }
            },
        }
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "NewGroup": {
                    "description": "New group",
                    "parameters": {
                        "moved_param": {
                            "defaultValue": {"value": "git_default"},
                            "description": "New desc",
                            "valueType": "STRING",
                        }
                    },
                }
            },
        }

        result = merge_templates(live, repo)

        self.assertNotIn("OldGroup", result.template["parameterGroups"])
        new_group_params = result.template["parameterGroups"]["NewGroup"]["parameters"]
        self.assertIn("moved_param", new_group_params)
        self.assertEqual(new_group_params["moved_param"]["defaultValue"]["value"], "custom_live")
        self.assertEqual(new_group_params["moved_param"]["description"], "New desc")

    def test_group_description_falls_back_to_live_when_repo_empty(self) -> None:
        live = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Groups": {
                    "description": "Live group description",
                    "parameters": {
                        "default_currency_code": {
                            "defaultValue": {"value": "USD"},
                            "valueType": "STRING",
                        }
                    },
                }
            },
        }
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "Groups": {
                    "description": "",
                    "parameters": {
                        "default_currency_code": {
                            "defaultValue": {"value": "EUR"},
                            "valueType": "STRING",
                        }
                    },
                }
            },
        }

        result = merge_templates(live, repo)

        self.assertEqual(
            result.template["parameterGroups"]["Groups"]["description"],
            "Live group description",
        )

    def test_live_only_parameters_preserved_in_root_and_groups(self) -> None:
        live = {
            "conditions": [],
            "parameters": {
                "legacy_root": {
                    "defaultValue": {"value": "keep_me"},
                    "valueType": "STRING",
                }
            },
            "parameterGroups": {
                "LegacyGroup": {
                    "description": "Legacy group",
                    "parameters": {
                        "legacy_grouped": {
                            "defaultValue": {"value": "99"},
                            "valueType": "NUMBER",
                        }
                    },
                }
            },
        }
        repo = {
            "conditions": [],
            "parameters": {},
            "parameterGroups": {
                "General": {
                    "description": "General",
                    "parameters": {
                        "support_email": {
                            "defaultValue": {"value": "support@splittrip.eu"},
                            "valueType": "STRING",
                        }
                    },
                }
            },
        }

        result = merge_templates(live, repo)

        self.assertEqual(
            set(result.stats.preserved_live_only_parameters),
            {"legacy_root", "legacy_grouped"},
        )
        self.assertEqual(
            result.template["parameters"]["legacy_root"]["defaultValue"]["value"],
            "keep_me",
        )
        self.assertEqual(
            result.template["parameterGroups"]["LegacyGroup"]["parameters"]["legacy_grouped"][
                "defaultValue"
            ]["value"],
            "99",
        )

    def test_new_condition_added_and_existing_condition_preserved(self) -> None:
        live = {
            "conditions": [
                {
                    "name": "Pre-1.0.0 Closed Test",
                    "expression": "app.build.<(['2000000'])",
                    "tagColor": "PINK",
                }
            ],
            "parameters": {},
            "parameterGroups": {},
        }
        repo = {
            "conditions": [
                {
                    "name": "Pre-1.0.0 Closed Test",
                    "expression": "app.build.<(['1000000'])",
                    "tagColor": "INDIGO",
                },
                {
                    "name": "Spain Users",
                    "expression": "device.country in ['es']",
                    "tagColor": "TEAL",
                },
            ],
            "parameters": {},
            "parameterGroups": {},
        }

        result = merge_templates(live, repo)

        self.assertEqual(result.stats.preserved_conditions, ("Pre-1.0.0 Closed Test",))
        self.assertEqual(result.stats.added_conditions, ("Spain Users",))
        self.assertEqual(len(result.template["conditions"]), 2)
        self.assertEqual(
            result.template["conditions"][0]["expression"],
            "app.build.<(['2000000'])",
        )
        self.assertEqual(result.template["conditions"][0]["tagColor"], "PINK")
        self.assertEqual(result.template["conditions"][1]["name"], "Spain Users")

    def test_ephemeral_version_and_etag_stripped(self) -> None:
        live = {
            "version": {"versionNumber": "42"},
            "etag": "live-etag",
            "conditions": [],
            "parameters": {"a": {"defaultValue": {"value": "1"}, "valueType": "NUMBER"}},
            "parameterGroups": {},
        }
        repo = {
            "version": {"versionNumber": "1"},
            "etag": "repo-etag",
            "conditions": [],
            "parameters": {"a": {"defaultValue": {"value": "2"}, "valueType": "NUMBER"}},
            "parameterGroups": {},
        }

        result = merge_templates(live, repo)

        self.assertNotIn("version", result.template)
        self.assertNotIn("etag", result.template)

    def test_duplicate_parameter_key_raises_value_error(self) -> None:
        bad_repo = {
            "conditions": [],
            "parameters": {"dup_key": {"defaultValue": {"value": "1"}}},
            "parameterGroups": {
                "General": {
                    "description": "General",
                    "parameters": {"dup_key": {"defaultValue": {"value": "2"}}},
                }
            },
        }

        with self.assertRaises(ValueError):
            merge_templates({}, bad_repo)

    def test_cli_writes_merged_json(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            live_file = tmp_path / "live.json"
            repo_file = tmp_path / "repo.json"
            output_file = tmp_path / "out.json"

            live_file.write_text(
                json.dumps(
                    {
                        "conditions": [],
                        "parameters": {},
                        "parameterGroups": {
                            "Subscriptions": {
                                "description": "Sub",
                                "parameters": {
                                    "flag_a": {
                                        "defaultValue": {"value": "live_val"},
                                        "valueType": "STRING",
                                    }
                                },
                            }
                        },
                    }
                ),
                encoding="utf-8",
            )
            repo_file.write_text(
                json.dumps(
                    {
                        "conditions": [],
                        "parameters": {},
                        "parameterGroups": {
                            "Subscriptions": {
                                "description": "Sub updated",
                                "parameters": {
                                    "flag_a": {
                                        "defaultValue": {"value": "repo_val"},
                                        "description": "Desc A",
                                        "valueType": "STRING",
                                    },
                                    "flag_b": {
                                        "defaultValue": {"value": "true"},
                                        "valueType": "BOOLEAN",
                                    },
                                },
                            }
                        },
                    }
                ),
                encoding="utf-8",
            )

            exit_code = main(
                ["--live", str(live_file), "--repo", str(repo_file), "--output", str(output_file)]
            )

            self.assertEqual(exit_code, 0)
            self.assertTrue(output_file.exists())
            merged = json.loads(output_file.read_text(encoding="utf-8"))
            params = merged["parameterGroups"]["Subscriptions"]["parameters"]
            self.assertEqual(params["flag_a"]["defaultValue"]["value"], "live_val")
            self.assertEqual(params["flag_a"]["description"], "Desc A")
            self.assertEqual(params["flag_b"]["defaultValue"]["value"], "true")

    def test_cli_empty_live_file_fallback(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            live_file = tmp_path / "live.json"
            repo_file = tmp_path / "repo.json"
            output_file = tmp_path / "out.json"

            live_file.write_text("", encoding="utf-8")
            repo_file.write_text(
                json.dumps(
                    {
                        "conditions": [],
                        "parameters": {"k": {"defaultValue": {"value": "v"}, "valueType": "STRING"}},
                        "parameterGroups": {},
                    }
                ),
                encoding="utf-8",
            )

            exit_code = main(
                ["--live", str(live_file), "--repo", str(repo_file), "--output", str(output_file)]
            )

            self.assertEqual(exit_code, 0)
            merged = json.loads(output_file.read_text(encoding="utf-8"))
            self.assertEqual(merged["parameters"]["k"]["defaultValue"]["value"], "v")

    def test_cli_fails_on_corrupt_live_json(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            live_file = tmp_path / "live.json"
            repo_file = tmp_path / "repo.json"
            output_file = tmp_path / "out.json"

            live_file.write_text("{not valid json", encoding="utf-8")
            repo_file.write_text(
                json.dumps({"conditions": [], "parameters": {}, "parameterGroups": {}}),
                encoding="utf-8",
            )

            exit_code = main(
                ["--live", str(live_file), "--repo", str(repo_file), "--output", str(output_file)]
            )

            self.assertEqual(exit_code, 1)
            self.assertFalse(output_file.exists())


if __name__ == "__main__":
    unittest.main()
