package com.github.alexgopen.javaish.application;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Point;
import com.github.alexgopen.javaish.model.TrackPoint;
import com.github.alexgopen.javaish.provider.CoordProvider;
import com.github.alexgopen.javaish.provider.MapImageProvider;
import com.github.alexgopen.javaish.utils.CoordMathUtils;

public class Javaish extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private static final long serialVersionUID = -1668129614007560894L;
    
    private static final long tickRate = 250;
    
    private BufferedImage imageMap;
    private BufferedImage trackLayer;
    private CoordProvider coordProvider;

    private Point imageDimms = new Point(0, 0);
    private Point lastPoint = new Point(0, 0);
    private Point offsetPoint = new Point(0, 0);
    private Point mousePoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private Point worldPoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

    // Seville is a common starting point
    private Point initialCenterCoord = new Point(15903, 3271);

    private boolean rclick = false;

    private List<TrackPoint> trackPoints = new ArrayList<>();

    boolean dragging;

    private long lastTime = -1;

    private boolean firstRender = true;

    public Javaish() {
        try {
            BufferedImage loadedImg = MapImageProvider.loadMap();
            
            imageMap = loadedImg;
            imageDimms.x = imageMap.getWidth();
            imageDimms.y = imageMap.getHeight();
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to load map file, exiting.");
            System.exit(1);
        }

        this.setFocusable(true);
        this.requestFocus();
        this.setPreferredSize(new Dimension(800, 600));
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        
        this.trackLayer = new BufferedImage(imageDimms.x, imageDimms.y, BufferedImage.TYPE_INT_ARGB);


        // Auto-center before first render
        this.centerOnInitialCoord();

        try {
            this.coordProvider = new CoordProvider();
        }
        catch (AWTException e) {
            System.err.println("Failed to instantiate CoordProvider.");
            e.printStackTrace();
            System.exit(1);
        }


        this.startCoordThread(this);
    }

    private void startCoordThread(Javaish javaishInstance) {        
        Thread coordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(tickRate);
                        
                        Point coord = coordProvider.getCoord();
                        Point worldCoord = coord;
                        int distTp = 999;

                        long currentTime = System.currentTimeMillis();
                        long timeDelta = currentTime - lastTime;

                        long deltaTimeTp = 0;
                        if (!trackPoints.isEmpty()) {
                            TrackPoint last = trackPoints.get(trackPoints.size() - 1);
                            deltaTimeTp = currentTime - last.timestamp;

                            int dx = CoordMathUtils.wrappedDelta(worldCoord.x, last.world.x);
                            int dy = CoordMathUtils.wrappedDelta(worldCoord.y, last.world.y);

                            distTp = (int) Math.sqrt(dx * dx + dy * dy);
                        }

                        // Estimate speed in units/sec
                        double newSpeed = deltaTimeTp > 0 ? distTp / (deltaTimeTp / 1000.0) : 0;

                        // Get average speed of last N points in units/sec
                        double avgSpeed = 0;
                        int N = 5;
                        if (trackPoints.size() > 1) {
                            int start = Math.max(0, trackPoints.size() - N);
                            double totalDist = 0;
                            long totalTime = 0;
                            for (int i = start + 1; i < trackPoints.size(); i++) {
                                totalDist += trackPoints.get(i).distanceFromPrev;
                                totalTime += trackPoints.get(i).deltaTime;
                            }
                            if (totalTime > 0) {
                                avgSpeed = totalDist / (totalTime / 1000.0);
                            }
                        }

                        // Skip this point if speed is more than 5x the recent average and greater than
                        // 2
                        if (avgSpeed > 0 && (newSpeed > 10 * avgSpeed && newSpeed > 3 || newSpeed >= 50)) {
                            System.err.println("Skipped spike point: newSpeed=" + newSpeed + ", avgSpeed=" + avgSpeed);
                            continue; // skip adding this point
                        }

                        if (timeDelta > 1000 && distTp >= 1) {
                            TrackPoint tp = new TrackPoint(worldCoord, currentTime, distTp, deltaTimeTp, new Point(offsetPoint.x, offsetPoint.y));
                            trackPoints.add(tp);
                            if (distTp != 999) {
                                System.err.println("Dist=" + distTp + ", delta=" + timeDelta + ", pos="
                                        + tp.world.toString() + ", newSpeed=" + newSpeed + ", avgSpeed=" + avgSpeed+", tpoffsetx="+tp.offset.x+", tpoffsety="+tp.offset.y);
                            }
                            lastTime = currentTime;
                            updateTrackLayer();
                            repaint();
                        }

                    }
                    catch (CoordNotFoundException cnfe) {
                        if (!coordProvider.onCooldown()) {
                            System.err.println("Coord not found.");
                        }
                        coordProvider.resetPrevFoundCoordLoc();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        coordThread.start();
    }


    private void centerOnInitialCoord() {
        // convert initialCenterCoord from world to map pixels
        double xW = initialCenterCoord.x / 1000.0 * 250.0;
        double yW = initialCenterCoord.y / 1000.0 * 250.0;

        int centerX = getPreferredSize().width / 2;
        int centerY = getPreferredSize().height / 2;

        int desiredOffsetX = centerX - (int) xW;
        int desiredOffsetY = centerY - (int) yW;

        // clamp vertical offset
        int topLimit = 0;
        int bottomLimit = Math.min(0, getHeight() - imageDimms.y);

        if (desiredOffsetY > topLimit)
            desiredOffsetY = topLimit;
        if (desiredOffsetY < bottomLimit)
            desiredOffsetY = bottomLimit;

        offsetPoint.x = desiredOffsetX;
        offsetPoint.y = desiredOffsetY;

        System.out.println("Initial view centered at " + initialCenterCoord + " with offset " + offsetPoint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int x = offsetPoint.x % imageDimms.x;
        if (x > 0) {
            x -= imageDimms.x;
        }

        int y = offsetPoint.y;

        while (x < getWidth()) {
            g.drawImage(imageMap, (x - imageDimms.x), y, null);
            g.drawImage(imageMap, x, y, null);
            g.drawImage(imageMap, (x + imageDimms.x), y, null);
            x += imageDimms.x;
        }

        // renderPoints(g);
        // renderPointsList(g);
        renderTrack((Graphics2D) g);
        renderHint(g);
        renderText(g);
        renderHover(g);
        renderPlayerArrow(g); // we’ll refactor arrow into a separate method
        renderPlayerDot(g);
    }
    
    private void drawWrappedLine(Graphics2D g2, Point p0, Point p1) {
        final int MAX_WORLD = 16384;

        int dx = p1.x - p0.x;

        // Check for crossing left-to-right
        if (dx > MAX_WORLD / 2) {
            // Crossing from near 0 to left
            int boundaryX = 0;
            double ratio = (double) (boundaryX - p0.x) / dx;
            int yBoundary = p0.y + (int) ((p1.y - p0.y) * ratio);
            g2.drawLine(p0.x, p0.y, boundaryX, yBoundary);
            return;
        }

        // Check for crossing right-to-left
        if (dx < -MAX_WORLD / 2) {
            // Crossing from near right to 0
            int boundaryX = MAX_WORLD;
            double ratio = (double) (boundaryX - p0.x) / dx;
            int yBoundary = p0.y + (int) ((p1.y - p0.y) * ratio);
            g2.drawLine(p0.x, p0.y, boundaryX, yBoundary);
            return;
        }

        // Normal line
        g2.drawLine(p0.x, p0.y, p1.x, p1.y);
    }


    
    private void renderTrack(Graphics2D g2) {
        if (trackPoints.isEmpty()) return;

        int[] offsetsX = {-imageDimms.x, 0, imageDimms.x}; // tile horizontally
        int[] offsetsY = {0}; // optionally add vertical tiling if needed

        g2.setColor(new Color(255, 0, 0, 255));

        for (TrackPoint tp : trackPoints) {
            Point p = purelyConvertWtoM(tp);

            for (int ox : offsetsX) {
                for (int oy : offsetsY) {
                    int px = p.x + ox;
                    int py = p.y + oy;
                    g2.fillOval(px - 1, py - 1, 2, 2);
                }
            }
        }

        // Draw connecting lines
        for (int i = 1; i < trackPoints.size(); i++) {
            Point prev = purelyConvertWtoM(trackPoints.get(i - 1));
            Point curr = purelyConvertWtoM(trackPoints.get(i));
            for (int ox : offsetsX) {
                int px0 = prev.x + ox;
                int px1 = curr.x + ox;

                drawWrappedLine(g2, new Point(px0, prev.y), new Point(px1, curr.y));
            }
        }
    }

    
    private void renderPlayerArrow(Graphics g) {
        if (trackPoints.isEmpty()) return;

        TrackPoint lastTP = trackPoints.get(trackPoints.size() - 1);
        Point player = purelyConvertWtoM(lastTP);

        if (!isOffscreen(player)) return;

        Graphics2D g2 = (Graphics2D) g;
        // Enable antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Point edge = getEdgePoint(player);

        int size = 16;        // triangle size
        int tailLength = 26;  // arrow tail
        int tipShift = 8;     // shift forward
        int glowShift = -10;  // glow shift

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        double angle = Math.atan2(player.y - cy, player.x - cx);

        // Tip shifted forward
        int tipX = edge.x + (int) (tipShift * Math.cos(angle));
        int tipY = edge.y + (int) (tipShift * Math.sin(angle));

        // Glow
        float pulse = 0.75f;
        int glowSize = (int) (size * 2 + pulse * 10);
        int glowX = edge.x + (int) (glowShift * Math.cos(angle));
        int glowY = edge.y + (int) (glowShift * Math.sin(angle));
        g2.setColor(new Color(0, 255, 0, (int) (128 * pulse)));
        g2.fillOval(glowX - glowSize / 2, glowY - glowSize / 2, glowSize, glowSize);

        // Triangle
        int[] xs = {tipX, edge.x - (int)(size*Math.cos(angle-Math.PI/6)), edge.x - (int)(size*Math.cos(angle+Math.PI/6))};
        int[] ys = {tipY, edge.y - (int)(size*Math.sin(angle-Math.PI/6)), edge.y - (int)(size*Math.sin(angle+Math.PI/6))};
        g2.setColor(Color.GREEN);
        g2.fillPolygon(xs, ys, 3);

        // Tail
        int tailX = edge.x - (int)(tailLength * Math.cos(angle));
        int tailY = edge.y - (int)(tailLength * Math.sin(angle));
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawLine(tailX, tailY, edge.x, edge.y);
    }

    
    private void updateTrackLayer() {
        if (trackLayer == null) return;

        Graphics2D g2 = trackLayer.createGraphics();

        // Clear previous content
        g2.setComposite(java.awt.AlphaComposite.Clear);
        g2.fillRect(0, 0, trackLayer.getWidth(), trackLayer.getHeight());
        g2.setComposite(java.awt.AlphaComposite.SrcOver);

        // Antialiasing for smooth lines
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw all track points
        for (int i = 0; i < trackPoints.size(); i++) {
            TrackPoint tp = trackPoints.get(i);
            Point p = purelyConvertWtoM(tp);

            // Draw point
            g2.setColor(new Color(255, 0, 0, 255));
            g2.fillOval(p.x - 1, p.y - 1, 2, 2);

            // Draw line from previous
            if (i > 0) {
                TrackPoint prev = trackPoints.get(i - 1);
                Point prevP = purelyConvertWtoM(prev);
                g2.drawLine(prevP.x, prevP.y, p.x, p.y);
            }
        }

        g2.dispose();
    }
    
    private void renderPlayerDot(Graphics g) {
        if (trackPoints.isEmpty()) return;

        TrackPoint lastTP = trackPoints.get(trackPoints.size() - 1);
        Point player = purelyConvertWtoM(lastTP);

        Graphics2D g2 = (Graphics2D) g;

        // Enable antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw green dot at player location
        g2.setColor(new Color(0, 255, 0, 255));
        g2.fillOval(player.x - 3, player.y - 3, 6, 6);
    }

    public void renderPointsList(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Save old hints
        RenderingHints oldHints = g2.getRenderingHints();

        int prevPointX = Integer.MIN_VALUE;
        int prevPointY = Integer.MIN_VALUE;

        int curPointX = Integer.MIN_VALUE;
        int curPointY = Integer.MIN_VALUE;

        for (int i = 0; i < trackPoints.size(); i++) {
            TrackPoint tp = trackPoints.get(i);
            Point mapCoord = convertWtoM(tp);
            
            Point p = mapCoord;

            if (i == trackPoints.size() - 2) {
                prevPointX = p.x;
                prevPointY = p.y;
            }

            if (i == trackPoints.size() - 1) {
                curPointX = p.x;
                curPointY = p.y;
            }

            if (i > 0) {
                g2.setColor(new Color(255, 0, 0, 255));
                
                TrackPoint tpPrev = trackPoints.get(i - 1);
                Point prev = convertWtoM(tpPrev);
                TrackPoint tpCurr = trackPoints.get(i);
                Point curr =  convertWtoM(tpCurr);
                g2.drawLine(prev.x, prev.y, curr.x, curr.y);
            }

            g2.setColor(new Color(255, 0, 0, 255));
            g2.fillOval(p.x - 1, p.y - 1, 2, 2);
        }

        if (prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE) {
            int transX = prevPointX;
            int transY = prevPointY;

            g2.setColor(new Color(255, 0, 0, 255));
            g2.fillOval(transX - 1, transY - 1, 2, 2);
        }
        if (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE) {
            int transX = curPointX;
            int transY = curPointY;

            // Enable full antialiasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g2.setColor(new Color(0, 255, 0, 255));
            g2.fillOval(transX - 3, transY - 3, 6, 6);

            g2.setRenderingHints(oldHints);
        }

        if ((prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE)
                && (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE)) {
            g2.setColor(new Color(255, 0, 0, 255));
            g2.drawLine(prevPointX, prevPointY, curPointX, curPointY);

            // Enable full antialiasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // WITH THIS:
            double smoothedHeadingDeg = CoordMathUtils.averageHeadingLastN(10, trackPoints) - 90;
            double smoothedHeadingRad = Math.toRadians(smoothedHeadingDeg);

            double dx = Math.cos(smoothedHeadingRad);
            double dy = Math.sin(smoothedHeadingRad);

            if (dx != 0 || dy != 0) {
                int width = getWidth();
                int height = getHeight();

                double tMax = Double.MAX_VALUE;

                // Intersect with vertical edges
                if (dx > 0)
                    tMax = (width - 1 - curPointX) / dx;
                else if (dx < 0)
                    tMax = -curPointX / dx;

                // Intersect with horizontal edges
                if (dy > 0)
                    tMax = Math.min(tMax, (height - 1 - curPointY) / dy);
                else if (dy < 0)
                    tMax = Math.min(tMax, -curPointY / dy);

                int endXHeading = curPointX + (int) (dx * tMax);
                int endYHeading = curPointY + (int) (dy * tMax);

                g2.setColor(new Color(0, 255, 255, 255));
                g2.drawLine(curPointX, curPointY, endXHeading, endYHeading);
            }

            int transX = curPointX;
            int transY = curPointY;

            g2.setColor(new Color(0, 255, 0, 255));
            g2.fillOval(transX - 3, transY - 3, 6, 6);
        }

        Point player = null;

        if (trackPoints.size() > 0) {
            player = convertWtoM(trackPoints.get(trackPoints.size() - 1));
        }

        if (player != null && isOffscreen(player)) {
            // Enable full antialiasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            Point edge = getEdgePoint(player);
            int size = 16; // triangle size
            int tailLength = 26; // length of the arrow tail
            int tipShift = 8; // how much to move the triangle tip forward
            int glowShift = -10; // shift the glow slightly forward

            // Cast to Graphics2D for advanced drawing
            Graphics2D g2d = (Graphics2D) g2;

            // Triangle pointing to player
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            double angle = Math.atan2(player.y - cy, player.x - cx);

            // Shift tip forward
            int tipX = edge.x + (int) (tipShift * Math.cos(angle));
            int tipY = edge.y + (int) (tipShift * Math.sin(angle));

            // Pulsating glow (shifted slightly forward)
            float pulse = 0.75f;
            int glowSize = (int) (size * 2 + pulse * 10); // glow radius varies
            int glowX = edge.x + (int) (glowShift * Math.cos(angle));
            int glowY = edge.y + (int) (glowShift * Math.sin(angle));
            Color glowColor = new Color(0, 255, 0, (int) (128 * pulse)); // translucent green
            g2d.setColor(glowColor);
            g2d.fillOval(glowX - glowSize / 2, glowY - glowSize / 2, glowSize, glowSize);

            // Draw the triangle
            int[] xs = new int[] { tipX, // tip shifted forward
                    edge.x - (int) (size * Math.cos(angle - Math.PI / 6)),
                    edge.x - (int) (size * Math.cos(angle + Math.PI / 6)) };
            int[] ys = new int[] { tipY, // tip shifted forward
                    edge.y - (int) (size * Math.sin(angle - Math.PI / 6)),
                    edge.y - (int) (size * Math.sin(angle + Math.PI / 6)) };
            g2d.setColor(Color.GREEN);
            g2d.fillPolygon(xs, ys, 3);

            // Draw arrow tail (unchanged)
            int tailX = edge.x - (int) (tailLength * Math.cos(angle));
            int tailY = edge.y - (int) (tailLength * Math.sin(angle));
            g2d.setStroke(new java.awt.BasicStroke(3));
            g2d.drawLine(tailX, tailY, edge.x, edge.y);
        }

        // Restore previous hints
        g2.setRenderingHints(oldHints);
    }

    public void renderHover(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Save old hints
        RenderingHints oldHints = g2.getRenderingHints();

        // Enable full antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        if (mousePoint.x == Integer.MIN_VALUE || mousePoint.y == Integer.MIN_VALUE) {
            return;
        }

        recalcWorldCoords();
        String coords = String.format("%d, %d", worldPoint.x, worldPoint.y);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(mousePoint.x, mousePoint.y - 16, coords.length() * 7, 16);

        Color textColor = Color.WHITE;
        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 1, 12));

        g2.drawString(coords, mousePoint.x + 4, mousePoint.y - 4);

        // Restore previous hints
        g2.setRenderingHints(oldHints);
    }

    public void recalcWorldCoords() {
        if (dragging) {
            return;
        }
        float coordsPerPixel = 1000 / 250f;

        worldPoint.x = mousePoint.x - offsetPoint.x;
        worldPoint.y = mousePoint.y - offsetPoint.y;

        int maxWidth = 16384;

        float x = worldPoint.x * coordsPerPixel;
        worldPoint.x = (int) x;

        float y = worldPoint.y * coordsPerPixel;
        worldPoint.y = (int) y;

        if (worldPoint.x >= maxWidth) {
            worldPoint.x = worldPoint.x % maxWidth;
        }

        if (worldPoint.x <= 0) {
            worldPoint.x = worldPoint.x % maxWidth;
            worldPoint.x += maxWidth;
        }
    }

    public void renderHint(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Save old hints
        RenderingHints oldHints = g2.getRenderingHints();

        // Enable full antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Background box
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(15, this.getHeight() - 65, 173, 50);

        int textInitY = this.getHeight() - 44;
        int row = 0;
        int inc = 20;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Verdana", Font.PLAIN, 20));

        // Draw text
        g2.drawString("Left-Click to drag", 22, textInitY + inc * row++);
        g2.drawString("R to clear plot", 22, textInitY + inc * row++);

        // Restore previous hints
        g2.setRenderingHints(oldHints);
    }

    public void renderText(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Save old hints
        RenderingHints oldHints = g2.getRenderingHints();

        // Enable full antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Color textColor = Color.WHITE;

        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 0, 20));

        int textInitY = 30;
        int row = 0;
        int inc = 30;

        TrackPoint lastPos = trackPoints.isEmpty() ? null : trackPoints.get(trackPoints.size() - 1);
        Point p = lastPos == null ? null : lastPos.world;
        int x = 0;
        int y = 0;
        if (p != null) {
            x = p.x;
            y = p.y;
        }
        String worldText = "Coords: " + x + ", " + y;
        g2.drawString(worldText, 15, textInitY + inc * row++);

        double heading = CoordMathUtils.averageHeadingLastN(10, trackPoints);
        String rotText = String.format("Rotation: %.0f deg", heading);
        g2.drawString(rotText, 15, textInitY + inc * row++);

        // Speed text
        double speed = CoordMathUtils.averageSpeedLastN(5, trackPoints);
        String speedNewText = String.format("Speed: %3.2f kt", speed);
        g2.drawString(speedNewText, 15, textInitY + inc * row++);

        double units = 0;
        for (TrackPoint tp : trackPoints) {
            units += tp.distanceFromPrev;
        }

        double gvonavishNmiCircumference = (2 * Math.PI * 6378.137) / 1.852;
        double nmiFactor = gvonavishNmiCircumference / 16384.0;

        double nmi = nmiFactor * units;
        String distanceText = String.format("Distance: %3.2f nmi", nmi);
        g2.drawString(distanceText, 15, textInitY + inc * row++);

        // Restore previous hints
        g2.setRenderingHints(oldHints);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastPoint.x = e.getX();
        lastPoint.y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON3) {
            rclick = true;
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        mousePoint.x = e.getX();
        mousePoint.y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON3) {
            rclick = false;
        }

        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mousePoint.x = e.getX();
        mousePoint.y = e.getY();
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mousePoint.x = Integer.MIN_VALUE;
        mousePoint.y = Integer.MIN_VALUE;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (rclick) {
            mousePoint.x = e.getX();
            mousePoint.y = e.getY();
            repaint();
            return;
        }

        dragging = true;
        mousePoint.x = e.getX();
        mousePoint.y = e.getY();

        int prevOffsetX = offsetPoint.x;
        int prevOffsetY = offsetPoint.y;

        int dx = e.getX() - lastPoint.x;
        int dy = e.getY() - lastPoint.y;
        offsetPoint.x += dx;
        offsetPoint.y += dy;
        lastPoint.x = e.getX();
        lastPoint.y = e.getY();

        int top = 0;
        int bottom = -1 * (imageDimms.y - this.getHeight());

        if (offsetPoint.y >= top) {
            offsetPoint.y = top;
            dragging = false;
            recalcWorldCoords();
            dragging = true;
        }

        if (offsetPoint.y <= bottom) {
            offsetPoint.y = bottom;
            dragging = false;
            recalcWorldCoords();
            dragging = true;
        }

        int dox = offsetPoint.x - prevOffsetX;
        int doy = offsetPoint.y - prevOffsetY;

        for (TrackPoint p : trackPoints) {
            p.offset.x += dox;
            p.offset.y += doy;
        }

        repaint();

        // System.out.printf("x=%d, y=%d \r\n", offsetX, offsetY);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePoint.x = e.getX();
        mousePoint.y = e.getY();

        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // todo
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Javaish");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new Javaish());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            // frame.setResizable(false);
        });
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_R) {
            // Clear tracking points (coordinates, distance, speed, rotation)
            trackPoints.clear();

            // Reset timing
            lastTime = -1;

            repaint();
        }
    }

    private Point purelyConvertWtoM(TrackPoint tp)
    {
        Point wCoord = tp.world;
        Point mCoord = new Point(0, 0);
        // Convert world coordinates to map pixels
        double xW = wCoord.x;
        double yW = wCoord.y;

        yW /= 1000.0;
        yW *= 250.0;

        xW /= 1000.0;
        xW *= 250.0;

        mCoord.y = (int) yW + tp.offset.y;
        mCoord.x = (int) xW + tp.offset.x;
        
        return mCoord;
    }
    
    private Point convertWtoM(TrackPoint tp) {
        Point wCoord = tp.world;
        Point mCoord = new Point(0, 0);

        // Convert world coords to map pixels
        double xW = wCoord.x / 1000.0 * 250.0;
        double yW = wCoord.y / 1000.0 * 250.0;

        mCoord.y = (int) yW + tp.offset.y;
        mCoord.x = (int) xW + tp.offset.x;

        if (firstRender) {
            // center first point
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            int desiredOffsetX = centerX - (int) xW;
            int desiredOffsetY = centerY - (int) yW;

            int topLimit = 0;
            int bottomLimit = Math.min(0, getHeight() - imageDimms.y);

            if (desiredOffsetY > topLimit) desiredOffsetY = topLimit;
            if (desiredOffsetY < bottomLimit) desiredOffsetY = bottomLimit;

            tp.offset.x = desiredOffsetX;
            tp.offset.y = desiredOffsetY;

            offsetPoint.x = desiredOffsetX;
            offsetPoint.y = desiredOffsetY;

            firstRender = false;

            // Update mCoord
            mCoord.x = (int) xW + offsetPoint.x;
            mCoord.y = (int) yW + offsetPoint.y;

            return mCoord;
        }

        if (!trackPoints.isEmpty()) {
            TrackPoint lastTP = trackPoints.get(trackPoints.size() - 1);
            Point lastPoint = purelyConvertWtoM(lastTP);

            int normXNew = ((mCoord.x % imageDimms.x) + imageDimms.x) % imageDimms.x;
            int normXLast = ((lastPoint.x % imageDimms.x) + imageDimms.x) % imageDimms.x;

            int diffX = normXNew - normXLast;
            int half = imageDimms.x / 2;

            // Detect boundary crossing and adjust offset
            if (diffX > half) {
                diffX -= imageDimms.x;
                tp.offset.x += imageDimms.x;  // add full map width to offset
            } else if (diffX < -half) {
                diffX += imageDimms.x;
                tp.offset.x -= imageDimms.x;  // subtract full map width
            }

            int diffY = mCoord.y - lastPoint.y;

            // Apply corrected movement
            mCoord.x = lastPoint.x + diffX;
            mCoord.y = lastPoint.y + diffY;
        }

        return mCoord;
    }


    private boolean isOffscreen(Point p) {
        return p.x < 0 || p.x > getWidth() || p.y < 0 || p.y > getHeight();
    }

    private Point getEdgePoint(Point p) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        double dx = p.x - cx;
        double dy = p.y - cy;

        double scale = 1.0;

        // horizontal scaling
        if (dx != 0) {
            double sx = (dx > 0) ? (getWidth() - 10 - cx) / dx : (10 - cx) / dx;
            scale = Math.min(scale, sx);
        }

        // vertical scaling
        if (dy != 0) {
            double sy = (dy > 0) ? (getHeight() - 10 - cy) / dy : (10 - cy) / dy;
            scale = Math.min(scale, sy);
        }

        int edgeX = cx + (int) (dx * scale);
        int edgeY = cy + (int) (dy * scale);

        return new Point(edgeX, edgeY);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
        // TODO Auto-generated method stub

    }
}
