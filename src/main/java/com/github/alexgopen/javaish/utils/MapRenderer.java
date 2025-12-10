package com.github.alexgopen.javaish.utils;

import com.github.alexgopen.javaish.model.Point;
import com.github.alexgopen.javaish.model.TrackPoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class MapRenderer {
    private final BufferedImage imageMap;
    private final CoordConverter converter;
    private final TrackManager trackManager;

    public MapRenderer(BufferedImage imageMap, CoordConverter converter, TrackManager trackManager) {
        this.imageMap = imageMap;
        this.converter = converter;
        this.trackManager = trackManager;
    }

    public void render(Graphics g, Dimension panelSize, Point offset, Point mousePoint, boolean dragging) {
        Graphics2D g2 = (Graphics2D) g;

        // Draw map tiles
        int imgWidth = imageMap.getWidth();
        int x = offset.x % imgWidth;
        if (x > 0) x -= imgWidth;
        int y = offset.y;
        while (x < panelSize.width) {
            g.drawImage(imageMap, x - imgWidth, y, null);
            g.drawImage(imageMap, x, y, null);
            g.drawImage(imageMap, x + imgWidth, y, null);
            x += imgWidth;
        }

        // Draw points/track
        drawPoints(g2);

        // Draw trajectory heading
        drawTrajectory(g2, panelSize);

        // Draw player offscreen indicator
        drawPlayerArrow(g2, panelSize);

        // Draw hover coordinates
        drawHoverCoords(g2, mousePoint, dragging, offset);

        // Draw instructions
        drawInstructions(g2, panelSize);

        // Draw statistics
        drawStats(g2);
    }

    private void drawPoints(Graphics2D g2) {
        List<TrackPoint> points = trackManager.getTrackPoints();
        if (points.isEmpty()) return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw track line and points
        g2.setColor(Color.RED);
        for (int i = 0; i < points.size(); i++) {
            TrackPoint tp = points.get(i);
            Point p = converter.worldToMap(tp.world);
            g2.fillOval(p.x - 1, p.y - 1, 2, 2);
            if (i > 0) {
                TrackPoint prevTp = points.get(i - 1);
                Point prev = converter.worldToMap(prevTp.world);
                g2.drawLine(prev.x, prev.y, p.x, p.y);
            }
        }

        // Draw current player position as green dot
        TrackPoint playerTp = points.get(points.size() - 1);
        Point player = converter.worldToMap(playerTp.world);
        g2.setColor(Color.GREEN);
        g2.fillOval(player.x - 3, player.y - 3, 6, 6);
    }

    private void drawTrajectory(Graphics2D g2, Dimension panelSize) {
        List<TrackPoint> trackPoints = trackManager.getTrackPoints();
        if (trackPoints.size() < 2) return;

        TrackPoint last = trackPoints.get(trackPoints.size() - 1);
        Point cur = last.world;

        double heading = trackManager.getSmoothedHeading(5) - 90; // degrees
        double rad = Math.toRadians(heading);
        double dx = Math.cos(rad);
        double dy = Math.sin(rad);

        if (dx != 0 || dy != 0) {
            int width = panelSize.width;
            int height = panelSize.height;
            double tMax = Double.MAX_VALUE;

            if (dx > 0) tMax = (width - 1 - cur.x) / dx;
            else if (dx < 0) tMax = -cur.x / dx;
            if (dy > 0) tMax = Math.min(tMax, (height - 1 - cur.y) / dy);
            else if (dy < 0) tMax = Math.min(tMax, -cur.y / dy);

            int endX = cur.x + (int) (dx * tMax);
            int endY = cur.y + (int) (dy * tMax);

            g2.setColor(Color.CYAN);
            g2.drawLine(cur.x, cur.y, endX, endY);
        }
    }

    private void drawPlayerArrow(Graphics2D g2, Dimension panelSize) {
        List<Point> points = trackManager.getPoints();
        if (points.isEmpty()) return;
        Point player = points.get(points.size() - 1);
        if (!isOffscreen(player, panelSize)) return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = panelSize.width / 2;
        int cy = panelSize.height / 2;
        double angle = Math.atan2(player.y - cy, player.x - cx);
        Point edge = getEdgePoint(player, panelSize);

        // Glow
        int glowSize = 36;
        int glowX = edge.x - glowSize / 2;
        int glowY = edge.y - glowSize / 2;
        g2.setColor(new Color(0, 255, 0, 128));
        g2.fillOval(glowX, glowY, glowSize, glowSize);

        // Triangle
        int size = 16;
        int tipShift = 8;
        int tipX = edge.x + (int) (tipShift * Math.cos(angle));
        int tipY = edge.y + (int) (tipShift * Math.sin(angle));
        int[] xs = new int[]{
                tipX,
                edge.x - (int) (size * Math.cos(angle - Math.PI / 6)),
                edge.x - (int) (size * Math.cos(angle + Math.PI / 6))
        };
        int[] ys = new int[]{
                tipY,
                edge.y - (int) (size * Math.sin(angle - Math.PI / 6)),
                edge.y - (int) (size * Math.sin(angle + Math.PI / 6))
        };
        g2.setColor(Color.GREEN);
        g2.fillPolygon(xs, ys, 3);

        // Arrow tail
        int tailLength = 26;
        int tailX = edge.x - (int) (tailLength * Math.cos(angle));
        int tailY = edge.y - (int) (tailLength * Math.sin(angle));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(tailX, tailY, edge.x, edge.y);
    }

    private boolean isOffscreen(Point p, Dimension panelSize) {
        return p.x < 0 || p.x > panelSize.width || p.y < 0 || p.y > panelSize.height;
    }

    private Point getEdgePoint(Point p, Dimension panelSize) {
        int cx = panelSize.width / 2;
        int cy = panelSize.height / 2;
        double dx = p.x - cx;
        double dy = p.y - cy;
        double scale = 1.0;

        if (dx != 0) scale = Math.min(scale, (dx > 0) ? (panelSize.width - 10 - cx) / dx : (10 - cx) / dx);
        if (dy != 0) scale = Math.min(scale, (dy > 0) ? (panelSize.height - 10 - cy) / dy : (10 - cy) / dy);

        return new Point(cx + (int) (dx * scale), cy + (int) (dy * scale));
    }

    private void drawHoverCoords(Graphics2D g2, Point mouse, boolean dragging, Point offset) {
        if (mouse.x == Integer.MIN_VALUE) return;
        Point world = converter.screenToWorld(mouse, offset);
        String coords = String.format("%d, %d", world.x, world.y);
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(mouse.x, mouse.y - 16, coords.length() * 7, 16);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Verdana", Font.BOLD, 12));
        g2.drawString(coords, mouse.x + 4, mouse.y - 4);
    }

    private void drawInstructions(Graphics2D g2, Dimension panelSize) {
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(15, panelSize.height - 65, 173, 50);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Verdana", Font.PLAIN, 20));
        g2.drawString("Left-Click to drag", 22, panelSize.height - 44);
        g2.drawString("R to clear plot", 22, panelSize.height - 24);
    }

    private void drawStats(Graphics2D g2) {
        List<TrackPoint> trackPoints = trackManager.getTrackPoints();
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Verdana", Font.PLAIN, 20));
        int y = 30, inc = 30;

        Point p = new Point(0,0);
        if (!trackPoints.isEmpty()) {
            TrackPoint last = trackPoints.get(trackPoints.size() - 1);
            p = last.world;
        }
        
        g2.drawString("Coords: " + p.x + ", " + p.y, 15, y); y += inc;

        double heading = trackManager.getSmoothedHeading(5);
        g2.drawString(String.format("Rotation: %.0f deg", heading), 15, y); y += inc;

        double speed = trackManager.getSmoothedSpeed(5);
        g2.drawString(String.format("Speed: %3.2f kt", speed), 15, y); y += inc;

        double distance = trackManager.getDistanceNmi();
        g2.drawString(String.format("Distance: %3.2f nmi", distance), 15, y); y += inc;
    }
}
