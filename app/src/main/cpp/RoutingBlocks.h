/*
 * RoutingBlocks.h — built-in SPLIT / MERGE routing blocks for Giggin' Gear.
 *
 * Motivation (see UI/UX spec "SPLIT & MERGE routing blocks"):
 *   Building parallel routing by freeform port-dot wire dragging is fiddly
 *   on a touchscreen. Helix Stadium solves it with structured insertable
 *   blocks: a SPLIT forks one signal into two branches; a MERGE recombines
 *   two branches back into one. We adapt the same idea (not a UI clone).
 *
 * These are synthetic in-memory LADSPA descriptors. They occupy a real
 * activePlugins slot (the engine maps canvas node id == activePlugins index
 * 1:1, so a routing block cannot be a "virtual" no-slot node) but they do
 * NOT do any DSP in run():
 *   - SPLIT (1 in, 2 out): its output buffer equals its input buffer; the
 *     GraphSolver emits downstream COPY ops to fan the output into each branch.
 *   - MERGE (2 in, 1 out): the GraphSolver emits COPY(buf[pred1]->buf[merge])
 *     then MIX(buf[pred2..]->buf[merge]) BEFORE RUN(MERGE). FullDuplexPass
 *     connects every MERGE port to buffer op.dst (already summed), so run()
 *     is a pass-through no-op.
 *
 * No changes to GraphSolver.h or FullDuplexPass.h RUN/COPY/MIX logic are
 * required. These blocks are plain DAG nodes with fixed port counts that the
 * solver already supports.
 */

#ifndef GIGGINGEAR_ROUTING_BLOCKS_H
#define GIGGINGEAR_ROUTING_BLOCKS_H

#include "ladspa.h"
#include <cstddef>
#include <cstring>

namespace ggrouting {

/* Unified little instance: just holds the connected audio buffer pointers.
   Since run() is a no-op for these pass-through blocks, the pointers are
   kept for correctness/safety but never processed. */
struct RoutingHandle {
    LADSPA_Data * port0 = nullptr;
    LADSPA_Data * port1 = nullptr;
    LADSPA_Data * port2 = nullptr;
};

/* ------------------------------------------------------------------ */
/* Shared helpers                                                      */
/* ------------------------------------------------------------------ */

inline LADSPA_Handle instantiate(const LADSPA_Descriptor *, unsigned long) {
    return new RoutingHandle();
}

inline void connect_port(LADSPA_Handle h, unsigned long port, LADSPA_Data * data) {
    if (h == nullptr) return;
    RoutingHandle * r = static_cast<RoutingHandle *>(h);
    switch (port) {
        case 0: r->port0 = data; break;
        case 1: r->port1 = data; break;
        case 2: r->port2 = data; break;
        default: break;
    }
}

inline void noop_run(LADSPA_Handle, unsigned long) {
    /* Pass-through: the DAG does the fan-out (COPY) and summing (MIX). */
}

inline void cleanup(LADSPA_Handle h) {
    delete static_cast<RoutingHandle *>(h);
}

/* ------------------------------------------------------------------ */
/* SPLIT — 1 audio input, 2 audio outputs                             */
/*   ports: [ in(0), out_A(1), out_B(2) ]                              */
/*   Plugin ctor maps -> inputPort=0, outputPort=1, outputPort2=2      */
/* ------------------------------------------------------------------ */
static const LADSPA_PortDescriptor SPLIT_PORTS_DESC[] = {
    LADSPA_PORT_INPUT | LADSPA_PORT_AUDIO,
    LADSPA_PORT_OUTPUT | LADSPA_PORT_AUDIO,
    LADSPA_PORT_OUTPUT | LADSPA_PORT_AUDIO
};
static const char * const SPLIT_PORT_NAMES[] = {
    "Dry input", "Split Out A", "Split Out B"
};
static const LADSPA_PortRangeHint SPLIT_HINTS[3] = { {0,0,0}, {0,0,0}, {0,0,0} };

inline const LADSPA_Descriptor * splitDescriptor() {
    static LADSPA_Descriptor d;
    d.UniqueID = 9001;
    d.Label = "GigginSplit";
    d.Name = "Giggin Gear SPLIT";
    d.Maker = "Giggin Gear";
    d.Copyright = "Giggin Gear";
    d.PortCount = 3;
    d.PortDescriptors = SPLIT_PORTS_DESC;
    d.PortNames = SPLIT_PORT_NAMES;
    d.PortRangeHints = SPLIT_HINTS;
    d.ImplementationData = nullptr;
    d.instantiate = instantiate;
    d.connect_port = connect_port;
    d.activate = nullptr;
    d.run = noop_run;
    d.run_adding = nullptr;
    d.set_run_adding_gain = nullptr;
    d.deactivate = nullptr;
    d.cleanup = cleanup;
    return &d;
}

/* ------------------------------------------------------------------ */
/* MERGE — 2 audio inputs, 1 audio output                             */
/*   ports: [ in_Left(0), in_Right(1), out(2) ]                        */
/*   Plugin ctor maps -> inputPort=0, inputPort2=1, outputPort=2       */
/* ------------------------------------------------------------------ */
static const LADSPA_PortDescriptor MERGE_PORTS_DESC[] = {
    LADSPA_PORT_INPUT | LADSPA_PORT_AUDIO,
    LADSPA_PORT_INPUT | LADSPA_PORT_AUDIO,
    LADSPA_PORT_OUTPUT | LADSPA_PORT_AUDIO
};
static const char * const MERGE_PORT_NAMES[] = {
    "Merge In A", "Merge In B", "Merge Out"
};
static const LADSPA_PortRangeHint MERGE_HINTS[3] = { {0,0,0}, {0,0,0}, {0,0,0} };

inline const LADSPA_Descriptor * mergeDescriptor() {
    static LADSPA_Descriptor d;
    d.UniqueID = 9002;
    d.Label = "GigginMerge";
    d.Name = "Giggin Gear MERGE";
    d.Maker = "Giggin Gear";
    d.Copyright = "Giggin Gear";
    d.PortCount = 3;
    d.PortDescriptors = MERGE_PORTS_DESC;
    d.PortNames = MERGE_PORT_NAMES;
    d.PortRangeHints = MERGE_HINTS;
    d.ImplementationData = nullptr;
    d.instantiate = instantiate;
    d.connect_port = connect_port;
    d.activate = nullptr;
    d.run = noop_run;
    d.run_adding = nullptr;
    d.set_run_adding_gain = nullptr;
    d.deactivate = nullptr;
    d.cleanup = cleanup;
    return &d;
}

} // namespace ggrouting

#endif // GIGGINGEAR_ROUTING_BLOCKS_H