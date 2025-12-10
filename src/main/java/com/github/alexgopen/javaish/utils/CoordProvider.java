package com.github.alexgopen.javaish.utils;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Point;

public class CoordProvider {
    
    private static CoordCropFinder coordCropFinder = new CoordCropFinder();
    
    public Point getCoord() throws AWTException, IOException {
        BufferedImage coordCrop = coordCropFinder.getCoordCrop();

        if (coordCrop == null)
        {
            throw new CoordNotFoundException("CoordCropFinder failed to get coordCrop. coordCrop=null");
        }
        
        Point p = CoordExtractor.getPoint(coordCrop);

        if (p == null) {
            throw new CoordNotFoundException("CoordExtractor failed to get point. p=null");
        }

        return p;
    }
    
    public static void resetPrevFoundCoordLoc()
    {
        coordCropFinder.resetPrevFoundCoordLoc();
    }
    
    public static boolean onCooldown()
    {
        return coordCropFinder.onCooldown();
    }
}
