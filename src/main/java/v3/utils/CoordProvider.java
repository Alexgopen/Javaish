package v3.utils;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import v3.exception.CoordNotFoundException;
import v3.model.Point;

public class CoordProvider {
    public Point getCoord() throws AWTException, IOException {
    	BufferedImage coordCrop = WindowCapture.getCoordCrop();
    	
        Point p = CoordExtractor.getPoint(coordCrop, true);
        
        if (p == null)
        {
        	throw new CoordNotFoundException();
        }
        else
        {
        	System.out.println("Latest coord: "+p.toString());
        }
        
        return p;
    }
}
