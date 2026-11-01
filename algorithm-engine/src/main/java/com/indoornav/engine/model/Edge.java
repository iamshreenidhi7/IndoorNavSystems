package com.indoornav.engine.model;

/**
 * One physical connection between two nodes.
 *
 * <p>Corresponds to one row of {@code edges} (DATABASE.md Section 4.5). A
 * bidirectional edge is stored as a single {@code Edge}; {@link
 * com.indoornav.engine.graph.NavigationGraphBuilder} is responsible for
 * expanding it into both directions when it builds the adjacency list
 * (ARCHITECTURE.md Section 5.3).
 *
 * <p>{@code connectionTypeCode} is an opaque reference to a row in
 * {@code connection_types} (DATABASE.md Section 4.3). The engine treats
 * every code generically in Phase 3; the reserved codes {@code STAIRS} and
 * {@code ELEVATOR} only gain special meaning once the preference surcharges
 * are implemented (ARCHITECTURE.md Section 5.4).
 *
 * @param id                  unique edge identifier
 * @param fromNodeId          one endpoint's node id
 * @param toNodeId             the other endpoint's node id
 * @param connectionTypeCode  code of the connection type, never blank
 * @param distanceMeters      physical distance in metres; strictly positive
 *                            (AGENTS.md Section 7: "All edge costs MUST be
 *                            strictly positive")
 * @param timeSecondsOverride explicit travel time in seconds, or {@code null}
 *                            when time should instead be derived from the
 *                            connection type's speed and fixed time
 *                            (DATABASE.md Section 4.5); when present, strictly
 *                            positive. Not yet used by Phase 3's distance-only
 *                            routing; carried so the cost model added in a
 *                            later phase does not need to change this type.
 * @param accessible          {@code false} for edges unsuitable for step-free
 *                            routing (for example a flight of stairs)
 * @param bidirectional       whether the connection can be traversed in
 *                            either direction
 */
public record Edge(
        long id,
        long fromNodeId,
        long toNodeId,
        String connectionTypeCode,
        double distanceMeters,
        Double timeSecondsOverride,
        boolean accessible,
        boolean bidirectional) {

    public Edge {
        if (connectionTypeCode == null || connectionTypeCode.isBlank()) {
            throw new IllegalArgumentException("edge " + id + " has a blank connectionTypeCode");
        }
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("edge " + id + " has a non-positive distance: " + distanceMeters);
        }
        if (timeSecondsOverride != null && timeSecondsOverride <= 0) {
            throw new IllegalArgumentException(
                    "edge " + id + " has a non-positive time override: " + timeSecondsOverride);
        }
    }

    /**
     * The node id on the other side of this edge from {@code nodeId}.
     *
     * <p>Used when walking the graph from either endpoint, since a single
     * {@code Edge} instance represents the connection regardless of the
     * direction it is traversed in.
     *
     * @throws IllegalArgumentException if {@code nodeId} is neither endpoint
     */
    public long otherEndpoint(long nodeId) {
        if (nodeId == fromNodeId) {
            return toNodeId;
        }
        if (nodeId == toNodeId) {
            return fromNodeId;
        }
        throw new IllegalArgumentException("node " + nodeId + " is not an endpoint of edge " + id);
    }
}
