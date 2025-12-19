package com.github.alexgopen.javaish.model;

public class TrackPoint {
    public final WorldCoord wCoord; // world coordinates
    public final PixelCoord pCoord; // map pixel coordinates
    public final long timestamp; // milliseconds
    public final double distanceFromPrev; // in world units
    public final long deltaTime; // ms since previous point

    public TrackPoint(WorldCoord world, PixelCoord map, long timestamp, double distanceFromPrev, long deltaTime) {
        this.wCoord = world;
        this.pCoord = map;
        this.timestamp = timestamp;
        this.distanceFromPrev = distanceFromPrev;
        this.deltaTime = deltaTime;
    }
}
