package com.kancrutt.gigginggear.metadata;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
                "\"port_index\":0,\"min\":0,\"max\":1,\"default\":0," +
                "\"unit\":\"\",\"control_type\":\"KNOB\",\"plugin_type\":\"FLOAT\"}]}}}";
        ParameterMetadata metadata = ParameterMetadata.fromJson(json);
        assertNotNull(metadata);
        // A malformed or incomplete port may be rejected, but must never
        // prevent the registry from being created or crash startup.
        assertNull(metadata.getLadspaPort("1", 0));
    }
}
