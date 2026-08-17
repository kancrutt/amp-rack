/*
 * GraphSolver.h — pure, audio-independent DAG execution planner for Giggin' Gear.
 *
 * This header has ZERO dependencies on Android, Oboe, LADSPA, LV2 or the Engine.
 * It can be compiled and unit-tested on the host (g++) in isolation, which is
 * intentional: the graph -> audio-chain mapping is the highest-risk logic in the
 * node-graph editor work, so it must be proven correct BEFORE it ever runs on an
 * audio thread.
 *
 * Scope:
 *   - Input:  N nodes (indexed 0..N-1, index == plugin chain position) + directed edges.
 *   - Output: an ordered execution Plan of primitive ops the audio engine can run:
 *         RUN  : connect plugin `node` ports to buffer `dst` and run it (in-place).
 *         COPY : buffers[dst] = buffers[src]  (branch / feed a node its input).
 *         MIX  : buffers[dst] += buffers[src] (merge / sum parallel branches).
 *   - The planner guarantees a valid topological order and rejects cycles.
 *
 * Buffer semantics (engine side):
 *   - buffer 0 is the main data buffer (holds dry input at start, final output at end).
 *   - buffers 1..numBuffers-1 are scratch buffers owned by the engine.
 */

#ifndef GIGGINGEAR_GRAPH_SOLVER_H
#define GIGGINGEAR_GRAPH_SOLVER_H

#include <cstddef>
#include <deque>
#include <string>
#include <vector>

namespace ggsolver {

enum class OpType {
    RUN,   // node processes buffer dst in place
    COPY,  // dst = src
    MIX    // dst += src
};

struct Op {
    OpType type;
    int node;   // plugin chain index (meaningful only for RUN)
    int src;    // source buffer index (COPY/MIX)
    int dst;    // destination buffer index (RUN/COPY/MIX)
};

struct Edge {
    int from;
    int to;
};

struct Plan {
    bool seriesOnly = false;             // fast path: linear chain, single buffer 0
    std::vector<Op> ops;                 // execution order
    int numBuffers = 1;                  // scratch buffers needed (indexes 0..numBuffers-1)
};

// Builds an execution plan for the given DAG.
// Returns true on success. On failure (cycle / bad node id) returns false and
// fills `err` with a reason. The plan is never partially populated on failure.
inline bool buildPlan(int numNodes,
               const std::vector<Edge>& edges,
               Plan& plan,
               std::string& err) {
    plan = Plan();
    if (numNodes <= 0) {
        plan.seriesOnly = true;
        return true;
    }

    // --- validate + adjacency ------------------------------------------------
    std::vector<int> indeg(numNodes, 0);
    std::vector<std::vector<int>> adj(numNodes);
    std::vector<std::vector<int>> preds(numNodes);
    for (const Edge& e : edges) {
        if (e.from < 0 || e.from >= numNodes || e.to < 0 || e.to >= numNodes) {
            err = "edge references node outside 0.." + std::to_string(numNodes - 1);
            return false;
        }
        adj[e.from].push_back(e.to);
        preds[e.to].push_back(e.from);
        indeg[e.to]++;
    }

    // --- topological sort (Kahn) --------------------------------------------
    std::vector<int> order;
    order.reserve(numNodes);
    std::deque<int> q;
    for (int i = 0; i < numNodes; i++) {
        if (indeg[i] == 0) q.push_back(i);
    }
    while (!q.empty()) {
        int u = q.front();
        q.pop_front();
        order.push_back(u);
        for (int v : adj[u]) {
            if (--indeg[v] == 0) q.push_back(v);
        }
    }
    if (static_cast<int>(order.size()) != numNodes) {
        err = "cycle detected in graph";
        return false;
    }

    // --- series-only fast path ----------------------------------------------
    // True linear chain: every node has at most one in and one out edge, and
    // exactly one source + one sink. Then we can run every plugin in place on
    // buffer 0 with zero scratch copies — identical to the existing series code,
    // preserving the ~5.3ms baseline.
    bool series = true;
    int srcs = 0, sinks = 0;
    for (int i = 0; i < numNodes; i++) {
        if (adj[i].size() > 1 || preds[i].size() > 1) series = false;
        if (adj[i].empty()) sinks++;
        if (preds[i].empty()) srcs++;
    }
    if (srcs != 1 || sinks != 1) series = false;

    if (series) {
        plan.seriesOnly = true;
        for (int n : order) plan.ops.push_back({OpType::RUN, n, 0, 0});
        return true;
    }

    // --- general DAG ----------------------------------------------------------
    // Give every node its own scratch buffer (1..N). Buffer 0 stays the main
    // in/out buffer. This is simple and provably correct; buffer reuse to cut
    // copies is a later optimisation.
    std::vector<int> buf(numNodes);
    for (int i = 0; i < numNodes; i++) buf[i] = i + 1;
    plan.numBuffers = numNodes + 1;

    for (int u : order) {
        if (preds[u].empty()) {
            // Source: feed main input into the node's buffer, then run it.
            plan.ops.push_back({OpType::COPY, -1, 0, buf[u]});
            plan.ops.push_back({OpType::RUN, u, buf[u], buf[u]});
        } else if (preds[u].size() == 1) {
            // Single predecessor: copy its output into this node's buffer.
            plan.ops.push_back({OpType::COPY, -1, buf[preds[u][0]], buf[u]});
            plan.ops.push_back({OpType::RUN, u, buf[u], buf[u]});
        } else {
            // Merge (join): copy first pred, then mix the rest, then run.
            plan.ops.push_back({OpType::COPY, -1, buf[preds[u][0]], buf[u]});
            for (size_t k = 1; k < preds[u].size(); k++) {
                plan.ops.push_back({OpType::MIX, -1, buf[preds[u][k]], buf[u]});
            }
            plan.ops.push_back({OpType::RUN, u, buf[u], buf[u]});
        }
    }

    // --- sink / final output --------------------------------------------------
    std::vector<int> sinkList;
    for (int i = 0; i < numNodes; i++) {
        if (adj[i].empty()) sinkList.push_back(i);
    }
    if (!sinkList.empty()) {
        plan.ops.push_back({OpType::COPY, -1, buf[sinkList[0]], 0});
        for (size_t k = 1; k < sinkList.size(); k++) {
            plan.ops.push_back({OpType::MIX, -1, buf[sinkList[k]], 0});
        }
    }

    return true;
}

// Small helper for tests / debug: renders the plan as a readable string.
inline std::string planToString(const Plan& plan) {
    if (plan.seriesOnly) return "seriesOnly";
    std::string s;
    for (const Op& op : plan.ops) {
        if (!s.empty()) s += " ";
        switch (op.type) {
            case OpType::RUN:  s += "RUN(" + std::to_string(op.node) + ",b" + std::to_string(op.dst) + ")"; break;
            case OpType::COPY: s += "COPY(b" + std::to_string(op.src) + "->b" + std::to_string(op.dst) + ")"; break;
            case OpType::MIX:  s += "MIX(b" + std::to_string(op.src) + "->b" + std::to_string(op.dst) + ")"; break;
        }
    }
    return s;
}

} // namespace ggsolver

#endif // GIGGINGEAR_GRAPH_SOLVER_H