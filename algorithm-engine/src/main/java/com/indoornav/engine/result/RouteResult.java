package com.indoornav.engine.result;

import java.util.List;

/**
 * Outcome of a single routing request.
 *
 * <p>Scoped to what Phase 3 ("a correct, isolated, in-memory shortest-distance
 * router", IMPLEMENTATION_PLAN.md) can compute: status, the ordered path and
 * its total distance. ARCHITECTURE.md Section 5.2 also lists total estimated
 * time and floor transitions as part of the engine's result contract; those
 * fields are added once the cost model (time) and multi-floor extraction
 * exist (IMPLEMENTATION_PLAN.md Phase 4, tasks 4.2 and 4.5) rather than being
 * carried here as placeholders with nothing to compute them yet.
 *
 * <p>Normally built through {@link #found} or {@link #noRoute}; the canonical
 * constructor's own validation keeps any instance internally consistent
 * regardless of how it is constructed.
 *
 * @param status          {@link RouteStatus#FOUND} or {@link RouteStatus#NO_ROUTE}
 * @param nodeIds         ordered path node ids; empty when there is no route
 * @param edgeIds         ordered path edge ids; empty when there is no route
 *                         or when start equals destination
 * @param distanceMeters  total distance; 0 when there is no route
 */
public record RouteResult(RouteStatus status, List<Long> nodeIds, List<Long> edgeIds, double distanceMeters) {

    public RouteResult {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("distanceMeters must not be negative: " + distanceMeters);
        }
        if (status == RouteStatus.NO_ROUTE && (!nodeIds.isEmpty() || !edgeIds.isEmpty() || distanceMeters != 0)) {
            throw new IllegalArgumentException("a NO_ROUTE result must have no path and zero distance");
        }
    }

    /** A route was found. {@code nodeIds} must start at the request's start node and end at its destination. */
    public static RouteResult found(List<Long> nodeIds, List<Long> edgeIds, double distanceMeters) {
        return new RouteResult(RouteStatus.FOUND, List.copyOf(nodeIds), List.copyOf(edgeIds), distanceMeters);
    }

    /** No path exists between the requested start and destination under the given constraints. */
    public static RouteResult noRoute() {
        return new RouteResult(RouteStatus.NO_ROUTE, List.of(), List.of(), 0.0);
    }
}
