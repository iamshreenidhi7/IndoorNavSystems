package com.indoornav.engine.result;

/**
 * Outcome of a routing request (ARCHITECTURE.md Section 5.2).
 *
 * <p>{@code NO_ROUTE} is a normal, valid outcome, never an error
 * (AGENTS.md Section 10.1, PRD FR-19/FR-20): the backend returns it to the
 * client as HTTP 200 with a reason code, rather than an error response.
 */
public enum RouteStatus {
    FOUND,
    NO_ROUTE
}
