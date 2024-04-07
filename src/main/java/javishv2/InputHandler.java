package javishv2;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class InputHandler implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_R) {
            Javish.self.gameMap.clearPoints();
            Javish.self.repaint();
        }
        if (e.getKeyCode() == KeyEvent.VK_O) {
            Point coord = Javish.self.coordProvider.getCoord();
            Javish.self.gameMap.addCoord(coord);
            Javish.self.repaint();
        }
        if (e.getKeyCode() == KeyEvent.VK_P) {
            try {
                Robot robot = new Robot();
                BufferedImage screenShot = robot
                        .createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                ImageIO.write(screenShot, "PNG", new File("test.png"));
                System.out.println("Screenshot taken");
            }
            catch (IOException | AWTException ex) {

            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Javish.self.gameMap.mousePoint.x = e.getX();
        Javish.self.gameMap.mousePoint.y = e.getY();

        int dx = e.getX() - Javish.self.gameMap.lastPoint.x;
        int dy = e.getY() - Javish.self.gameMap.lastPoint.y;
        Javish.self.gameMap.offsetPoint.x += dx;
        Javish.self.gameMap.offsetPoint.y += dy;
        Javish.self.gameMap.lastPoint.x = e.getX();
        Javish.self.gameMap.lastPoint.y = e.getY();

        int top = 0;
        int bottom = -1 * (Javish.self.gameMap.imageDimms.y - Javish.self.getHeight());

        if (Javish.self.gameMap.offsetPoint.y >= top) {
            Javish.self.gameMap.offsetPoint.y = top;
        }

        if (Javish.self.gameMap.offsetPoint.y <= bottom) {
            Javish.self.gameMap.offsetPoint.y = bottom;
        }

        Javish.self.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Javish.self.gameMap.mousePoint.x = e.getX();
        Javish.self.gameMap.mousePoint.y = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        Javish.self.gameMap.lastPoint.x = e.getX();
        Javish.self.gameMap.lastPoint.y = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Javish.self.gameMap.mousePoint.x = e.getX();
        Javish.self.gameMap.mousePoint.y = e.getY();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
