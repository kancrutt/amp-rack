import json
import unittest
from pathlib import Path

from build_param_audit import (
    LADSPA_RDF_DIR,
    parse_ladspa_xml,
    validate_metadata,
)


class MetadataAuditTest(unittest.TestCase):
    def test_caps_rdf_fixture_parses(self):
        entities = {
            "&ladspa;": "http://ladspa.org/ontology#",
            "&rdf;": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "&rdfs;": "http://www.w3.org/2000/01/rdf-schema#",
            "&dc;": "http://purl.org/dc/elements/1.1/",
        }
        root = parse_ladspa_xml(LADSPA_RDF_DIR / "caps.rdf", entities)
        self.assertTrue(root.tag.endswith("RDF"))
        self.assertGreaterEqual(len(list(root)), 1)

    def test_validation_rejects_enum_mismatch(self):
        profile = {
            "ladspa": {
                "1": {
                    "ports": [
                        {
                            "port_index": 0,
                            "min": 0,
                            "max": 1,
                            "default": 0,
                            "unit": "",
                            "control_type": "ENUM",
                            "enum_values": [0],
                            "enum_labels": [],
                        }
                    ]
                }
            },
            "lv2": {},
        }
        with self.assertRaises(ValueError):
            validate_metadata(profile)

    def test_validation_allows_explicit_unknown_range(self):
        profile = {
            "ladspa": {},
            "lv2": {
                "urn:test": {
                    "ports": [
                        {
                            "symbol": "mode",
                            "min": None,
                            "max": None,
                            "default": None,
                            "unit": "",
                            "control_type": "KNOB",
                            "enum_values": [],
                            "enum_labels": [],
                        }
                    ]
                }
            },
        }
        validate_metadata(profile)


if __name__ == "__main__":
    unittest.main()
