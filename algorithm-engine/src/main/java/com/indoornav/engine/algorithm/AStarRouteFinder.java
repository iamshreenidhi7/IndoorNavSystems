package com.indoornav.engine.algorithm;

import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.model.NavigationNode;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.RouteResult;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* implementation using an admissible horizontal lower-bound heuristic.
 */
public final class AStarRouteFinder extends AbstractRouteFinder {
    @Override
    public RouteResult findRoute(NavigationGraph graph, RouteRequest request, RoutingCostConfiguration costConfiguration) {
        validateRequest(graph, request, costConfiguration);
        if (request.blockedNodeIds().contains(request.startNodeId())
                || request.blockedNodeIds().contains(request.destinationNodeId())) {
            return RouteResult.noRoute();
        }
        Map<Long, Double> costs = new HashMap<>();
        Map<Long, PreviousStep> previousSteps = new HashMap<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingDouble(QueueEntry::estimatedTotalCost)
                .thenComparingLong(QueueEntry::nodeId));
        boolean canUseTimeHeuristic = !hasFasterThanGeometricTimeOverride(graph, costConfiguration);
        costs.put(request.startNodeId(), 0.0);
        queue.add(new QueueEntry(
                request.startNodeId(),
                0.0,
                heuristic(graph, request, costConfiguration, request.startNodeId(), canUseTimeHeuristic)));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.remove();
            if (isStale(current, costs)) {
                continue;
            }
            if (current.nodeId() == request.destinationNodeId()) {
                break;
            }
            relaxTraversals(graph, request, costConfiguration, current, costs, previousSteps, queue, canUseTimeHeuristic);
        }
        return createResult(graph, request, costConfiguration, previousSteps);
    }

    private boolean isStale(QueueEntry entry, Map<Long, Double> costs) {
        return entry.cost() > costs.getOrDefault(entry.nodeId(), Double.POSITIVE_INFINITY);
    }

    private void relaxTraversals(
            NavigationGraph graph,
            RouteRequest request,
            RoutingCostConfiguration configuration,
            QueueEntry current,
            Map<Long, Double> costs,
            Map<Long, PreviousStep> previousSteps,
            PriorityQueue<QueueEntry> queue,
            boolean canUseTimeHeuristic) {
        for (NavigationGraph.Traversal traversal : graph.traversalsFrom(current.nodeId())) {
            if (!isAvailable(graph, request, current.nodeId(), traversal)) {
                continue;
            }
            double candidateCost = current.cost() + traversalCost(traversal.edge(), request.preference(), configuration);
            double knownCost = costs.getOrDefault(traversal.destinationNodeId(), Double.POSITIVE_INFINITY);
            if (isBetter(candidateCost, knownCost)) {
                costs.put(traversal.destinationNodeId(), candidateCost);
                previousSteps.put(traversal.destinationNodeId(), new PreviousStep(current.nodeId(), traversal.edge()));
                double estimate = candidateCost
                        + heuristic(graph, request, configuration, traversal.destinationNodeId(), canUseTimeHeuristic);
                queue.add(new QueueEntry(traversal.destinationNodeId(), candidateCost, estimate));
            }
        }
    }

    private double heuristic(
            NavigationGraph graph,
            RouteRequest request,
            RoutingCostConfiguration configuration,
            long nodeId,
            boolean canUseTimeHeuristic) {
        NavigationNode node = graph.node(nodeId);
        NavigationNode destination = graph.node(request.destinationNodeId());
        double nodeScale = graph.metresPerUnit(node.floorId());
        double destinationScale = graph.metresPerUnit(destination.floorId());
        double deltaX = (node.x() * nodeScale) - (destination.x() * destinationScale);
        double deltaY = (node.y() * nodeScale) - (destination.y() * destinationScale);
        double horizontalDistance = Math.hypot(deltaX, deltaY);
        if (request.preference() == RoutePreference.SHORTEST) {
            return horizontalDistance;
        }
        if (!canUseTimeHeuristic) {
            return 0.0;
        }
        return horizontalDistance / configuration.fastestHorizontalSpeedMetresPerSecond();
    }

    private boolean hasFasterThanGeometricTimeOverride(
            NavigationGraph graph,
            RoutingCostConfiguration configuration) {
        for (long edgeId : graph.edgeIds()) {
            NavigationEdge edge = graph.edge(edgeId);
            if (edge.timeSeconds() != null
                    && edge.timeSeconds() < edge.distanceMetres() / configuration.fastestHorizontalSpeedMetresPerSecond()) {
                return true;
            }
        }
        return false;
    }

    private record QueueEntry(long nodeId, double cost, double estimatedTotalCost) {
    }
}
