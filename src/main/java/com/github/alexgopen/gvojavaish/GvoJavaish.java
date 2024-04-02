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

public class GvoJavaish extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private static final long serialVersionUID = -1668129614007560894L;
    private BufferedImage imageMap;
    private int imageWidth, imageHeight;
    private int offsetX = 0;
    private int offsetY = 0;
    private int lastX, lastY;

    private int mouseX = Integer.MIN_VALUE;
    private int mouseY = Integer.MIN_VALUE;

    private int worldX = Integer.MIN_VALUE;
    private int worldY = Integer.MIN_VALUE;

    private int prevPointX = Integer.MIN_VALUE;
    private int prevPointY = Integer.MIN_VALUE;
    private int curPointX = Integer.MIN_VALUE;
    private int curPointY = Integer.MIN_VALUE;

    private boolean rclick = false;

    private List<Point> points = new ArrayList<>();

    boolean dragging;

    public GvoJavaish() {
        try {
            String map = "map.png";
            map = "uwogrid.png";
            imageMap = ImageIO.read(GvoJavaish.class.getResource(map));

            imageWidth = imageMap.getWidth();
            imageHeight = imageMap.getHeight();
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
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int x = offsetX % imageWidth;
        if (x > 0) {
            x -= imageWidth;
        }

        int y = offsetY;

        while (x < getWidth()) {
            g.drawImage(imageMap, (x - imageWidth), y, null);
            g.drawImage(imageMap, x, y, null);
            g.drawImage(imageMap, (x + imageWidth), y, null);
            x += imageWidth;
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
            g2.fillOval(p.x - 5, p.y - 5, 10, 10);
        }

        if (prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE) {
            int transX = prevPointX;
            int transY = prevPointY;

            g2.setColor(new Color(255, 0, 0, 255));
            g2.fillOval(transX - 5, transY - 5, 10, 10);
        }
        if (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE) {
            int transX = curPointX;
            int transY = curPointY;

            g2.setColor(new Color(0, 255, 0, 255));
            g2.fillOval(transX - 5, transY - 5, 10, 10);
        }

        if ((prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE)
                && (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE)) {
            g2.setColor(new Color(255, 0, 0, 255));
            g2.drawLine(prevPointX, prevPointY, curPointX, curPointY);

            int xDiff = curPointX - prevPointX;
            int yDiff = curPointY - prevPointY;
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
            g2.fillOval(transX - 5, transY - 5, 10, 10);
        }

    }

    public void renderPoints(final Graphics g2) {
        if (prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE) {
            int transX = prevPointX;
            int transY = prevPointY;

            g2.setColor(new Color(255, 0, 0, 255));
            g2.fillOval(transX - 5, transY - 5, 10, 10);
        }
        if (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE) {
            int transX = curPointX;
            int transY = curPointY;

            g2.setColor(new Color(0, 255, 0, 255));
            g2.fillOval(transX - 5, transY - 5, 10, 10);
        }

        if ((prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE)
                && (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE)) {
            g2.setColor(new Color(255, 0, 0, 255));
            g2.drawLine(prevPointX, prevPointY, curPointX, curPointY);

            int xDiff = curPointX - prevPointX;
            int yDiff = curPointY - prevPointY;
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
            g2.fillOval(transX - 5, transY - 5, 10, 10);
        }

    }

    public void renderHover(final Graphics g2) {

        if (mouseX == Integer.MIN_VALUE || mouseY == Integer.MIN_VALUE) {
            return;
        }

        recalcWorldCoords();
        String coords = String.format("%d, %d", worldX, worldY);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(mouseX, mouseY - 16, coords.length() * 8 + 8, 16);

        Color textColor = Color.WHITE;
        g2.setColor(textColor);
        g2.setFont(new Font("Verdana", 1, 12));

        g2.drawString(coords, mouseX + 4, mouseY - 4);
    }

    public void recalcWorldCoords() {
        if (dragging) {
            return;
        }
        float coordsPerPixel = 1000 / 125f;

        worldX = mouseX - offsetX;
        worldY = mouseY - offsetY;

        int maxWidth = 16384;

        float x = worldX / 250f;
        x *= 1000;
        worldX = (int) x;

        float y = worldY / 250f;
        y *= 1000;
        worldY = (int) y;

        if (worldX >= maxWidth) {
            worldX = worldX % maxWidth;
        }

        if (worldX <= 0) {
            worldX = worldX % maxWidth;
            worldX += maxWidth;
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
        String worldText = "Coords: " + worldX + ", " + worldY;
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
        lastX = e.getX();
        lastY = e.getY();

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

            prevPointX = curPointX;
            prevPointY = curPointY;

            curPointX = wX;
            curPointY = wY;

            points.add(new Point(wX, wY));

            repaint();
        }

        System.out.println(e.getButton());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        mouseX = e.getX();
        mouseY = e.getY();

        if (e.getButton() == MouseEvent.BUTTON3) {
            rclick = false;
        }

        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseX = Integer.MIN_VALUE;
        mouseY = Integer.MIN_VALUE;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (rclick) {
            mouseX = e.getX();
            mouseY = e.getY();
            repaint();
            return;
        }

        dragging = true;
        mouseX = e.getX();
        mouseY = e.getY();

        int prevOffsetX = offsetX;
        int prevOffsetY = offsetY;

        int dx = e.getX() - lastX;
        int dy = e.getY() - lastY;
        offsetX += dx;
        offsetY += dy;
        lastX = e.getX();
        lastY = e.getY();

        int bottomLimit = -1 * (imageHeight - this.getHeight());
        if (offsetY <= bottomLimit) {
            // offsetY = bottomLimit;
            // lastY = bottomLimit;

        }

        // 2048 image
        // 800 preferred

        // y=0
        // y=-1248

        int top = 0;
        int bottom = -1 * (imageHeight - this.getHeight());

        if (offsetY >= top) {
            offsetY = top;
            dragging = false;
            recalcWorldCoords();
            dragging = true;
        }

        if (offsetY <= bottom) {
            offsetY = bottom;
            dragging = false;
            recalcWorldCoords();
            dragging = true;
        }

        int dox = offsetX - prevOffsetX;
        int doy = offsetY - prevOffsetY;

        if (prevPointX != Integer.MIN_VALUE && prevPointY != Integer.MIN_VALUE) {
            prevPointX += dox;
            prevPointY += doy;
        }
        if (curPointX != Integer.MIN_VALUE && curPointY != Integer.MIN_VALUE) {
            curPointX += dox;
            curPointY += doy;
        }

        for (Point p : points) {
            p.x += dox;
            p.y += doy;
        }

        repaint();

        // System.out.printf("x=%d, y=%d \r\n", offsetX, offsetY);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

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
        System.out.println("Key");
        if (e.getKeyCode() == KeyEvent.VK_R) {
            prevPointX = Integer.MIN_VALUE;
            prevPointY = Integer.MIN_VALUE;
            curPointX = Integer.MIN_VALUE;
            curPointY = Integer.MIN_VALUE;
            points.clear();
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
        // TODO Auto-generated method stub

    }
}
