package com.kancrutt.gigginggear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kancrutt.gigginggear.SignalChainCanvasView.Edge;
import com.kancrutt.gigginggear.SignalChainCanvasView.Node;
import com.kancrutt.gigginggear.SignalChainCanvasView.NodeType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * R19: the one-row parallel layout is geometry-only. These tests pin the
 * derived graph edges (pre-R19 algorithm, moved verbatim into
 * deriveWiringEdges) so a layout change can never silently alter topology.
 */
public class RoutingWiresTest {

    private static Node node(int id, String name, NodeType type) {
        Node n = new Node(id, id, name, 0, 0);
        n.nodeType = type;
        return n;
    }

    private static Set<String> edgeSet(List<Edge> edges) {
        Set<String> s = new TreeSet<>();
        for (Edge e : edges) s.add(e.fromNodeId + "->" + e.toNodeId);
        return s;
    }

    private static Set<String> edgesOf(Node in, Node out, Node... chain) {
        List<Node> walk = new ArrayList<>();
        for (Node n : chain) walk.add(n);
        return edgeSet(SignalChainCanvasView.deriveWiringEdges(in, out, walk));
    }

    @Test
    public void seriesChainHasNoParallelEdges() {
        Node in = node(-100, "IN", NodeType.INPUT);
        Node a = node(0, "A", NodeType.PLUGIN);
        Node b = node(1, "B", NodeType.PLUGIN);
        Node c = node(2, "C", NodeType.PLUGIN);
        Node out = node(-200, "OUT", NodeType.OUTPUT);
        Set<String> edges = edgesOf(in, out, a, b, c);
        Set<String> want = new TreeSet<>();
        want.add("-100->0");
        want.add("0->1");
        want.add("1->2");
        want.add("2->-200");
        assertEquals(want, edges);
        assertEquals(0, a.branch + b.branch + c.branch);
    }

    @Test
    public void diamondSplitMergeEdgesUnchanged() {
        Node in = node(-100, "IN", NodeType.INPUT);
        Node split = node(0, "SPLIT", NodeType.SPLIT);
        Node a = node(1, "DELAY", NodeType.PLUGIN);
        Node b = node(2, "REVERB", NodeType.PLUGIN);
        Node merge = node(3, "MERGE", NodeType.MERGE);
        Node out = node(-200, "OUT", NodeType.OUTPUT);
        Set<String> edges = edgesOf(in, out, split, a, b, merge);
        Set<String> want = new TreeSet<>();
        want.add("-100->0");
        want.add("0->1");   // SPLIT -> lane A
        want.add("0->2");   // SPLIT -> lane B (fork)
        want.add("1->3");   // lane A -> MERGE (join)
        want.add("2->3");   // lane B -> MERGE
        want.add("3->-200");
        assertEquals(want, edges);
        // Alternating lane assignment: first block after SPLIT is lane A.
        assertEquals(0, split.branch);
        assertEquals(1, a.branch);
        assertEquals(2, b.branch);
        assertEquals(0, merge.branch);
    }

    @Test
    public void fourBlockInterleavedAlternatesLanes() {
        Node in = node(-100, "IN", NodeType.INPUT);
        Node split = node(0, "SPLIT", NodeType.SPLIT);
        Node a1 = node(1, "A1", NodeType.PLUGIN);
        Node b1 = node(2, "B1", NodeType.PLUGIN);
        Node a2 = node(3, "A2", NodeType.PLUGIN);
        Node b2 = node(4, "B2", NodeType.PLUGIN);
        Node merge = node(5, "MERGE", NodeType.MERGE);
        Node out = node(-200, "OUT", NodeType.OUTPUT);
        Set<String> edges = edgesOf(in, out, split, a1, b1, a2, b2, merge);
        Set<String> want = new TreeSet<>();
        want.add("-100->0");
        want.add("0->1");   // SPLIT -> A1
        want.add("0->2");   // SPLIT -> B1 (fork)
        want.add("1->3");   // A1 -> A2
        want.add("2->4");   // B1 -> B2
        want.add("3->5");   // A2 -> MERGE (join)
        want.add("4->5");   // B2 -> MERGE
        want.add("5->-200");
        assertEquals(want, edges);
        assertEquals(1, a1.branch);
        assertEquals(2, b1.branch);
        assertEquals(1, a2.branch);
        assertEquals(2, b2.branch);
    }

    @Test
    public void trunkPrefixAndSuffixStaySeries() {
        Node in = node(-100, "IN", NodeType.INPUT);
        Node amp = node(0, "AMP", NodeType.PLUGIN);
        Node split = node(1, "SPLIT", NodeType.SPLIT);
        Node a = node(2, "DELAY", NodeType.PLUGIN);
        Node b = node(3, "REVERB", NodeType.PLUGIN);
        Node merge = node(4, "MERGE", NodeType.MERGE);
        Node cab = node(5, "CAB", NodeType.PLUGIN);
        Node out = node(-200, "OUT", NodeType.OUTPUT);
        Set<String> edges = edgesOf(in, out, amp, split, a, b, merge, cab);
        Set<String> want = new TreeSet<>();
        want.add("-100->0");
        want.add("0->1");
        want.add("1->2");
        want.add("1->3");
        want.add("2->4");
        want.add("3->4");
        want.add("4->5");
        want.add("5->-200");
        assertEquals(want, edges);
        assertEquals(0, amp.branch);
        assertEquals(0, cab.branch);
    }

    @Test
    public void loneSplitFallsBackToSeries() {
        Node in = node(-100, "IN", NodeType.INPUT);
        Node split = node(0, "SPLIT", NodeType.SPLIT);
        Node a = node(1, "A", NodeType.PLUGIN);
        Node out = node(-200, "OUT", NodeType.OUTPUT);
        Set<String> edges = edgesOf(in, out, split, a);
        Set<String> want = new TreeSet<>();
        want.add("-100->0");
        want.add("0->1");
        want.add("1->-200");
        assertEquals(want, edges);
        assertTrue(split.isRouting());
    }
}
