package com.github.alexgopen.javaish.model;

public class WorldCoord {
    public static final int MAX_X = 16384;
    public static final int MAX_Y = 8192;

    public final int x;
    public final int y;

    public WorldCoord(int x, int y) {
        this.x = modX(x);
        this.y = clampY(y);
    }

    private static int modX(int x) {
        int mx = x % MAX_X;
        if (mx < 0) mx += MAX_X;
        return mx;
    }

    private static int clampY(int y) {
        if (y < 0) return 0;
        if (y > MAX_Y) return MAX_Y;
        return y;
    }

    // Shortest delta from this to other, respecting wraparound
    public int deltaX(WorldCoord other) {
        int dx = other.x - this.x;
        if (dx > MAX_X / 2) dx -= MAX_X;
        if (dx < -MAX_X / 2) dx += MAX_X;
        return dx;
    }

    public int deltaY(WorldCoord other) {
        return other.y - this.y;
    }

    public double distanceTo(WorldCoord other) {
        int dx = deltaX(other);
        int dy = deltaY(other);
        return Math.hypot(dx, dy);
    }

    // Convert to map pixel coordinates (0-based)
    public Point toMapPixel() {
        int px = x / 4; // 1 pixel = 4 world coords
        int py = y / 4;
        return new Point(px, py);
    }

    public WorldCoord add(int dx, int dy) {
        return new WorldCoord(x + dx, y + dy);
    }

    public WorldCoord subtract(int dx, int dy) {
        return new WorldCoord(x - dx, y - dy);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}