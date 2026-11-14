package com.indoornav.engine.algorithm;

import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.RouteResult;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra implementation retained as the reference route finder.
 */
public final class DijkstraRouteFinder extends AbstractRouteFinder {
    @Override
    public RouteResult findRoute(NavigationGraph graph, RouteRequest request, RoutingCostConfiguration costConfiguration) {
        validateRequest(graph, request, costConfiguration);
        if (request.blockedNodeIds().contains(request.startNodeId())
                || request.blockedNodeIds().contains(request.destinationNodeId())) {
            return RouteResult.noRoute();
        }
        Map<Long, Double> costs = new HashMap<>();
        Map<Long, PreviousStep> previousSteps = new HashMap<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingDouble(QueueEntry::cost)
                .thenComparingLong(QueueEntry::nodeId));
        costs.put(request.startNodeId(), 0.0);
        queue.add(new QueueEntry(request.startNodeId(), 0.0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.remove();
            if (isStale(current, costs) || current.nodeId() == request.destinationNodeId()) {
                continue;
            }
            relaxTraversals(graph, request, costConfiguration, current, costs, previousSteps, queue);
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
            PriorityQueue<QueueEntry> queue) {
        for (NavigationGraph.Traversal traversal : graph.traversalsFrom(current.nodeId())) {
            if (!isAvailable(graph, request, current.nodeId(), traversal)) {
                continue;
            }
            double candidateCost = current.cost() + traversalCost(traversal.edge(), request.preference(), configuration);
            double knownCost = costs.getOrDefault(traversal.destinationNodeId(), Double.POSITIVE_INFINITY);
            if (isBetter(candidateCost, knownCost)) {
                costs.put(traversal.destinationNodeId(), candidateCost);
                previousSteps.put(traversal.destinationNodeId(), new PreviousStep(current.nodeId(), traversal.edge()));
                queue.add(new QueueEntry(traversal.destinationNodeId(), candidateCost));
            }
        }
    }

    private record QueueEntry(long nodeId, double cost) {
    }
}
