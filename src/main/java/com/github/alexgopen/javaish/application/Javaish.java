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
    
    private BufferedImage mapImage;
    private Dimension mapImageDimms = new Dimension(0,0);
    
    private CoordProvider coordProvider;

    // Mouse stuff
    private boolean dragging;
    private boolean rclick = false;
    private Point lastClickPos = new Point(0, 0);
    private Point mousePos = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private Point hoveredCoord = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    
    // View offset
    private Point viewOffsetPixels = new Point(0, 0);

    // Points to draw (pixels)
    private List<Point> points = new ArrayList<>();
    // Points for tracking (we should enhance the implementation to depend only on these)
    private List<TrackPoint> trackPoints = new ArrayList<>();
    
    // Seville is a common starting point
    private Point initialCenterCoord = new Point(15903, 3271);

    // Loop management
    private long lastTime = -1;
    private boolean firstRender = true;

    public Javaish() {
        try {
            BufferedImage loadedImg = MapImageProvider.loadMap();
            
            mapImage = loadedImg;
            mapImageDimms.width = mapImage.getWidth();
            mapImageDimms.height = mapImage.getHeight();
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
        final Javaish javaish = javaishInstance;
        
        Thread coordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(tickRate);
                        
                        Point coord = javaish.coordProvider.getCoord();
                        Point worldCoord = coord;
                        Point mapCoord = convertWtoM(coord);
                        int dist = 999;

                        if (javaish.points.size() > 0) {
                            Point lastCoord = javaish.points.get(javaish.points.size() - 1);

                            dist = (int) Math.sqrt(
                                    Math.pow(mapCoord.x - lastCoord.x, 2) + Math.pow(mapCoord.y - lastCoord.y, 2));
                        }

                        long currentTime = System.currentTimeMillis();
                        long timeDelta = currentTime - javaish.lastTime;

                        long deltaTimeTp = 0;
                        int distTp = 0;
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

                        if (timeDelta > 1000 && dist >= 2) {
                            TrackPoint tp = new TrackPoint(worldCoord, mapCoord, currentTime, distTp, deltaTimeTp);
                            trackPoints.add(tp);
                            if (dist != 999) {
                                System.err.println("Dist=" + dist + ", delta=" + timeDelta + ", pos="
                                        + tp.world.toString() + ", newSpeed=" + newSpeed + ", avgSpeed=" + avgSpeed);
                            }
                            javaish.lastTime = currentTime;
                            javaish.points.add(mapCoord);
                            javaish.repaint();
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
        int bottomLimit = Math.min(0, getHeight() - mapImageDimms.height);

        if (desiredOffsetY > topLimit)
            desiredOffsetY = topLimit;
        if (desiredOffsetY < bottomLimit)
            desiredOffsetY = bottomLimit;

        viewOffsetPixels.x = desiredOffsetX;
        viewOffsetPixels.y = desiredOffsetY;

        System.out.println("Initial view centered at " + initialCenterCoord + " with offset " + viewOffsetPixels);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int x = viewOffsetPixels.x % mapImageDimms.width;
        if (x > 0) {
            x -= mapImageDimms.width;
        }

        int y = viewOffsetPixels.y;

        while (x < getWidth()) {
            g.drawImage(mapImage, (x - mapImageDimms.width), y, null);
            g.drawImage(mapImage, x, y, null);
            g.drawImage(mapImage, (x + mapImageDimms.width), y, null);
            x += mapImageDimms.width;
        }

        // renderPoints(g);
        renderPointsList(g);
        renderHint(g);
        renderText(g);
        renderHover(g);
    }

    public void renderPointsList(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Save old hints
        RenderingHints oldHints = g2.getRenderingHints();

        int prevPointX = Integer.MIN_VALUE;
        int prevPointY = Integer.MIN_VALUE;

        int curPointX = Integer.MIN_VALUE;
        int curPointY = Integer.MIN_VALUE;

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);

            if (i == points.size() - 2) {
                prevPointX = p.x;
                prevPointY = p.y;
            }

            if (i == points.size() - 1) {
                curPointX = p.x;
                curPointY = p.y;
            }

            if (i > 0) {
                g2.setColor(new Color(255, 0, 0, 255));
                g2.drawLine(points.get(i - 1).x, points.get(i - 1).y, points.get(i).x, points.get(i).y);
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

            int xDiff = curPointX - prevPointX;
            int yDiff = curPointY - prevPointY;

            if (points.size() > 10) {
                xDiff = curPointX - points.get(points.size() - 10).x;
                yDiff = curPointY - points.get(points.size() - 10).y;
            }

            int maxFactorX = 0;
            int maxFactorY = 0;

            double xMaxDiff = 0;
            double yMaxDiff = 0;

            // start x
            if (curPointX <= 0) {
                if (xDiff <= 0) {
                    xMaxDiff = Integer.MIN_VALUE - curPointX;
                }
                if (xDiff > 0) {
                    xMaxDiff = curPointX + Integer.MAX_VALUE;
                }
            }
            if (curPointX > 0) {
                if (xDiff < 0) {
                    xMaxDiff = curPointX + Integer.MIN_VALUE;
                }
                if (xDiff > 0) {
                    xMaxDiff = Integer.MAX_VALUE - curPointX;
                }
            }
            // end x

            // start y
            if (curPointY <= 0) {
                if (yDiff <= 0) {
                    yMaxDiff = Integer.MIN_VALUE - curPointY;
                }
                if (yDiff > 0) {
                    yMaxDiff = curPointY + Integer.MAX_VALUE;
                }
            }
            if (curPointY > 0) {
                if (yDiff < 0) {
                    yMaxDiff = curPointY + Integer.MIN_VALUE;
                }
                if (yDiff > 0) {
                    yMaxDiff = Integer.MAX_VALUE - curPointY;
                }
            }
            // end y

            maxFactorX = (int) Math.floor(Math.abs(xMaxDiff / xDiff));
            maxFactorY = (int) Math.floor(Math.abs(yMaxDiff / yDiff));

            if (xDiff == 0 || yDiff == 0) {
                int max = Math.max(maxFactorX, maxFactorY);
                maxFactorX = max;
                maxFactorY = max;
            }

            // WITH THIS:
            double smoothedHeadingDeg = CoordMathUtils.averageHeadingLastN(5, trackPoints) - 90;
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

        if (points.size() > 0) {
            player = points.get(points.size() - 1);
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

        if (mousePos.x == Integer.MIN_VALUE || mousePos.y == Integer.MIN_VALUE) {
            return;
        }

        recalcWorldCoords();
        String coords = String.format("%d, %d", hoveredCoord.x, hoveredCoord.y);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(mousePos.x, mousePos.y - 16, coords.length() * 7, 16);

        Color textColor = Color.WHITE;
        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 1, 12));

        g2.drawString(coords, mousePos.x + 4, mousePos.y - 4);

        // Restore previous hints
        g2.setRenderingHints(oldHints);
    }

    public void recalcWorldCoords() {
        if (dragging) {
            return;
        }
        float coordsPerPixel = 1000 / 250f;

        Point worldPixel = new Point(mousePos.x - viewOffsetPixels.x, mousePos.y - viewOffsetPixels.y);

        int maxWidth = 16384;

        float x = worldPixel.x * coordsPerPixel;
        int rawX = (int) x;

        float y = worldPixel.y * coordsPerPixel;
        int rawY = (int) y;

        if (rawX >= maxWidth) {
            rawX = rawX % maxWidth;
        }

        if (rawX <= 0) {
            rawX = rawX % maxWidth;
            rawX += maxWidth;
        }
        
        hoveredCoord.x = rawX;
        hoveredCoord.y = rawY;
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

        double heading = CoordMathUtils.averageHeadingLastN(5, trackPoints);
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
        lastClickPos.x = e.getX();
        lastClickPos.y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON3) {
            rclick = true;
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        mousePos.x = e.getX();
        mousePos.y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON3) {
            rclick = false;
        }

        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mousePos.x = e.getX();
        mousePos.y = e.getY();
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mousePos.x = Integer.MIN_VALUE;
        mousePos.y = Integer.MIN_VALUE;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (rclick) {
            mousePos.x = e.getX();
            mousePos.y = e.getY();
            repaint();
            return;
        }

        dragging = true;
        mousePos.x = e.getX();
        mousePos.y = e.getY();

        int prevOffsetX = viewOffsetPixels.x;
        int prevOffsetY = viewOffsetPixels.y;

        int dx = e.getX() - lastClickPos.x;
        int dy = e.getY() - lastClickPos.y;
        viewOffsetPixels.x += dx;
        viewOffsetPixels.y += dy;
        lastClickPos.x = e.getX();
        lastClickPos.y = e.getY();

        int top = 0;
        int bottom = -1 * (mapImageDimms.height - this.getHeight());

        if (viewOffsetPixels.y >= top) {
            viewOffsetPixels.y = top;
            dragging = false;
            recalcWorldCoords();
            dragging = true;
        }

        if (viewOffsetPixels.y <= bottom) {
            viewOffsetPixels.y = bottom;
            dragging = false;
            recalcWorldCoords();
            dragging = true;
        }

        int dox = viewOffsetPixels.x - prevOffsetX;
        int doy = viewOffsetPixels.y - prevOffsetY;

        for (Point p : points) {
            p.x += dox;
            p.y += doy;
        }

        repaint();

        // System.out.printf("x=%d, y=%d \r\n", offsetX, offsetY);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePos.x = e.getX();
        mousePos.y = e.getY();

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
            // Clear plotted map points
            points.clear();

            // Clear tracking points (coordinates, distance, speed, rotation)
            trackPoints.clear();

            // Reset timing
            lastTime = -1;

            repaint();
        }
    }

    private Point convertWtoM(Point wCoord) {
        Point mCoord = new Point(0, 0);

        // Convert world coordinates to map pixels
        double xW = wCoord.x;
        double yW = wCoord.y;

        yW /= 1000.0;
        yW *= 250.0;

        xW /= 1000.0;
        xW *= 250.0;

        mCoord.y = (int) yW + viewOffsetPixels.y;
        mCoord.x = (int) xW + viewOffsetPixels.x;

        if (firstRender) {
            // Compute offset to center the first point
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            int desiredOffsetX = centerX - (int) xW;
            int desiredOffsetY = centerY - (int) yW;

            // Clamp vertical offset to map limits
            int topLimit = 0;
            int bottomLimit = Math.min(0, getHeight() - mapImageDimms.height);

            if (desiredOffsetY > topLimit)
                desiredOffsetY = topLimit;
            if (desiredOffsetY < bottomLimit)
                desiredOffsetY = bottomLimit;

            viewOffsetPixels.x = desiredOffsetX;
            viewOffsetPixels.y = desiredOffsetY;

            firstRender = false;

            System.out.println("Auto-centered view with offset: " + viewOffsetPixels.toString());

            // Update mCoord after shifting offset
            mCoord.x = (int) xW + viewOffsetPixels.x;
            mCoord.y = (int) yW + viewOffsetPixels.y;
        }
        else if (!points.isEmpty()) {
            // Normal adjustment relative to last point
            Point lastPoint = points.get(points.size() - 1);

            int normXNew = mCoord.x % mapImageDimms.width;
            int normXLast = lastPoint.x % mapImageDimms.width;

            int yNew = mCoord.y;
            int yLast = lastPoint.y;

            // --- FIX: compute wrapped X difference ---
            int diffx = normXNew - normXLast;
            // wrap horizontally (half-map threshold)
            int half = mapImageDimms.width / 2;
            if (diffx > half)
                diffx -= mapImageDimms.width;
            if (diffx < -half)
                diffx += mapImageDimms.width;
            // --- END FIX ---

            int diffy = yNew - yLast;

            // Apply corrected continuous movement
            mCoord.x = lastPoint.x + diffx;
            mCoord.y = lastPoint.y + diffy;
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
