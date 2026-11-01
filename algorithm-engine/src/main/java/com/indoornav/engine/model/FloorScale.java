package com.indoornav.engine.model;

/**
 * Conversion factor from a floor's plan units to metres.
 *
 * <p>Corresponds to {@code floors.metres_per_unit} (DATABASE.md Section 4.2).
 * Phase 3's Dijkstra router does not use this value; it is accepted by
 * {@link com.indoornav.engine.graph.NavigationGraphBuilder} now, as task 3.2
 * of IMPLEMENTATION_PLAN.md requires ("per-floor scale"), so the A* heuristic
 * added in a later phase (ARCHITECTURE.md Section 5.5) can read it from the
 * graph without changing the builder's shape.
 *
 * @param floorId       identifier of the floor
 * @param metresPerUnit strictly positive conversion factor
 */
public record FloorScale(long floorId, double metresPerUnit) {

    public FloorScale {
        if (metresPerUnit <= 0) {
            throw new IllegalArgumentException(
                    "floor " + floorId + " has a non-positive metresPerUnit: " + metresPerUnit);
        }
    }
}
