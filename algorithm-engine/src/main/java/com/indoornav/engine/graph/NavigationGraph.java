package com.indoornav.engine.graph;

import com.indoornav.engine.model.Edge;
import com.indoornav.engine.model.FloorScale;
import com.indoornav.engine.model.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable, in-memory weighted graph of a building.
 *
 * <p>Built only through {@link NavigationGraphBuilder}, never constructed
 * directly, so every instance is guaranteed to satisfy the invariants checked
 * at build time (ARCHITECTURE.md Section 5.3: "Immutable after construction").
 * A bidirectional edge already appears in both of its endpoints' adjacency
 * lists; callers never need to reason about direction themselves.
 */
public final class NavigationGraph {

    private final Map<Long, Node> nodesById;
    private final Map<Long, Edge> edgesById;
    private final Map<Long, List<Edge>> outgoingEdgesByNodeId;
    private final Map<Long, FloorScale> floorScalesByFloorId;

    NavigationGraph(
            Map<Long, Node> nodesById,
            Map<Long, Edge> edgesById,
            Map<Long, List<Edge>> outgoingEdgesByNodeId,
            Map<Long, FloorScale> floorScalesByFloorId) {
        this.nodesById = Map.copyOf(nodesById);
        this.edgesById = Map.copyOf(edgesById);

        Map<Long, List<Edge>> copiedAdjacency = new HashMap<>();
        outgoingEdgesByNodeId.forEach((nodeId, edges) -> copiedAdjacency.put(nodeId, List.copyOf(edges)));
        this.outgoingEdgesByNodeId = Map.copyOf(copiedAdjacency);

        this.floorScalesByFloorId = Map.copyOf(floorScalesByFloorId);
    }

    /** Whether a node with this id exists in the graph. */
    public boolean containsNode(long nodeId) {
        return nodesById.containsKey(nodeId);
    }

    /** The node with this id, if it exists. */
    public Optional<Node> findNode(long nodeId) {
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    /** The edge with this id, if it exists. */
    public Optional<Edge> findEdge(long edgeId) {
        return Optional.ofNullable(edgesById.get(edgeId));
    }

    /** The scale recorded for this floor, if any was supplied when the graph was built. */
    public Optional<FloorScale> findFloorScale(long floorId) {
        return Optional.ofNullable(floorScalesByFloorId.get(floorId));
    }

    /**
     * Edges leaving this node, in a deterministic order (ascending edge id).
     * A bidirectional edge appears here for both of its endpoints. Never
     * {@code null}; a node with no outgoing edges returns an empty list.
     */
    public List<Edge> outgoingEdges(long nodeId) {
        return outgoingEdgesByNodeId.getOrDefault(nodeId, List.of());
    }

    /** All node ids in the graph. */
    public Set<Long> nodeIds() {
        return nodesById.keySet();
    }

    /** All edge ids in the graph. */
    public Set<Long> edgeIds() {
        return edgesById.keySet();
    }

    public int nodeCount() {
        return nodesById.size();
    }

    public int edgeCount() {
        return edgesById.size();
    }
}
