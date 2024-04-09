package tess;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.util.LoadLibs;

public class TessDemo extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = -1668129614007560894L;
    private BufferedImage imageMap;
    private BufferedImage imageSurvey;
    private BufferedImage imageSurveyCrop;
    private int imageWidth, imageHeight;
    private int offsetX = 0;
    private int offsetY = 0;
    private int lastX, lastY;
    private Tesseract tess;

    private int mouseX = -1;
    private int mouseY = -1;

    private int worldX = -1;
    private int worldY = -1;

    boolean dragging;

    public static String parseImage(BufferedImage img) {
        String ret = "error";

        return ret;
    }

    public TessDemo() {
        try {
            String map = "map.png";
            map = "uwogrid.png";
            imageMap = ImageIO.read(TessDemo.class.getResource(map));

            imageWidth = imageMap.getWidth();
            imageHeight = imageMap.getHeight();

            boolean tessEnabled = false;

            if (tessEnabled) {
                try {
                    imageSurvey = ImageIO.read(TessDemo.class.getResource("examplesurvey.png"));
                    imageSurveyCrop = ImageIO.read(TessDemo.class.getResource("examplesurveycrop.png"));

                    File tmpFolder = LoadLibs.extractTessResources("win32-x86-64");
                    for (File f : tmpFolder.listFiles()) {
                        System.load(f.getAbsolutePath());
                    }

                    tess = new Tesseract();
                    tess.setLanguage("eng");
                    tess.setOcrEngineMode(1);
                    tess.setPageSegMode(7);
                    Path dataDirectory = Paths.get(TessDemo.class.getResource("data").toURI());
                    tess.setDatapath(dataDirectory.toString());

                    tess.setVariable("tessedit_char_whitelist", "123456789,. ");
                    System.out.println("OCRed:");

                    System.out.println(tess.doOCR(ImageHelper.convertImageToGrayscale(imageSurveyCrop)));
                    System.out.println("Expected:");
                    System.out.println("15842,3284");
                }
                catch (TesseractException | URISyntaxException te) {
                    te.printStackTrace();
                }
            }

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
            g.drawImage(imageMap, (x - imageWidth), y, null);
            g.drawImage(imageMap, x, y, null);
            g.drawImage(imageMap, (x + imageWidth), y, null);
            x += imageWidth;
        }

        // renderText(g);
        renderHover(g);
    }

    public void renderHover(final Graphics g2) {

        if (mouseX == -1 || mouseY == -1) {
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
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        System.out.println("Entered");
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        System.out.println("Exited");
        mouseX = -1;
        mouseY = -1;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dragging = true;
        mouseX = e.getX();
        mouseY = e.getY();
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
        System.out.println("Moved");
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
            frame.getContentPane().add(new TessDemo());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            // frame.setResizable(false);
        });
    }
}
