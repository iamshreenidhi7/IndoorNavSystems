package com.indoornav.engine.model;

/**
 * A point in the navigation graph.
 *
 * <p>Corresponds to one row of {@code nodes} (DATABASE.md Section 4.4), but is
 * a separate type from the backend's JPA entity (ARCHITECTURE.md Section 5.3):
 * the engine never depends on persistence classes.
 *
 * @param id         unique node identifier
 * @param floorId    identifier of the floor this node sits on
 * @param x          horizontal position in the floor's plan units, 0 or more
 * @param y          vertical position in the floor's plan units, 0 or more
 * @param nodeType   kind of node
 * @param accessible {@code false} for nodes unsuitable for step-free routing
 *                   (for example a stair landing)
 */
public record Node(long id, long floorId, double x, double y, NodeType nodeType, boolean accessible) {

    public Node {
        if (nodeType == null) {
            throw new IllegalArgumentException("nodeType must not be null for node " + id);
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("node " + id + " has a negative coordinate (" + x + ", " + y + ")");
        }
    }
}
