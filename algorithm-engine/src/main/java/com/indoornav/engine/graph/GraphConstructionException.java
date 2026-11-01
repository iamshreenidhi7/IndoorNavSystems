package com.indoornav.engine.graph;

/**
 * Thrown by {@link NavigationGraphBuilder#build()} when the supplied nodes,
 * edges or floor scales cannot form a valid immutable graph (for example an
 * edge referencing an unknown node, or a duplicate identifier).
 *
 * <p>This is one of the engine's small, documented exception set
 * (IMPLEMENTATION_PLAN.md Phase 3, task 3.1; ARCHITECTURE.md Section 5.2).
 */
public class GraphConstructionException extends RuntimeException {

    public GraphConstructionException(String message) {
        super(message);
    }
}
