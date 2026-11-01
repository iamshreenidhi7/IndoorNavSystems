package com.indoornav.engine.algorithm;

import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.result.RouteResult;

/**
 * The engine's one stable public entry point (ARCHITECTURE.md Section 5.2:
 * "one stable interface with two interchangeable implementations"). Dijkstra
 * implements it from Phase 3; A* is added behind the same interface in
 * IMPLEMENTATION_PLAN.md Phase 5, so the backend can switch implementations
 * without any caller-visible change.
 *
 * <p>Implementations MUST be deterministic and free of side effects
 * (AGENTS.md Section 7): the same graph and request always produce the same
 * result.
 */
public interface RoutingAlgorithm {

    /**
     * Finds a route for {@code request} over {@code graph}.
     *
     * @throws UnknownNodeException if the request's start or destination node
     *                              does not exist in {@code graph}
     */
    RouteResult findRoute(NavigationGraph graph, RoutingRequest request);
}
