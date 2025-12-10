package com.github.alexgopen.javaish.model;

public class TrackPoint {
    public final Point world; // world coordinates
    public final Point map; // map pixel coordinates
    public final long timestamp; // milliseconds
    public final double distanceFromPrev; // in world units
    public final long deltaTime; // ms since previous point

    public TrackPoint(Point world, Point map, long timestamp, double distanceFromPrev, long deltaTime) {
        this.world = world;
        this.map = map;
        this.timestamp = timestamp;
        this.distanceFromPrev = distanceFromPrev;
        this.deltaTime = deltaTime;
    }

    public double speed() {
        // units per second
        if (deltaTime <= 0)
            return 0;
        return (distanceFromPrev / deltaTime) * 1000.0;
    }
}
