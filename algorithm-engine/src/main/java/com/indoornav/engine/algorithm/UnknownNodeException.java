package com.indoornav.engine.algorithm;

/**
 * Thrown when a {@link RoutingRequest}'s start or destination node id does
 * not exist in the graph the request is run against.
 *
 * <p>This is the "engine-level invalid-input exception" described in
 * ARCHITECTURE.md Section 5.2; the backend is responsible for translating it
 * (PRD FR-19: "Invalid or unknown start/destination returns a validation or
 * not-found error, not a route").
 */
public class UnknownNodeException extends RuntimeException {

    public UnknownNodeException(long nodeId) {
        super("unknown node id: " + nodeId);
    }
}
