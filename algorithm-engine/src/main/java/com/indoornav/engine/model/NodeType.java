package com.indoornav.engine.model;

/**
 * Kind of a graph node.
 *
 * <p>Mirrors the fixed set in {@code nodes.node_type} (DATABASE.md Section 4.4).
 * The engine does not branch its routing logic on this value in Phase 3; it is
 * carried on {@link Node} because ARCHITECTURE.md Section 5.3 lists type as
 * part of the graph model, and later phases (for example instruction wording
 * in the backend) rely on it being present.
 */
public enum NodeType {
    CORRIDOR_JUNCTION,
    ROOM_ENTRANCE,
    STAIR_LANDING,
    ELEVATOR_ENTRANCE,
    BUILDING_ENTRANCE,
    FLOOR_TRANSITION,
    POINT_OF_INTEREST
}
