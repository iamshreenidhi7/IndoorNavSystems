package com.indoornav.engine.algorithm;

import java.util.Set;

/**
 * One routing query.
 *
 * <p>Scoped to what Phase 3 routes on: a start, a destination, and the
 * obstacles to avoid (IMPLEMENTATION_PLAN.md Phase 3, task 3.3: "Dijkstra
 * with blocked node and edge IDs"). ARCHITECTURE.md Section 5.2 also lists a
 * preference and a cost configuration as request inputs; those arrive with
 * the cost model in a later phase (IMPLEMENTATION_PLAN.md Phase 4), at which
 * point SHORTEST (pure distance, what this phase already computes) becomes
 * one case among several rather than the only behaviour.
 *
 * @param startNodeId       id of the node to start from
 * @param destinationNodeId id of the node to reach
 * @param blockedNodeIds    node ids to exclude from routing (from active
 *                          obstacles); never {@code null} in the stored value,
 *                          a {@code null} argument is treated as empty
 * @param blockedEdgeIds    edge ids to exclude from routing; same null
 *                          handling as {@code blockedNodeIds}
 */
public record RoutingRequest(long startNodeId, long destinationNodeId, Set<Long> blockedNodeIds, Set<Long> blockedEdgeIds) {

    public RoutingRequest {
        blockedNodeIds = blockedNodeIds == null ? Set.of() : Set.copyOf(blockedNodeIds);
        blockedEdgeIds = blockedEdgeIds == null ? Set.of() : Set.copyOf(blockedEdgeIds);
    }
}
