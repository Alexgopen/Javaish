package com.github.alexgopen.gvojavaish;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GvoJavaish extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private BufferedImage image;
    private int imageWidth, imageHeight;
    private int offsetX = 0;
    private int offsetY = 0;
    private int lastX, lastY;

    public GvoJavaish() {
        try {
            image = ImageIO.read(GvoJavaish.class.getResource("map.png"));
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        setPreferredSize(new Dimension(1200, 800));
        addMouseListener(this);
        addMouseMotionListener(this);
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
            g.drawImage(image, (x - imageWidth), y, null);
            g.drawImage(image, x, y, null);
            g.drawImage(image, (x + imageWidth), y, null);
            x += imageWidth;
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastX = e.getX();
        lastY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
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
            // lastY = top;
        }

        if (offsetY <= bottom) {
            offsetY = bottom;
            // lastY = bottom;
        }

        repaint();

        // System.out.printf("x=%d, y=%d \r\n", offsetX, offsetY);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
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
}
