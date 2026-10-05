package com.kancrutt.gigginggear.metadata;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ParameterMetadataSafetyTest {
    @Test public void missingMetadataIsSafe() {
        assertNotNull(ParameterMetadata.fromJson(null));
        assertNotNull(ParameterMetadata.fromJson(""));
    }

    @Test public void malformedMetadataIsSafe() {
        assertNotNull(ParameterMetadata.fromJson("{bad json"));
    }

    @Test public void missingPluginAndPortReturnFallback() {
        ParameterMetadata metadata = ParameterMetadata.fromJson("{\"ladspa\":{}}");
        assertNull(metadata.getLadspaPort("9999", 0));
        assertNull(metadata.getLv2Port("urn:missing", "mode"));
    }

    @Test public void invalidRangeIsClampedToSafeRange() {
        String json = "{\"ladspa\":{\"1\":{\"ports\":[{" +
                "\"port_index\":0,\"min\":5,\"max\":2,\"default\":3," +
                "\"unit\":\"\",\"control_type\":\"KNOB\",\"plugin_type\":\"FLOAT\"}]}}}";
        ParameterMetadata metadata = ParameterMetadata.fromJson(json);
        assertNotNull(metadata);
        // A min>max range must be clamped to a safe 0..1 window, not dropped:
        // startup must never lose the port because of a bad descriptor.
        ParameterMetadata.PortMeta port = metadata.getLadspaPort("1", 0);
        assertNotNull(port);
        assertEquals(0f, port.min, 0f);
        assertEquals(1f, port.max, 0f);
    }

    @Test public void lv2LookupFallsBackToPortName() {
        String json = "{\"lv2\":{\"urn:x:test\":{\"name\":\"Test\",\"ports\":[{" +
                "\"symbol\":\"foo\",\"name\":\"Bar Baz\",\"min\":0,\"max\":1," +
                "\"default\":0.5,\"unit\":\"\",\"control_type\":\"KNOB\"," +
                "\"plugin_type\":\"FLOAT\"}]}}}";
        ParameterMetadata metadata = ParameterMetadata.fromJson(json);
        assertNotNull(metadata.getLv2Port("urn:x:test", "foo"));
        // MainActivity looks LV2 ports up by display name.
        ParameterMetadata.PortMeta byName = metadata.getLv2Port("urn:x:test", "Bar Baz");
        assertNotNull(byName);
        assertEquals("foo", byName.symbol);
        assertNull(metadata.getLv2Port("urn:x:test", "nope"));
    }

    @Test public void bundledAssetParsesAndCarriesLv2GroundTruth() throws Exception {
        File asset = new File("src/main/assets/parameter_metadata.json");
        assertTrue("asset missing at " + asset.getAbsolutePath(), asset.exists());
        String json = new String(Files.readAllBytes(asset.toPath()), "UTF-8");
        ParameterMetadata metadata = ParameterMetadata.fromJson(json);
        assertNotNull(metadata);

        // LADSPA section preserved.
        ParameterMetadata.PortMeta ladspaPort =
                metadata.getLadspaPort("1407", 0);
        assertNotNull("ladspa section lost", ladspaPort);

        // P0-3: lv2 section populated from TTL ground truth.
        ParameterMetadata.PortMeta slope =
                metadata.getLv2Port("http://ssj71.github.io/infamousPlugins/plugs.html#octolo", "SLOPE");
        assertNotNull("octolo SLOPE missing", slope);
        assertEquals(0.8f, slope.defaultValue, 0.0001f);
        assertEquals(ParameterMetadata.ControlType.KNOB, slope.controlType);

        // Name fallback reaches the stereoctolo twin with its own default.
        ParameterMetadata.PortMeta stereoSlope =
                metadata.getLv2Port("http://ssj71.github.io/infamousPlugins/plugs.html#stereoctolo", "Slope");
        assertNotNull("stereoctolo SLOPE missing via name fallback", stereoSlope);
        assertEquals(1.0f, stereoSlope.defaultValue, 0.0001f);

        // Stale bundled TTL: #octolo lacks PSEQUENCE entirely. The generator
        // synthesizes the port from the runtime JSON and inherits labels from
        // the #stereoctolo sibling; out-of-range points (6,7) are dropped.
        ParameterMetadata.PortMeta panSeq =
                metadata.getLv2Port("http://ssj71.github.io/infamousPlugins/plugs.html#octolo", "Pan Sequence");
        assertNotNull("octolo PSEQUENCE (json fallback) missing", panSeq);
        assertEquals(ParameterMetadata.ControlType.ENUM_PICKER, panSeq.controlType);
        org.junit.Assert.assertArrayEquals(
                new String[]{"Center", "Alternate", "Alternate 2", "Alternate 3", "Alternate 4", "Step"},
                panSeq.enumLabels);

        // Invariants across the whole lv2 section.
        int plugins = 0;
        int withEnums = 0;
        for (String uri : metadata.lv2Keys()) {
            plugins++;
            java.util.Map<String, ParameterMetadata.PortMeta> lv = metadata.getLv2Plugin(uri);
            for (ParameterMetadata.PortMeta p : lv.values()) {
                if (p.hasEnum()) {
                    withEnums++;
                    assertEquals(p.enumValues.length, p.enumLabels.length);
                    for (String label : p.enumLabels) {
                        assertNotNull(label);
                        assertTrue("blank enum label", !label.trim().isEmpty());
                    }
                }
                if (p.controlType == ParameterMetadata.ControlType.TOGGLE) {
                    assertEquals(0f, p.min, 0f);
                    assertEquals(1f, p.max, 0f);
                }
            }
        }
        assertTrue("lv2 section too small: " + plugins, plugins >= 150);
        assertTrue("no enum labels found: " + withEnums, withEnums >= 50);
    }

    @Test public void cabinetIvManualLadspaMetadataIsPresent() throws Exception {
        File asset = new File("src/main/assets/parameter_metadata.json");
        ParameterMetadata metadata = ParameterMetadata.fromJson(
                new String(Files.readAllBytes(asset.toPath()), "UTF-8"));

        // P2 manual entry for the one curated plugin the host probe cannot
        // reach (Android ELF). Port 0 = cabinet-model enum, port 1 = gain dB.
        ParameterMetadata.PortMeta model = metadata.getLadspaPort("2606", 0);
        assertNotNull("CabinetIV model port missing", model);
        assertEquals(ParameterMetadata.ControlType.ENUM_PICKER, model.controlType);
        assertEquals(25, model.enumLabels.length);
        assertEquals("mega wookie 800", model.enumLabels[0]);
        assertEquals(12f, model.defaultValue, 0.001f);

        ParameterMetadata.PortMeta gain = metadata.getLadspaPort("2606", 1);
        assertNotNull("CabinetIV gain port missing", gain);
        assertEquals("dB", gain.unit);
        assertEquals(-24f, gain.min, 0.001f);
        assertEquals(24f, gain.max, 0.001f);
    }

    @Test public void noToggledBooleanConflicts() throws Exception {
        // 3.2-A: a TOGGLE/BOOLEAN must collapse to a 0..1 range. Any wider is a
        // metadata bug that would render a checkbox outside its boolean domain.
        File asset = new File("src/main/assets/parameter_metadata.json");
        ParameterMetadata metadata = ParameterMetadata.fromJson(
                new String(Files.readAllBytes(asset.toPath()), "UTF-8"));
        int violations = 0;
        for (String uri : metadata.lv2Keys()) {
            for (ParameterMetadata.PortMeta p : metadata.getLv2Plugin(uri).values()) {
                if (p.controlType == ParameterMetadata.ControlType.TOGGLE
                        || p.pluginType == ParameterMetadata.PluginType.BOOLEAN) {
                    if (p.min < 0f || p.max > 1f) violations++;
                }
            }
        }
        assertEquals("toggled/boolean ports must span [0,1]", 0, violations);
    }

    @Test public void sampleRatePortsAreFlagged() throws Exception {
        // 3.2-C: sample-rate-relative ports carry isSampleRate=true and a blank
        // unit (formatter appends "/sr"). Verify >=30 are flagged + gain port is not.
        File asset = new File("src/main/assets/parameter_metadata.json");
        ParameterMetadata metadata = ParameterMetadata.fromJson(
                new String(Files.readAllBytes(asset.toPath()), "UTF-8"));
        int srCount = 0;
        for (String uri : metadata.lv2Keys()) {
            for (ParameterMetadata.PortMeta p : metadata.getLv2Plugin(uri).values()) {
                if (p.isSampleRate) {
                    srCount++;
                    assertTrue("sr port unit must be blank",
                            p.unit == null || p.unit.isEmpty());
                }
            }
        }
        for (String uid : metadata.ladspaKeys()) {
            for (ParameterMetadata.PortMeta p : metadata.getLadspaPlugin(uid).values()) {
                if (p.isSampleRate) {
                    srCount++;
                    assertTrue("sr port unit must be blank",
                            p.unit == null || p.unit.isEmpty());
                }
            }
        }
        assertTrue("expected >=30 sample_rate ports flagged", srCount >= 30);
        ParameterMetadata.PortMeta gain = metadata.getLadspaPort("2606", 1);
        assertFalse("CabinetIV gain must not be sr-relative",
                gain != null && gain.isSampleRate);
    }

    @Test public void conflictDefaultDoesNotBreakParse() {
        // 3.2-B: a port declaring conflict_default=true must still parse.
        String json = "{\"ladspa\":{\"2606\":{\"name\":\"test\",\"library\":\"libx.so\",\"ports\":["
                + "{\"name\":\"conflict\",\"symbol\":\"c\",\"port_index\":0,"
                + "\"default\":1.0,\"min\":0.0,\"max\":2.0,\"unit\":\"\",\"control_type\":\"ENUM\","
                + "\"plugin_type\":\"ENUM\",\"rdf_default\":2.0,\"hint_default\":1.0,"
                + "\"conflict_default\":true,\"sample_rate\":false,"
                + "\"enum_values\":[0,1,2],\"enum_labels\":[\"a\",\"b\",\"c\"]}"
                + "]}},\"lv2\":{}}";
        ParameterMetadata meta = ParameterMetadata.fromJson(json);
        ParameterMetadata.PortMeta p = meta.getLadspaPort("2606", 0);
        assertNotNull(p);
        assertEquals(1.0f, p.defaultValue, 0.001f);
        assertEquals(3, p.enumLabels.length);
    }
}
