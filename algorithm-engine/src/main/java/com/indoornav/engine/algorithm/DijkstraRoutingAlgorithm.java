package com.indoornav.engine.algorithm;

import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.Edge;
import com.indoornav.engine.result.RouteResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Shortest-distance router using Dijkstra's algorithm.
 *
 * <p>Minimises {@link Edge#distanceMeters()} only (IMPLEMENTATION_PLAN.md
 * Phase 3: "a correct, isolated, in-memory shortest-distance router"); the
 * preference-driven objectives (FASTEST, ACCESSIBLE, AVOID_STAIRS,
 * PREFER_ELEVATOR) and their cost model arrive in Phase 4. This
 * implementation also serves as the equivalence baseline for A* once that is
 * added in Phase 5 (ARCHITECTURE.md Section 5.2: "Dijkstra remains for
 * verification tests and as a fallback").
 *
 * <p>Deterministic by construction (task 3.4): node ids and edge ids are
 * never used as a {@code HashMap}/{@code HashSet} iteration order to decide
 * output, outgoing edges are visited in ascending edge-id order (sorted once
 * by {@link com.indoornav.engine.graph.NavigationGraphBuilder}), and the
 * priority queue breaks distance ties by ascending node id. A neighbour's
 * distance is only updated on a strictly shorter candidate, so of several
 * equally short paths the one reached first under this fixed visiting order
 * is always the one kept.
 */
public final class DijkstraRoutingAlgorithm implements RoutingAlgorithm {

    @Override
    public RouteResult findRoute(NavigationGraph graph, RoutingRequest request) {
        requireKnownNode(graph, request.startNodeId());
        requireKnownNode(graph, request.destinationNodeId());

        if (request.startNodeId() == request.destinationNodeId()) {
            return RouteResult.found(List.of(request.startNodeId()), List.of(), 0.0);
        }

        Map<Long, Double> bestDistance = new HashMap<>();
        Map<Long, Edge> predecessorEdge = new HashMap<>();
        Set<Long> finalized = new HashSet<>();

        bestDistance.put(request.startNodeId(), 0.0);
        PriorityQueue<Candidate> frontier = new PriorityQueue<>();
        frontier.add(new Candidate(0.0, request.startNodeId()));

        while (!frontier.isEmpty()) {
            Candidate current = frontier.poll();
            if (!finalized.add(current.nodeId())) {
                continue; // stale entry superseded by an earlier, shorter one
            }
            if (current.nodeId() == request.destinationNodeId()) {
                break; // finalized with the minimal distance; nothing more to improve
            }

            for (Edge edge : graph.outgoingEdges(current.nodeId())) {
                if (request.blockedEdgeIds().contains(edge.id())) {
                    continue;
                }
                long neighborId = edge.otherEndpoint(current.nodeId());
                if (finalized.contains(neighborId) || request.blockedNodeIds().contains(neighborId)) {
                    continue;
                }

                double candidateDistance = current.distance() + edge.distanceMeters();
                Double knownDistance = bestDistance.get(neighborId);
                if (knownDistance == null || candidateDistance < knownDistance) {
                    bestDistance.put(neighborId, candidateDistance);
                    predecessorEdge.put(neighborId, edge);
                    frontier.add(new Candidate(candidateDistance, neighborId));
                }
            }
        }

        if (!finalized.contains(request.destinationNodeId())) {
            return RouteResult.noRoute();
        }

        return RouteResult.found(
                reconstructNodePath(request.startNodeId(), request.destinationNodeId(), predecessorEdge),
                reconstructEdgePath(request.destinationNodeId(), predecessorEdge),
                bestDistance.get(request.destinationNodeId()));
    }

    private static List<Long> reconstructNodePath(long startNodeId, long destinationNodeId, Map<Long, Edge> predecessorEdge) {
        List<Long> nodeIds = new ArrayList<>();
        long currentNodeId = destinationNodeId;
        nodeIds.add(currentNodeId);
        while (currentNodeId != startNodeId) {
            Edge edge = predecessorEdge.get(currentNodeId);
            currentNodeId = edge.otherEndpoint(currentNodeId);
            nodeIds.add(currentNodeId);
        }
        Collections.reverse(nodeIds);
        return nodeIds;
    }

    private static List<Long> reconstructEdgePath(long destinationNodeId, Map<Long, Edge> predecessorEdge) {
        List<Long> edgeIds = new ArrayList<>();
        long currentNodeId = destinationNodeId;
        while (predecessorEdge.containsKey(currentNodeId)) {
            Edge edge = predecessorEdge.get(currentNodeId);
            edgeIds.add(edge.id());
            currentNodeId = edge.otherEndpoint(currentNodeId);
        }
        Collections.reverse(edgeIds);
        return edgeIds;
    }

    private static void requireKnownNode(NavigationGraph graph, long nodeId) {
        if (!graph.containsNode(nodeId)) {
            throw new UnknownNodeException(nodeId);
        }
    }

    /** A node reachable at {@code distance}, ordered for deterministic frontier expansion. */
    private record Candidate(double distance, long nodeId) implements Comparable<Candidate> {
        @Override
        public int compareTo(Candidate other) {
            int byDistance = Double.compare(distance, other.distance);
            return byDistance != 0 ? byDistance : Long.compare(nodeId, other.nodeId);
        }
    }
}
