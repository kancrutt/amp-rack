package com.kancrutt.gigginggear;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * ggrouting SPLIT/MERGE restore gate (r17 A.2 P1). loadPresetInner() routes
 * entries matching these names through AudioEngine.addRoutingBlock() instead
 * of the name-based loader, which can never resolve them (synthetic LADSPA
 * descriptors, absent from every library and the curated catalog).
 */
public class RoutingNameTest {

    @Test public void splitDescriptorIsRoutingName() {
        assertTrue(PluginNaming.isRoutingName("Giggin Gear SPLIT"));
        assertTrue(PluginNaming.isRoutingSplit("Giggin Gear SPLIT"));
    }

    @Test public void mergeDescriptorIsRoutingName() {
        assertTrue(PluginNaming.isRoutingName("Giggin Gear MERGE"));
        assertFalse(PluginNaming.isRoutingSplit("Giggin Gear MERGE"));
    }

    @Test public void caseInsensitiveMatch() {
        assertTrue(PluginNaming.isRoutingName("giggin gear split"));
        assertTrue(PluginNaming.isRoutingName("GIGGIN GEAR MERGE"));
        assertTrue(PluginNaming.isRoutingSplit("Giggin gear SPLIT"));
    }

    @Test public void nullAndEmptyRejected() {
        assertFalse(PluginNaming.isRoutingName(null));
        assertFalse(PluginNaming.isRoutingName(""));
        assertFalse(PluginNaming.isRoutingSplit(null));
        assertFalse(PluginNaming.isRoutingSplit(""));
    }

    @Test public void ordinaryPluginNamesRejected() {
        assertFalse(PluginNaming.isRoutingName("the infamous octolo"));
        assertFalse(PluginNaming.isRoutingName("TONE3000"));
        // A catalog splitter ("split" substring) must NOT take the routing path.
        assertFalse(PluginNaming.isRoutingName("mdasplitter"));
        assertFalse(PluginNaming.isRoutingSplit("MDA Splitter"));
        assertFalse(PluginNaming.isRoutingName("GxHotBox"));
    }

    @Test public void partiallyMatchesRejected() {
        // Substrings of the descriptor without the full marker are not routing.
        assertFalse(PluginNaming.isRoutingName("SPLIT"));
        assertFalse(PluginNaming.isRoutingName("MERGE"));
        assertFalse(PluginNaming.isRoutingName("Giggin Gear"));
    }
}
