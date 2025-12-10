package com.github.alexgopen.javaish.utils;

import java.util.List;

import com.github.alexgopen.javaish.model.Point;
import com.github.alexgopen.javaish.model.TrackPoint;

public class CoordUtils {

    public static int wrappedDelta(int a, int b) {
        int dx = a - b;
        if (dx > 8192) {
            dx -= 16384;
        }
        if (dx < -8192) {
            dx += 16384;
        }
        return dx;
    }

    public static double averageSpeedLastN(int n, List<TrackPoint> trackPoints) {
        if (trackPoints.size() < 2) {
            return 0;
        }
        int start = Math.max(0, trackPoints.size() - n);
        double totalDist = 0;
        long totalTime = 0;
        for (int i = start + 1; i < trackPoints.size(); i++) {
            totalDist += trackPoints.get(i).distanceFromPrev;
            totalTime += trackPoints.get(i).deltaTime;
        }
        if (totalTime == 0) {
            return 0;
        }
        double unitsPerSec = totalDist / totalTime * 1000.0; // units per second

        return gvonavishKt(unitsPerSec);
    }

    public static double gvonavishKt(double unitsPerSec) {
        // real earth circumference in km / ingame earth circumference coordinate units
        // / timescale / kmh per kt
        double knotsFactor = (2 * 3.141592654 * 6378.137) / 16384.0 / 0.4 / 1.852;
        return unitsPerSec * knotsFactor;
    }

    public static double averageHeadingLastN(int n, List<TrackPoint> trackPoints) {
        int size = trackPoints.size();
        if (size < 2) {
            return 0;
        }

        int start = Math.max(0, size - n);

        // Regression sums
        double sumT = 0;
        double sumT2 = 0;
        double sumX = 0;
        double sumXT = 0;
        double sumY = 0;
        double sumYT = 0;

        int count = size - start;
        if (count < 2) {
            return 0;
        }

        // base point for unwrapping
        Point base = trackPoints.get(start).world;

        // t = 0,1,2,... for the subset
        int t = 0;
        for (int i = start; i < size; i++, t++) {
            Point p = trackPoints.get(i).world;

            // unwrap relative to base to avoid wrap discontinuity
            int ux = wrappedDelta(p.x, base.x); // p.x - base.x (wrapped)
            int uy = wrappedDelta(base.y, p.y); // NOTE: match previous sign convention (prev.y - curr.y)

            sumT += t;
            sumT2 += t * t;
            sumX += ux;
            sumY += uy;
            sumXT += t * ux;
            sumYT += t * uy;
        }

        // least-squares slope denominator
        double denom = count * sumT2 - sumT * sumT;
        if (denom == 0) {
            return 0;
        }

        // slopes (Δx/Δt, Δy/Δt)
        double slopeX = (count * sumXT - sumT * sumX) / denom;
        double slopeY = (count * sumYT - sumT * sumY) / denom;

        if (slopeX == 0 && slopeY == 0) {
            return 0;
        }

        // Use same atan2 ordering as your old method: atan2(x, y)
        double angle = Math.toDegrees(Math.atan2(slopeX, slopeY));
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }
}
