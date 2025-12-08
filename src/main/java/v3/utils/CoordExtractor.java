package v3.utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import v3.exception.CoordNotFoundException;
import v3.model.Digit;
import v3.model.Point;

public class CoordExtractor {
	
	public static final int COORD_SECTION_WIDTH = Digit.WIDTH * 10;
	public static final int COORD_SECTION_HEIGHT = Digit.HEIGHT;
	
    public static Point getPoint(BufferedImage coordCrop, boolean silent) throws IOException {
        Point p = null;

        int digitWidth = Digit.WIDTH;
        int height = Digit.HEIGHT;

        String allString = "";
        for (int i = 0; i < coordCrop.getWidth() / digitWidth; i++) {
            BufferedImage digitPixels = cropImage(coordCrop, new Rectangle(i * digitWidth, 0, digitWidth, height));
            //ImageIO.write(digitPixels, "png", new File("digit"+i+".png"));
            
            Digit d = new Digit(digitPixels);
            
            if (d.isValid())
            {
            	allString += d.getString();
            }
            else
            {
            	//System.err.printf("Parsed digit at index %d is invalid: %d\r\n", i, d.getLongValue());
            }
            
        }
        
        if (!allString.isEmpty())
        {
        	if (!silent)
        	{
        		System.out.println(allString);
        	}

            int xVal = Integer.parseInt(allString.split(",")[0]);
            int yVal = Integer.parseInt(allString.split(",")[1]);

            String digitParsed = xVal + ", " + yVal;
            
            if (!silent) {
            	System.out.println("Digitparsed: " + digitParsed);
            }

            Point actualCoords = new Point(xVal, yVal);
            p = actualCoords;
        }
        else
        {
        	if (!silent)
        	{
        		System.out.println("No coordinates found.");
        		WindowCapture.resetPrevFoundCoords();
        		throw new CoordNotFoundException();
        	}
        }

        return p;
    }

    private static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }
}
