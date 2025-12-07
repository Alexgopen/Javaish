package com.github.alexgopen.gvojavaish;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import exceptions.CoordNotFoundException;
import utils.CoordProvider;
import utils.Point;
import utils.TrackPoint;
import utils.WindowCapture;

// Ideas:
// Mark coords of each point
// List total distance of current trip
// List duration of current trip
// Mark shipwreck hits (triangulation)
// Implement zoom
// How is circumnavigation handled?  Do we render the points on every map

// Do i put coords in another layer and overlay+tile it left and right?

public class GvoJavaish extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private static final long serialVersionUID = -1668129614007560894L;
    private BufferedImage imageMap;
    private static CoordProvider coordProvider;

    private Point imageDimms = new Point(0, 0);
    private Point lastPoint = new Point(0, 0);
    private Point offsetPoint = new Point(0, 0);
    private Point mousePoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private Point worldPoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    
    // Seville
    private Point initialCenterCoord = new Point(15903, 3271);

    private Point neighbors = new Point(-1, 0);

    private boolean rclick = false;

    private List<Point> points = new ArrayList<>();
    
    private List<TrackPoint> trackPoints = new ArrayList<>();


    boolean dragging;

    private static GvoJavaish gvojavaish;

    private static long lastTime = -1;
    
    private static final long tickRate = 250;
    
    private static long failureDelay = 0;
    
    private boolean firstRender = true;

    public static void failedToFindCoord()
    {
    	failureDelay = 250;
    }
    
    public GvoJavaish() {
    	
        try {
            String map = "map.png";
            map = "/uwogrid.png";
            imageMap = ImageIO.read(GvoJavaish.class.getResource(map));

            imageDimms.x = imageMap.getWidth();
            imageDimms.y = imageMap.getHeight();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        setFocusable(true);
        requestFocus();
        setPreferredSize(new Dimension(800, 600));
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        
        // Auto-center before first render
        centerOnInitialCoord();

        GvoJavaish.coordProvider = new CoordProvider();
        GvoJavaish.gvojavaish = this;

        Thread coordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(tickRate + GvoJavaish.failureDelay);
                        
                        GvoJavaish.failureDelay = 0;
                        Point coord = GvoJavaish.coordProvider.getCoord();
                        Point worldCoord = coord; 
                        Point mapCoord = convertWtoM(coord);
                        int dist = 999;

                        if (GvoJavaish.gvojavaish.points.size() > 0) {
                            Point lastCoord = GvoJavaish.gvojavaish.points.get(GvoJavaish.gvojavaish.points.size() - 1);

                            dist = (int) Math.sqrt(
                                    Math.pow(mapCoord.x - lastCoord.x, 2) + Math.pow(mapCoord.y - lastCoord.y, 2));
                        }

                        long currentTime = System.currentTimeMillis();
                        long timeDelta = currentTime - GvoJavaish.lastTime;
                        
                        long deltaTimeTp = 0;
                        int distTp = 0;
                        if (!trackPoints.isEmpty()) {
                            TrackPoint last = trackPoints.get(trackPoints.size() - 1);
                            deltaTimeTp = currentTime - last.timestamp;
                            
                            int dx = wrappedDelta(worldCoord.x, last.world.x);
                            int dy = wrappedDelta(worldCoord.y, last.world.y);

                            distTp = (int)Math.sqrt(dx*dx + dy*dy);
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

                        // Skip this point if speed is more than 5x the recent average
                        if (avgSpeed > 0 && (newSpeed > 10 * avgSpeed || newSpeed >= 50)) {
                            System.err.println("Skipped spike point: newSpeed=" + newSpeed + ", avgSpeed=" + avgSpeed);
                            continue; // skip adding this point
                        }
                        
                        System.err.println("Dist=" + dist + ", delta=" + timeDelta);
                        if (timeDelta > 1000 && dist >= 2) {
                        	TrackPoint tp = new TrackPoint(worldCoord, mapCoord, currentTime, distTp, deltaTimeTp);
                            trackPoints.add(tp);
                            GvoJavaish.lastTime = currentTime;
                            GvoJavaish.gvojavaish.points.add(mapCoord);
                            GvoJavaish.gvojavaish.repaint();
                        }

                    }
                    catch (CoordNotFoundException cnfe)
                    {
                    	System.err.println("Coord not found.");
                    	WindowCapture.resetPrevFoundCoords();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        coordThread.start();
    }
    
    private int wrappedDelta(int a, int b) {
        int dx = a - b;
        if (dx > 8192)  dx -= 16384;
        if (dx < -8192) dx += 16384;
        return dx;
    }
    
    public double averageSpeedLastN(int n) {
        if (trackPoints.size() < 2) return 0;
        int start = Math.max(0, trackPoints.size() - n);
        double totalDist = 0;
        long totalTime = 0;
        for (int i = start + 1; i < trackPoints.size(); i++) {
            totalDist += trackPoints.get(i).distanceFromPrev;
            totalTime += trackPoints.get(i).deltaTime;
        }
        if (totalTime == 0) return 0;
        double unitsPerSec =  totalDist / totalTime * 1000.0; // units per second
        
        return gvonavishKt(unitsPerSec);
    }
    
    public static double nauticalMileKt(double unitsPerSec) {     
        double worldToNm = 21600.0 / 16384.0; // 21600 real nautical miles is 16384 coordinate units ingame
        double timeScale = 1.0 / 1440.0; // 1 real minute is 1 ingame day (24*60 minutes)
        double secondsPerHr = 3600;
            
        return unitsPerSec * worldToNm * secondsPerHr * timeScale;
    }

    public static double gvonavishKt(double unitsPerSec)
	{
        // real earth circumference in km / ingame earth circumference coordinate units / timescale / kmh per kt
		double knotsFactor = (2 * 3.141592654 * 6378.137) / 16384.0 / 0.4 / 1.852;
		return unitsPerSec * knotsFactor;
	} 

    public double averageHeadingLastN(int n) {
        if (trackPoints.size() < 2) return 0.0;

        int start = Math.max(0, trackPoints.size() - n);

        // Sum of unit vectors
        double sumX = 0.0;
        double sumY = 0.0;

        for (int i = start + 1; i < trackPoints.size(); i++) {
            Point prev = trackPoints.get(i - 1).world;
            Point curr = trackPoints.get(i).world;

            int dx = wrappedDelta(curr.x, prev.x);
            int dy = wrappedDelta(prev.y, curr.y);

            // Skip zero-length segments
            if (dx == 0 && dy == 0) continue;

            double length = Math.sqrt(dx*dx + dy*dy);
            sumX += dx / length;
            sumY += dy / length;
        }

        if (sumX == 0 && sumY == 0) return 0.0; // no movement

        double angleRad = Math.atan2(sumX, sumY); // dx as x, dy as y
        double angleDeg = Math.toDegrees(angleRad);
        if (angleDeg < 0) angleDeg += 360;

        return angleDeg;
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

        if (desiredOffsetY > topLimit) desiredOffsetY = topLimit;
        if (desiredOffsetY < bottomLimit) desiredOffsetY = bottomLimit;

        offsetPoint.x = desiredOffsetX;
        offsetPoint.y = desiredOffsetY;

        System.out.println("Initial view centered at " + initialCenterCoord + " with offset " + offsetPoint);
    }
    
    public void updateNeighbors() {
        int left;
        int right;

        int rhOffset = offsetPoint.x * -1 + imageDimms.x / 2;

        left = rhOffset / imageDimms.x - 1;
        right = left + 1;

        neighbors.x = left;
        neighbors.y = right;

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

        renderText(g);
        renderHover(g);
        // renderPoints(g);
        renderPointsList(g);
        renderHint(g);
    }

    public void renderPointsList(final Graphics g2) {
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

            g2.setColor(new Color(0, 255, 0, 255));
            g2.fillOval(transX - 3, transY - 3, 6, 6);
        }

        if ((prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE)
                && (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE)) {
            g2.setColor(new Color(255, 0, 0, 255));
            g2.drawLine(prevPointX, prevPointY, curPointX, curPointY);

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

            int lowestFactor = (int) (0.5 * Math.max(0, Math.min(maxFactorX, maxFactorY) - 1));

            // WITH THIS:
            double smoothedHeadingDeg = averageHeadingLastN(5) - 90;
            double smoothedHeadingRad = Math.toRadians(smoothedHeadingDeg);

            double dx = Math.cos(smoothedHeadingRad);
            double dy = Math.sin(smoothedHeadingRad);

            if (dx != 0 || dy != 0) {
                int width = getWidth();
                int height = getHeight();

                double tMax = Double.MAX_VALUE;

                // Intersect with vertical edges
                if (dx > 0) tMax = (width - 1 - curPointX) / dx;
                else if (dx < 0) tMax = -curPointX / dx;

                // Intersect with horizontal edges
                if (dy > 0) tMax = Math.min(tMax, (height - 1 - curPointY) / dy);
                else if (dy < 0) tMax = Math.min(tMax, -curPointY / dy);

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
        
        if (points.size() > 0)
        {
        	player = points.get(points.size() - 1);
        }

        if (player != null && isOffscreen(player)) {
            Point edge = getEdgePoint(player);
            int size = 16;       // triangle size
            int tailLength = 26; // length of the arrow tail
            int tipShift = 8;    // how much to move the triangle tip forward
            int glowShift = -10;   // shift the glow slightly forward

            // Cast to Graphics2D for advanced drawing
            Graphics2D g2d = (Graphics2D) g2;

            // Triangle pointing to player
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            double angle = Math.atan2(player.y - cy, player.x - cx);

            // Shift tip forward
            int tipX = edge.x + (int)(tipShift * Math.cos(angle));
            int tipY = edge.y + (int)(tipShift * Math.sin(angle));

            // Pulsating glow (shifted slightly forward)
            float pulse = 0.75f;
            int glowSize = (int)(size * 2 + pulse * 10); // glow radius varies
            int glowX = edge.x + (int)(glowShift * Math.cos(angle));
            int glowY = edge.y + (int)(glowShift * Math.sin(angle));
            Color glowColor = new Color(0, 255, 0, (int)(128 * pulse)); // translucent green
            g2d.setColor(glowColor);
            g2d.fillOval(glowX - glowSize / 2, glowY - glowSize / 2, glowSize, glowSize);

            // Draw the triangle
            int[] xs = new int[] {
                tipX, // tip shifted forward
                edge.x - (int)(size * Math.cos(angle - Math.PI / 6)),
                edge.x - (int)(size * Math.cos(angle + Math.PI / 6))
            };
            int[] ys = new int[] {
                tipY, // tip shifted forward
                edge.y - (int)(size * Math.sin(angle - Math.PI / 6)),
                edge.y - (int)(size * Math.sin(angle + Math.PI / 6))
            };
            g2d.setColor(Color.GREEN);
            g2d.fillPolygon(xs, ys, 3);

            // Draw arrow tail (unchanged)
            int tailX = edge.x - (int)(tailLength * Math.cos(angle));
            int tailY = edge.y - (int)(tailLength * Math.sin(angle));
            g2d.setStroke(new java.awt.BasicStroke(3));
            g2d.drawLine(tailX, tailY, edge.x, edge.y);
        }

    }

    public void renderHover(final Graphics g2) {

        if (mousePoint.x == Integer.MIN_VALUE || mousePoint.y == Integer.MIN_VALUE) {
            return;
        }

        recalcWorldCoords();
        String coords = String.format("%d, %d", worldPoint.x, worldPoint.y);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(mousePoint.x, mousePoint.y - 16, coords.length() * 8 + 8, 16);

        Color textColor = Color.WHITE;
        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 1, 12));

        g2.drawString(coords, mousePoint.x + 4, mousePoint.y - 4);
    }

    public void recalcWorldCoords() {
        if (dragging) {
            return;
        }
        float coordsPerPixel = 1000 / 125f;

        worldPoint.x = mousePoint.x - offsetPoint.x;
        worldPoint.y = mousePoint.y - offsetPoint.y;

        int maxWidth = 16384;

        float x = worldPoint.x / 250f;
        x *= 1000;
        worldPoint.x = (int) x;

        float y = worldPoint.y / 250f;
        y *= 1000;
        worldPoint.y = (int) y;

        if (worldPoint.x >= maxWidth) {
            worldPoint.x = worldPoint.x % maxWidth;
        }

        if (worldPoint.x <= 0) {
            worldPoint.x = worldPoint.x % maxWidth;
            worldPoint.x += maxWidth;
        }
    }

    public void renderHint(final Graphics g2) {

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(15, this.getHeight() - 93, 200, 70);

        int textInitY = this.getHeight() - 70;
        int row = 0;
        int inc = 20;

        Color textColor = Color.CYAN;

        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 0, 20));

        // Drag text
        String dragText = String.format("Left-Click to drag");
        g2.drawString(dragText, 22, textInitY + inc * row++);

        // Plot text
        String plotText = String.format("Right-Click for settings");
        g2.drawString(plotText, 22, textInitY + inc * row++);

        // Clear text
        String clearText = String.format("R to clear plot");
        g2.drawString(clearText, 22, textInitY + inc * row++);
    }

    public void renderText(final Graphics g2) {

        Color textColor = Color.MAGENTA;

        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 0, 20));

        int textInitY = 30;
        int row = 0;
        int inc = 30;

        TrackPoint lastPos = trackPoints.isEmpty() ? null : trackPoints.get(trackPoints.size() - 1);
        Point p = lastPos == null ? null : lastPos.world;
        int x = 0;
        int y = 0;
        if (p != null)
        {
        	x = p.x;
        	y = p.y;
        }
        String worldText = "Coords: " + x + ", " + y;
        g2.drawString(worldText, 15, textInitY + inc * row++);

        double heading = averageHeadingLastN(5);
        String rotText = String.format("Rotation: %.0f deg", heading);
        g2.drawString(rotText, 15, textInitY + inc * row++);
        
        // Speed text
        double speed = averageSpeedLastN(5);
        String speedNewText = String.format("Speed: %3.2f kt", speed);
        g2.drawString(speedNewText, 15, textInitY + inc * row++);
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

        int bottomLimit = -1 * (imageDimms.y - this.getHeight());
        if (offsetPoint.y <= bottomLimit) {
            // offsetY = bottomLimit;
            // lastY = bottomLimit;

        }

        // 2048 image
        // 800 preferred

        // y=0
        // y=-1248

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

        for (Point p : points) {
            p.x += dox;
            p.y += doy;
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
            JFrame frame = new JFrame("UwoMap");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new GvoJavaish());
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
            points.clear();
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

        mCoord.y = (int) yW + offsetPoint.y;
        mCoord.x = (int) xW + offsetPoint.x;

        if (firstRender) {
            // Compute offset to center the first point
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            int desiredOffsetX = centerX - (int) xW;
            int desiredOffsetY = centerY - (int) yW;

            // Clamp vertical offset to map limits
            int topLimit = 0;
            int bottomLimit = Math.min(0, getHeight() - imageDimms.y);

            if (desiredOffsetY > topLimit) desiredOffsetY = topLimit;
            if (desiredOffsetY < bottomLimit) desiredOffsetY = bottomLimit;

            offsetPoint.x = desiredOffsetX;
            offsetPoint.y = desiredOffsetY;

            firstRender = false;
            
            System.out.println("Auto-centered view with offset: "+offsetPoint.toString());

            // Update mCoord after shifting offset
            mCoord.x = (int) xW + offsetPoint.x;
            mCoord.y = (int) yW + offsetPoint.y;
        }
        else if (!points.isEmpty()) {
            // Normal adjustment relative to last point
            Point lastPoint = points.get(points.size() - 1);

            int normXNew = mCoord.x % imageDimms.x;
            int normXLast = lastPoint.x % imageDimms.x;

            int yNew = mCoord.y;
            int yLast = lastPoint.y;

            // --- FIX: compute wrapped X difference ---
            int diffx = normXNew - normXLast;
            // wrap horizontally (half-map threshold)
            int half = imageDimms.x / 2;
            if (diffx >  half) diffx -= imageDimms.x;
            if (diffx < -half) diffx += imageDimms.x;
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
