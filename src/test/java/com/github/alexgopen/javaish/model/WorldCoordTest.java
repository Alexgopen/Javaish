package com.github.alexgopen.javaish.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorldCoordTest {

    //
    // --- Construction, modX, clampY ---
    //

    @Test
    void testXWrapsAtBoundary() {
        WorldCoord c = new WorldCoord(16384, 1000);
        assertEquals(0, c.x);

        c = new WorldCoord(16385, 1000);
        assertEquals(1, c.x);

        c = new WorldCoord(-1, 1000);
        assertEquals(16383, c.x);

        c = new WorldCoord(-16384, 1000);
        assertEquals(0, c.x);
    }

    @Test
    void testXMultiWrap() {
        WorldCoord c = new WorldCoord(16384 * 3 + 5, 0);
        assertEquals(5, c.x);

        c = new WorldCoord(-16384 * 2 + 10, 0);
        assertEquals(10, c.x);
    }

    @Test
    void testClampY() {
        assertEquals(0, new WorldCoord(0, -10).y);
        assertEquals(8192, new WorldCoord(0, 9000).y);
        assertEquals(4000, new WorldCoord(0, 4000).y);
    }

    //
    // --- deltaX shortest wraparound logic ---
    //

    @Test
    void testDeltaX_NoWrap() {
        WorldCoord a = new WorldCoord(1000, 0);
        WorldCoord b = new WorldCoord(1500, 0);
        assertEquals(500, a.deltaX(b));
    }

    @Test
    void testDeltaX_WrapPositiveAcrossBoundary() {
        // from 16380 → 5 should be +9 (5 - 16380 = -16375 → +9 after wrap)
        WorldCoord a = new WorldCoord(16380, 0);
        WorldCoord b = new WorldCoord(5, 0);
        assertEquals(9, a.deltaX(b));
    }

    @Test
    void testDeltaX_WrapNegativeAcrossBoundary() {
        // from 5 → 16380 should be -9 (16380 - 5 = +16375 → -9 after wrap)
        WorldCoord a = new WorldCoord(5, 0);
        WorldCoord b = new WorldCoord(16380, 0);
        assertEquals(-9, a.deltaX(b));
    }

    @Test
    void testDeltaX_ExactlyHalfBoundaryPositive() {
        // MAX_X/2 = 8192
        // dx = 8192 → should NOT wrap (because dx is not > 8192)
        WorldCoord a = new WorldCoord(0, 0);
        WorldCoord b = new WorldCoord(8192, 0);
        assertEquals(8192, a.deltaX(b));
    }

    @Test
    void testDeltaX_JustOverHalfBoundaryPositive() {
        // dx = 8193 should wrap → dx -= 16384 = -8191
        WorldCoord a = new WorldCoord(0, 0);
        WorldCoord b = new WorldCoord(8193, 0);
        assertEquals(-8191, a.deltaX(b));
    }

    @Test
    void testDeltaX_JustOverHalfBoundaryNegative() {
        // negative dx = -8193 < -8192 → dx += 16384
        WorldCoord a = new WorldCoord(8193, 0);
        WorldCoord b = new WorldCoord(0, 0);
        assertEquals(8191, a.deltaX(b));
    }

    //
    // --- deltaY ---
    //

    @Test
    void testDeltaY() {
        WorldCoord a = new WorldCoord(0, 1000);
        WorldCoord b = new WorldCoord(0, 1200);
        assertEquals(200, a.deltaY(b));

        WorldCoord c = new WorldCoord(0, 200);
        assertEquals(-1000, b.deltaY(c));
    }

    //
    // --- distanceTo ---
    //

    @Test
    void testDistance_NoWrap() {
        WorldCoord a = new WorldCoord(0, 0);
        WorldCoord b = new WorldCoord(300, 400);
        assertEquals(500, a.distanceTo(b), 0.0001);
    }

    @Test
    void testDistance_WrapBoundary() {
        WorldCoord a = new WorldCoord(16380, 0);
        WorldCoord b = new WorldCoord(5, 12);

        int dx = a.deltaX(b);   // should be +9
        int dy = a.deltaY(b);   // 12

        double expected = Math.hypot(dx, dy);
        assertEquals(expected, a.distanceTo(b), 0.0001);
    }

    //
    // --- Pixel conversions ---
    //

    @Test
    void testToPixelCoord() {
        WorldCoord a = new WorldCoord(8, 12);
        PixelCoord p = a.toPixelCoord();
        assertEquals(2, p.x);
        assertEquals(3, p.y);
    }

    @Test
    void testFromPixelCoord() {
        PixelCoord p = new PixelCoord(100, 250);
        WorldCoord w = WorldCoord.fromPixelCoord(p);
        assertEquals(400, w.x);
        assertEquals(1000, w.y);
    }

    @Test
    void testPixelRoundTrip() {
        WorldCoord a = new WorldCoord(1234, 4321);
        PixelCoord p = a.toPixelCoord();
        WorldCoord b = WorldCoord.fromPixelCoord(p);

        // Not exact because division truncates, but must be consistent
        assertEquals(p.x * 4, b.x);
        assertEquals(p.y * 4, b.y);
    }

    //
    // --- toString ---
    //

    @Test
    void testToStringFormat() {
        WorldCoord a = new WorldCoord(100, 200);
        assertEquals("100, 200", a.toString());
    }
}
