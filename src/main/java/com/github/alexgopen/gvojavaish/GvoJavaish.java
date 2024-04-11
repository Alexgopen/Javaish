package com.github.alexgopen.gvojavaish;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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

// Ideas:
// Points is mouse coords, we need it in world coords
// Store time for each point
// Get distance between points
// Get speed between that segment
// Get all distance/time for avg speed
// Mark coords of each point
// List current speed
// Read coords for auto plotting
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

    private Point neighbors = new Point(-1, 0);

    private boolean rclick = false;

    private List<Point> points = new ArrayList<>();

    boolean dragging;

    private static GvoJavaish gvojavaish;

    private static long lastTime = -1;

    public GvoJavaish() {
        try {
            String map = "map.png";
            map = "uwogrid.png";
            imageMap = ImageIO.read(GvoJavaish.class.getResource(map));

            imageDimms.x = imageMap.getWidth();
            imageDimms.y = imageMap.getHeight();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        setFocusable(true);
        requestFocus();
        setPreferredSize(new Dimension(1200, 800));
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        GvoJavaish.coordProvider = new CoordProvider();
        GvoJavaish.gvojavaish = this;

        Thread coordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(250);
                        Point coord = GvoJavaish.coordProvider.getCoord();
                        Point converted = convertWtoM(coord);
                        int dist = 999;

                        if (GvoJavaish.gvojavaish.points.size() > 0) {
                            Point lastCoord = GvoJavaish.gvojavaish.points.get(GvoJavaish.gvojavaish.points.size() - 1);

                            dist = (int) Math.sqrt(
                                    Math.pow(converted.x - lastCoord.x, 2) + Math.pow(converted.y - lastCoord.y, 2));
                        }

                        long currentTime = System.currentTimeMillis();
                        long timeDelta = currentTime - GvoJavaish.lastTime;
                        System.err.println("Dist=" + dist + ", delta=" + timeDelta);
                        if (timeDelta > 1000 && dist >= 2) {

                            GvoJavaish.lastTime = currentTime;
                            GvoJavaish.gvojavaish.points.add(converted);
                            GvoJavaish.gvojavaish.repaint();
                        }

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        coordThread.start();
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

        // renderText(g);
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

            g2.setColor(new Color(0, 255, 255, 255));
            int endX = curPointX + xDiff * lowestFactor;
            int endY = curPointY + yDiff * lowestFactor;
            g2.drawLine(curPointX, curPointY, endX, endY);

            int transX = curPointX;
            int transY = curPointY;

            g2.setColor(new Color(0, 255, 0, 255));
            g2.fillOval(transX - 3, transY - 3, 6, 6);
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
        String plotText = String.format("Right-Click to plot");
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

        // Zone text
        String zoneText = String.format("Zone: %s", "unknown");
        g2.drawString(zoneText, 15, textInitY + inc * row++);

        recalcWorldCoords();
        String worldText = "Coords: " + worldPoint.x + ", " + worldPoint.y;
        g2.drawString(worldText, 15, textInitY + inc * row++);

        // Speed text
        String speedText = String.format("Speed: %3.2f kt", 0.0f);
        g2.drawString(speedText, 15, textInitY + inc * row++);

        // Rot text
        String rotText = String.format("Rotation: %d deg", 0);
        g2.drawString(rotText, 15, textInitY + inc * row++);
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

            int mx = e.getX();
            int my = e.getY();

            float coordsPerPixel = 1000 / 125f;

            int wX = -1;
            int wY = -1;

            wX = mx;// - offsetX;
            wY = my;// - offsetY;

            boolean world = false;
            if (world) {
                int maxWidth = 16384;

                float x = wX / 250f;
                x *= 1000;
                wX = (int) x;

                float y = wY / 250f;
                y *= 1000;
                wY = (int) y;

                if (wX >= maxWidth) {
                    wX = wX % maxWidth;
                }

                if (wX <= 0) {
                    wX = wX % maxWidth;
                    wX += maxWidth;
                }
            }

            points.add(new Point(wX, wY));

            repaint();
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
        if (e.getKeyCode() == KeyEvent.VK_O) {
            Point coord = this.coordProvider.getCoord();
            Point converted = convertWtoM(coord);
            this.points.add(converted);
            repaint();
        }
    }

    private Point convertWtoM(Point wCoord) {
        Point mCoord = new Point(0, 0);

        /// invert

        double xW = wCoord.x;
        double yW = wCoord.y;

        yW /= 1000.0;
        yW *= 250.0;

        xW /= 1000.0;
        xW *= 250.0;

        mCoord.y = (int) yW + offsetPoint.y;
        mCoord.x = (int) xW + offsetPoint.x;

        if (points != null && !points.isEmpty()) {
            Point lastPoint = points.get(points.size() - 1);

            int normXNew = mCoord.x % imageDimms.x;
            int normXLast = lastPoint.x % imageDimms.x;

            int yNew = mCoord.y;
            int yLast = lastPoint.y;

            int diffx = normXNew - normXLast;
            int diffy = yNew - yLast;

            mCoord.x = lastPoint.x + diffx;
            mCoord.y = lastPoint.y + diffy;

            // this.points changes values based on offset when dragged
            System.out.printf("offsetPoint.x=%d, normXNew=%d, normXLast=%d, lastPoint.x=%d,mCoord.x=%d\r\n",
                    offsetPoint.x, normXNew, normXLast, lastPoint.x, mCoord.x);
        }
        else {
            int signum = offsetPoint.x / Math.abs(offsetPoint.x);
            signum *= -1;
            int fakeOffset = signum * (Math.abs(offsetPoint.x) + (imageDimms.x / 2));
            int mapNeighbor = Math.abs(fakeOffset) / imageDimms.x;

            int offsetFactor = signum * mapNeighbor;
            int offset = offsetFactor * imageDimms.x - imageDimms.x;

            int oldCoordX = mCoord.x;
            int newCoordX = mCoord.x + offset;
            int m1 = mCoord.x + offset - imageDimms.x;
            int p2 = mCoord.x + offset + 2 * imageDimms.x;

            mCoord.x = newCoordX;

            if (points != null && !points.isEmpty()) {
                Point lastPoint = points.get(points.size() - 1);

                int oldDiff = Math.abs(oldCoordX - lastPoint.x);
                int newDiff = Math.abs(newCoordX - lastPoint.x);
                int m1Diff = Math.abs(m1 - lastPoint.x);
                int p2Diff = Math.abs(p2 - lastPoint.x);

                if (oldDiff < newDiff && oldDiff < m1Diff && oldDiff < p2Diff) {
                    mCoord.x = oldCoordX;
                }

                if (newDiff < oldDiff && newDiff < m1Diff && newDiff < p2Diff) {
                    mCoord.x = newCoordX;
                }

                if (m1Diff < oldDiff && m1Diff < newDiff && m1Diff < p2Diff) {
                    mCoord.x = m1;
                }

                if (p2Diff < oldDiff && p2Diff < newDiff && p2Diff < m1Diff) {
                    mCoord.x = p2;
                }
            }
        }

        return mCoord;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
        // TODO Auto-generated method stub

    }
}
