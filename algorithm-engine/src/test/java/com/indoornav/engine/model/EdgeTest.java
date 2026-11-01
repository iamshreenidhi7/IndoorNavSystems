package com.indoornav.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdgeTest {

    @Test
    void storesAllFields() {
        Edge edge = new Edge(1L, 10L, 20L, "CORRIDOR", 4.5, 12.0, true, true);

        assertEquals(1L, edge.id());
        assertEquals(10L, edge.fromNodeId());
        assertEquals(20L, edge.toNodeId());
        assertEquals("CORRIDOR", edge.connectionTypeCode());
        assertEquals(4.5, edge.distanceMeters());
        assertEquals(12.0, edge.timeSecondsOverride());
        assertEquals(true, edge.accessible());
        assertEquals(true, edge.bidirectional());
    }

    @Test
    void allowsNullTimeOverride() {
        Edge edge = new Edge(1L, 10L, 20L, "CORRIDOR", 4.5, null, true, true);

        assertEquals(null, edge.timeSecondsOverride());
    }

    @Test
    void rejectsBlankConnectionTypeCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new Edge(1L, 10L, 20L, "  ", 4.5, null, true, true));
    }

    @Test
    void rejectsNonPositiveDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Edge(1L, 10L, 20L, "CORRIDOR", 0.0, null, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Edge(1L, 10L, 20L, "CORRIDOR", -3.0, null, true, true));
    }

    @Test
    void rejectsNonPositiveTimeOverride() {
        assertThrows(IllegalArgumentException.class,
                () -> new Edge(1L, 10L, 20L, "CORRIDOR", 4.5, 0.0, true, true));
    }

    @Test
    void otherEndpointReturnsOppositeNode() {
        Edge edge = new Edge(1L, 10L, 20L, "CORRIDOR", 4.5, null, true, true);

        assertEquals(20L, edge.otherEndpoint(10L));
        assertEquals(10L, edge.otherEndpoint(20L));
    }

    @Test
    void otherEndpointRejectsNodeNotOnEdge() {
        Edge edge = new Edge(1L, 10L, 20L, "CORRIDOR", 4.5, null, true, true);

        assertThrows(IllegalArgumentException.class, () -> edge.otherEndpoint(99L));
    }
}
