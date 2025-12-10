package com.github.alexgopen.javaish.utils;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Point;

public class CoordProvider {
    public Point getCoord() throws AWTException, IOException {
        BufferedImage coordCrop = WindowCapture.getCoordCrop();

        if (coordCrop == null)
        {
            throw new CoordNotFoundException("WindowCapture failed to get coordCrop. coordCrop=null");
        }
        
        Point p = CoordExtractor.getPoint(coordCrop, true);

        if (p == null) {
            throw new CoordNotFoundException("CoordExtractor failed to get point. p=null");
        }

        return p;
    }
}
