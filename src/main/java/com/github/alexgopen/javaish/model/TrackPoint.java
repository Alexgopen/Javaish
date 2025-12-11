package com.github.alexgopen.javaish.model;

public class TrackPoint {
    public final Point world; // world coordinates
    public final long timestamp; // milliseconds
    public final double distanceFromPrev; // in world units
    public final long deltaTime; // ms since previous point
    public final Point offset;

    public TrackPoint(Point world, long timestamp, double distanceFromPrev, long deltaTime, Point offset) {
        this.world = world;
        this.timestamp = timestamp;
        this.distanceFromPrev = distanceFromPrev;
        this.deltaTime = deltaTime;
        this.offset = offset;
    }
}
