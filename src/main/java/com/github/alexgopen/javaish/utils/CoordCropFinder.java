package com.github.alexgopen.javaish.utils;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Compass;
import com.github.alexgopen.javaish.model.Point;

public class CoordCropFinder {
    private Rectangle prevFoundCoordLoc = null;
    private long lostCoordsTimestamp = 0;
    
    public CoordCropFinder()
    {
        
    }

    public boolean onCooldown() {
        return lostCoordsTimestamp != 0
                && (System.currentTimeMillis() - lostCoordsTimestamp) < 5000;
    }

    public void resetPrevFoundCoordLoc() {
        prevFoundCoordLoc = null;

        if (lostCoordsTimestamp == 0) {
            lostCoordsTimestamp = System.currentTimeMillis();
        }
    }

    private boolean shouldSearchCoords() {
        boolean shouldSearch = lostCoordsTimestamp == 0
                || (System.currentTimeMillis() - lostCoordsTimestamp) >= 5000;

        return shouldSearch;
    }

    public BufferedImage getCoordCrop() throws AWTException, IOException {
        BufferedImage coordCrop = null;

        BufferedImage ss = null;

        Rectangle found = null;
        if (prevFoundCoordLoc == null && shouldSearchCoords()) {
            ss = ScreenCapture.getAllMonitorScreenshot();

            lostCoordsTimestamp = 0;
            // Try to locate the coordinate display by scanning the lower-right quadrant
            found = Compass.findCoordCropFromCompass(ss);

            if (found != null) {
                prevFoundCoordLoc = found;
            }
            else {
                throw new CoordNotFoundException();
            }
        }
        else {
            found = prevFoundCoordLoc;
        }

        if (found != null) {
            try {
                if (ss != null) {
                    coordCrop = ImageUtils.cropImage(ss, found);
                }
                else {
                    coordCrop = ScreenCapture.getScreenshotOfRectangle(found);
                }

                try {
                    // Either extracts a valid coordinate or throws exception
                    Point p = CoordExtractor.getPoint(coordCrop);
                }
                catch (Exception e) {
                    // Reset and we will try again on the next pass of the loop
                    resetPrevFoundCoordLoc();
                }
            }
            catch (Exception e) {
                resetPrevFoundCoordLoc();
                e.printStackTrace();
            }
        }
        else {
            throw new CoordNotFoundException();
        }

        return coordCrop;
    }
}
