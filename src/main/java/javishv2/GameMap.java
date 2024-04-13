package javishv2;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import utils.Point;

public class GameMap {
    private BufferedImage imageMap;

    public Point imageDimms = new Point(0, 0);
    public Point lastPoint = new Point(0, 0);
    public Point offsetPoint = new Point(0, 0);
    public Point mousePoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public Point worldPoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

    private List<Point> coords;

    public GameMap() {
        try {
            String map = "uwogrid.png";
            imageMap = ImageIO.read(Javish.class.getResource(map));

            imageDimms.x = imageMap.getWidth();
            imageDimms.y = imageMap.getHeight();

            coords = new ArrayList<>();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addCoord(Point p) {
        coords.add(p);
    }

    public void clearPoints() {
        coords.clear();
    }

    private Point convertWtoM(Point wCoord) {
        Point mCoord = new Point(0, 0);

        double xW = wCoord.x;
        double yW = wCoord.y;

        yW /= 1000.0;
        yW *= 250.0;

        xW /= 1000.0;
        xW *= 250.0;

        mCoord.y = (int) yW + offsetPoint.y;
        mCoord.x = (int) xW + offsetPoint.x;

        return mCoord;
    }

    public void render(Graphics g) {
        int x = offsetPoint.x % imageDimms.x;
        if (x > 0) {
            x -= imageDimms.x;
        }

        int y = offsetPoint.y;

        while (x < Javish.self.getWidth()) {
            g.drawImage(imageMap, (x - imageDimms.x), y, null);
            g.drawImage(imageMap, x, y, null);
            g.drawImage(imageMap, (x + imageDimms.x), y, null);
            x += imageDimms.x;
        }

        // renderText(g);
        // renderHover(g);
        // renderPoints(g);
        // renderPointsList(g);
        // renderHint(g);
    }

}
