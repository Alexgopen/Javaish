package com.github.alexgopen.javaish.application;

import com.github.alexgopen.javaish.utils.*;
import com.github.alexgopen.javaish.model.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class JavaishV3 extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
    private final CoordConverter converter;
    private final TrackManager trackManager;
    private final MapRenderer renderer;
    private Point lastMouse = new Point(0,0);
    private boolean dragging = false;

    public JavaishV3(BufferedImage imageMap) {
        this.setPreferredSize(new Dimension(800,600));
        this.setFocusable(true);
        this.requestFocus();
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);

        trackManager = new TrackManager();
        converter = new CoordConverter(getPreferredSize(), new Point(imageMap.getWidth(), imageMap.getHeight()));
        renderer = new MapRenderer(imageMap, converter, trackManager);

        CoordProvider provider = new CoordProvider();
        Thread updater = new Thread(new CoordUpdater(trackManager, provider, converter));
        updater.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g, getSize(), converter.getOffset(), this.getMousePoint(), dragging);
    }
    
    private Point getMousePoint()
    {
        return new Point(lastMouse.x, lastMouse.y);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dragging = true;
        int dx = e.getX() - lastMouse.x;
        int dy = e.getY() - lastMouse.y;
        converter.shiftOffset(dx, dy);
        lastMouse.x = e.getX();
        lastMouse.y = e.getY();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouse.x = e.getX();
        lastMouse.y = e.getY();
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        lastMouse.x = e.getX();
        lastMouse.y = e.getY();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_R) {
            trackManager.clear();
            repaint();
        }
    }

    // Empty implementations
    @Override public void mouseMoved(MouseEvent e) { lastMouse = new Point(e.getX(), e.getY()); repaint(); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BufferedImage img = null;
            try {
                img = MapLoader.loadMap();
            }
            catch (IOException e) {
                System.err.println("Failed to load map image.");
                e.printStackTrace();
                System.exit(1);
            }
            JFrame frame = new JFrame("Javaish");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new JavaishV3(img));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
