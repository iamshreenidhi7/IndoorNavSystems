package com.indoornav.engine.graph;

import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.model.NavigationNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable graph and floor scales used by a route finder.
 */
public final class NavigationGraph {
    private final Map<Long, NavigationNode> nodesById;
    private final Map<Long, NavigationEdge> edgesById;
    private final List<Long> edgeIds;
    private final Map<Long, List<Traversal>> traversalsByNodeId;
    private final Map<Long, Double> metresPerUnitByFloorId;

    public NavigationGraph(
            Collection<NavigationNode> nodes,
            Collection<NavigationEdge> edges,
            Map<Long, Double> metresPerUnitByFloorId) {
        this.nodesById = indexNodes(nodes);
        this.edgesById = indexEdges(edges);
        this.edgeIds = edgesById.keySet().stream().sorted().toList();
        this.metresPerUnitByFloorId = copyAndValidateFloorScales(metresPerUnitByFloorId);
        validateNodeFloorScales();
        validateEdgeDistances();
        this.traversalsByNodeId = buildTraversals();
    }

    public NavigationNode node(long nodeId) {
        return nodesById.get(nodeId);
    }

    public NavigationEdge edge(long edgeId) {
        return edgesById.get(edgeId);
    }

    public List<Long> edgeIds() {
        return edgeIds;
    }

    public List<Traversal> traversalsFrom(long nodeId) {
        return traversalsByNodeId.getOrDefault(nodeId, List.of());
    }

    public double metresPerUnit(long floorId) {
        Double scale = metresPerUnitByFloorId.get(floorId);
        if (scale == null) {
            throw new IllegalArgumentException("No scale exists for floor " + floorId + ".");
        }
        return scale;
    }

    private Map<Long, NavigationNode> indexNodes(Collection<NavigationNode> nodes) {
        Objects.requireNonNull(nodes, "nodes must not be null");
        Map<Long, NavigationNode> indexed = new HashMap<>();
        for (NavigationNode node : nodes) {
            NavigationNode previous = indexed.put(node.id(), Objects.requireNonNull(node, "node must not be null"));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate node ID " + node.id() + ".");
            }
        }
        return Map.copyOf(indexed);
    }

    private Map<Long, NavigationEdge> indexEdges(Collection<NavigationEdge> edges) {
        Objects.requireNonNull(edges, "edges must not be null");
        Map<Long, NavigationEdge> indexed = new HashMap<>();
        for (NavigationEdge edge : edges) {
            NavigationEdge previous = indexed.put(edge.id(), Objects.requireNonNull(edge, "edge must not be null"));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate edge ID " + edge.id() + ".");
            }
            if (!nodesById.containsKey(edge.fromNodeId()) || !nodesById.containsKey(edge.toNodeId())) {
                throw new IllegalArgumentException("Edge " + edge.id() + " references an unknown node.");
            }
        }
        return Map.copyOf(indexed);
    }

    private Map<Long, Double> copyAndValidateFloorScales(Map<Long, Double> floorScales) {
        Objects.requireNonNull(floorScales, "metresPerUnitByFloorId must not be null");
        Map<Long, Double> copied = new HashMap<>();
        for (Map.Entry<Long, Double> entry : floorScales.entrySet()) {
            Long floorId = Objects.requireNonNull(entry.getKey(), "floor ID must not be null");
            Double scale = Objects.requireNonNull(entry.getValue(), "floor scale must not be null");
            if (floorId <= 0 || !Double.isFinite(scale) || scale <= 0.0) {
                throw new IllegalArgumentException("Floor IDs and scales must be positive.");
            }
            copied.put(floorId, scale);
        }
        return Map.copyOf(copied);
    }

    private void validateNodeFloorScales() {
        for (NavigationNode node : nodesById.values()) {
            metresPerUnit(node.floorId());
        }
    }

    private void validateEdgeDistances() {
        for (NavigationEdge edge : edgesById.values()) {
            NavigationNode from = node(edge.fromNodeId());
            NavigationNode to = node(edge.toNodeId());
            double fromScale = metresPerUnit(from.floorId());
            double toScale = metresPerUnit(to.floorId());
            double horizontalDistance = Math.hypot(
                    (from.x() * fromScale) - (to.x() * toScale),
                    (from.y() * fromScale) - (to.y() * toScale));
            if (edge.distanceMetres() + 1.0e-9 < horizontalDistance) {
                throw new IllegalArgumentException("Edge " + edge.id() + " is shorter than its horizontal distance.");
            }
        }
    }

    private Map<Long, List<Traversal>> buildTraversals() {
        Map<Long, List<Traversal>> mutable = new HashMap<>();
        for (long edgeId : edgeIds) {
            NavigationEdge edge = edgesById.get(edgeId);
            addTraversal(mutable, edge.fromNodeId(), edge.toNodeId(), edge);
            if (edge.bidirectional()) {
                addTraversal(mutable, edge.toNodeId(), edge.fromNodeId(), edge);
            }
        }
        Map<Long, List<Traversal>> immutable = new HashMap<>();
        for (Map.Entry<Long, List<Traversal>> entry : mutable.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(Traversal::destinationNodeId)
                    .thenComparingLong(traversal -> traversal.edge().id()));
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutable);
    }

    private void addTraversal(Map<Long, List<Traversal>> traversals, long fromNodeId, long toNodeId, NavigationEdge edge) {
        traversals.computeIfAbsent(fromNodeId, ignored -> new ArrayList<>()).add(new Traversal(toNodeId, edge));
    }

    public record Traversal(long destinationNodeId, NavigationEdge edge) {
    }
}
