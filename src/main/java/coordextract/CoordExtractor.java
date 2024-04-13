package coordextract;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.github.alexgopen.gvojavaish.Point;

import opencv.Digit;

public class CoordExtractor {
    public static Point getPoint(BufferedImage coordCrop) {
        Point p = null;

        int digitWidth = Digit.WIDTH;
        int height = Digit.HEIGHT;

        String allString = "";
        for (int i = 0; i < coordCrop.getWidth() / digitWidth; i++) {
            BufferedImage digitPixels = cropImage(coordCrop, new Rectangle(i * digitWidth, 0, digitWidth, height));
            Digit d = new Digit(digitPixels);
            allString += d.getString();
        }
        System.out.println(allString);

        int xVal = Integer.parseInt(allString.split(",")[0]);
        int yVal = Integer.parseInt(allString.split(",")[1]);

        String digitParsed = xVal + ", " + yVal;
        System.out.println("Digitparsed: " + digitParsed);

        Point actualCoords = new Point(xVal, yVal);
        p = actualCoords;

        return p;
    }

    private static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }
}
