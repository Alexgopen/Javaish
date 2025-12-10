package com.github.alexgopen.javaish.utils;

import com.github.alexgopen.javaish.model.Point;
import com.github.alexgopen.javaish.model.TrackPoint;

import java.util.ArrayList;
import java.util.List;

public class TrackManager {
    private final List<Point> points = new ArrayList<>();
    private final List<TrackPoint> trackPoints = new ArrayList<>();
    private long lastTime = -1;

    public List<Point> getPoints() { return points; }
    public List<TrackPoint> getTrackPoints() { return trackPoints; }

    public synchronized void addTrackPoint(Point worldCoord, Point mapCoord, int distTp, long deltaTimeTp) {
        long currentTime = System.currentTimeMillis();

        double newSpeed = deltaTimeTp > 0 ? distTp / (deltaTimeTp / 1000.0) : 0;
        double avgSpeed = averageSpeedLastN(5);

        if (avgSpeed > 0 && (newSpeed > 10 * avgSpeed && newSpeed > 3 || newSpeed >= 50)) {
            System.err.println("Skipped spike point: newSpeed=" + newSpeed + ", avgSpeed=" + avgSpeed);
            return;
        }

        if (currentTime - lastTime > 1000 && (points.isEmpty() || distance(mapCoord, points.get(points.size()-1)) >= 2)) {
            TrackPoint tp = new TrackPoint(worldCoord, mapCoord, currentTime, distTp, deltaTimeTp);
            trackPoints.add(tp);
            points.add(mapCoord);
            lastTime = currentTime;
        }
    }

    public double averageSpeedLastN(int n) {
        int start = Math.max(0, trackPoints.size() - n);
        double totalDist = 0;
        long totalTime = 0;
        for (int i = start+1; i < trackPoints.size(); i++) {
            TrackPoint tp = trackPoints.get(i);
            totalDist += tp.distanceFromPrev;
            totalTime += tp.deltaTime;
        }
        return totalTime > 0 ? totalDist / (totalTime / 1000.0) : 0;
    }

    private int distance(Point a, Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return (int)Math.sqrt(dx*dx + dy*dy);
    }
    
    public double getSmoothedSpeed(int lastN) {
        return CoordUtils.averageSpeedLastN(lastN, this.getTrackPoints());
    }
    
    public double getSmoothedHeading(int lastN) {
        return CoordUtils.averageHeadingLastN(lastN, this.getTrackPoints());
    }

    public double getDistanceNmi() {
        double units = 0;
        for (TrackPoint tp : trackPoints) { units += tp.distanceFromPrev; }
        double gvonavishNmiCircumference = (2 * Math.PI * 6378.137) / 1.852;
        double nmiFactor = gvonavishNmiCircumference / 16384.0;
        return units * nmiFactor;
    }
    
    public synchronized void clear() {
        points.clear();
        trackPoints.clear();
        lastTime = -1;
    }
}
