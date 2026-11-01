package com.indoornav.engine.graph;

import com.indoornav.engine.model.Edge;
import com.indoornav.engine.model.FloorScale;
import com.indoornav.engine.model.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an immutable {@link NavigationGraph} from nodes, edges and
 * per-floor scales.
 *
 * <p>Collects everything added, then validates and assembles the graph in
 * {@link #build()} (IMPLEMENTATION_PLAN.md Phase 3, task 3.2: "Immutable
 * graph builder: nodes with floor and coordinates, per-floor scale, edges,
 * bidirectional expansion, and validation of positive costs and known
 * endpoints"). Edge cost positivity is already enforced by {@link Edge}'s own
 * constructor; this builder additionally checks that every edge references
 * nodes that were actually added, and that no id is reused.
 *
 * <p>Not thread-safe; a single builder is meant to be used from one thread to
 * assemble one graph.
 */
public final class NavigationGraphBuilder {

    private final Map<Long, Node> nodesById = new LinkedHashMap<>();
    private final Map<Long, Edge> edgesById = new LinkedHashMap<>();
    private final Map<Long, FloorScale> floorScalesByFloorId = new LinkedHashMap<>();

    public NavigationGraphBuilder addNode(Node node) {
        if (nodesById.containsKey(node.id())) {
            throw new GraphConstructionException("duplicate node id: " + node.id());
        }
        nodesById.put(node.id(), node);
        return this;
    }

    public NavigationGraphBuilder addEdge(Edge edge) {
        if (edgesById.containsKey(edge.id())) {
            throw new GraphConstructionException("duplicate edge id: " + edge.id());
        }
        edgesById.put(edge.id(), edge);
        return this;
    }

    public NavigationGraphBuilder addFloorScale(FloorScale floorScale) {
        if (floorScalesByFloorId.containsKey(floorScale.floorId())) {
            throw new GraphConstructionException("duplicate floor id in floor scale: " + floorScale.floorId());
        }
        floorScalesByFloorId.put(floorScale.floorId(), floorScale);
        return this;
    }

    /**
     * Validates everything added so far and assembles the immutable graph.
     *
     * @throws GraphConstructionException if an edge references a node that
     *                                     was never added
     */
    public NavigationGraph build() {
        for (Edge edge : edgesById.values()) {
            requireKnownNode(edge, edge.fromNodeId());
            requireKnownNode(edge, edge.toNodeId());
        }

        Map<Long, List<Edge>> outgoingEdgesByNodeId = new LinkedHashMap<>();
        for (Node node : nodesById.values()) {
            outgoingEdgesByNodeId.put(node.id(), new ArrayList<>());
        }
        for (Edge edge : edgesById.values()) {
            outgoingEdgesByNodeId.get(edge.fromNodeId()).add(edge);
            if (edge.bidirectional()) {
                outgoingEdgesByNodeId.get(edge.toNodeId()).add(edge);
            }
        }
        // Deterministic traversal order regardless of insertion order, so
        // Dijkstra visits neighbours the same way on every run (task 3.4).
        outgoingEdgesByNodeId.values().forEach(edges -> edges.sort(Comparator.comparingLong(Edge::id)));

        return new NavigationGraph(nodesById, edgesById, outgoingEdgesByNodeId, floorScalesByFloorId);
    }

    private void requireKnownNode(Edge edge, long nodeId) {
        if (!nodesById.containsKey(nodeId)) {
            throw new GraphConstructionException(
                    "edge " + edge.id() + " references unknown node " + nodeId);
        }
    }
}
