package com.github.alexgopen.javaish.utils;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Point;

public class CoordProvider {
    public Point getCoord() throws AWTException, IOException {
    	BufferedImage coordCrop = WindowCapture.getCoordCrop();
    	
        Point p = CoordExtractor.getPoint(coordCrop, true);
        
        if (p == null) {
        	throw new CoordNotFoundException();
        }
        else {
        	// System.out.println("Latest coord: "+p.toString());
        }
        
        return p;
    }
}
