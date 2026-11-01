package com.indoornav.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NodeTest {

    @Test
    void storesAllFields() {
        Node node = new Node(1L, 10L, 5.0, 6.0, NodeType.CORRIDOR_JUNCTION, true);

        assertEquals(1L, node.id());
        assertEquals(10L, node.floorId());
        assertEquals(5.0, node.x());
        assertEquals(6.0, node.y());
        assertEquals(NodeType.CORRIDOR_JUNCTION, node.nodeType());
        assertEquals(true, node.accessible());
    }

    @Test
    void rejectsNullNodeType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Node(1L, 10L, 0.0, 0.0, null, true));
    }

    @Test
    void rejectsNegativeX() {
        assertThrows(IllegalArgumentException.class,
                () -> new Node(1L, 10L, -1.0, 0.0, NodeType.CORRIDOR_JUNCTION, true));
    }

    @Test
    void rejectsNegativeY() {
        assertThrows(IllegalArgumentException.class,
                () -> new Node(1L, 10L, 0.0, -1.0, NodeType.CORRIDOR_JUNCTION, true));
    }
}
