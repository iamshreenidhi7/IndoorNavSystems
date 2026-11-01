package com.indoornav.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FloorScaleTest {

    @Test
    void storesFields() {
        FloorScale scale = new FloorScale(1L, 0.05);

        assertEquals(1L, scale.floorId());
        assertEquals(0.05, scale.metresPerUnit());
    }

    @Test
    void rejectsNonPositiveMetresPerUnit() {
        assertThrows(IllegalArgumentException.class, () -> new FloorScale(1L, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new FloorScale(1L, -0.1));
    }
}
