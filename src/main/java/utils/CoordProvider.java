package utils;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class CoordProvider {

    private List<Point> testPoints;
    private Random r;

    int lastX = 0;
    int lastY = 0;

    int xDir;
    int yDir;

    public CoordProvider() {
        r = new Random(1337);
        lastX = 750 + 14000;
        lastY = 750 + 2500;

        xDir = r.nextBoolean() ? 1 : -1;
        yDir = r.nextBoolean() ? 1 : -1;
    }

    public Point getCoord() throws AWTException, IOException {
        // return new Point(15750, 3300);
        // return new Point(0, 0);
        // return getNextTestPoint();

    	BufferedImage coordCrop = WindowCapture.getCoordCrop();
    	
        Point p = CoordExtractor.getPoint(coordCrop, false);

        return p;
    }

    private Point getNextTestPoint() {

        if (r.nextFloat() < 0.1f) {
            xDir *= -1;
        }
        if (r.nextFloat() < 0.1f) {
            yDir *= -1;
        }

        int xOff = r.nextInt(100) * xDir;
        int yOff = r.nextInt(100) * yDir;

        int newX = lastX + xOff;
        int newY = lastY + yOff;

        int cappedX = Math.max(14000, Math.min(newX, 14000 + 750 * 2));
        int cappedY = Math.max(2500, Math.min(newY, 2500 + 750 * 2));

        double clampX = 1 - (Math.abs(cappedX - (750 + 14000)) / 750.0);
        double clampY = 1 - (Math.abs(cappedY - (750 + 2500)) / 750.0);

        newX = (int) (lastX + xOff * clampX);
        newY = (int) (lastY + yOff * clampY);

        cappedX = Math.max(14000, Math.min(newX, 14000 + 750 * 2));
        cappedY = Math.max(2500, Math.min(newY, 2500 + 750 * 2));

        lastX = cappedX;
        lastY = cappedY;

        return new Point(cappedX, cappedY);
    }
}
