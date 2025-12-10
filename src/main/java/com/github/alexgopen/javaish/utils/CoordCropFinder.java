package com.github.alexgopen.javaish.utils;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Digit;
import com.github.alexgopen.javaish.model.Point;

public class CoordCropFinder {

    private static final int COORD_CROP_SEARCH_COOLDOWN = 5000;
    
    private static final int COORD_CROP_WIDTH = Digit.WIDTH * 10;
    private static final int COORD_CROP_HEIGHT = Digit.HEIGHT;
    
    private static final int COMPASS_TO_COORD_OFFSET_X = 62;
    private static final int COMPASS_TO_COORD_OFFSET_Y = 6;
    
    private Rectangle prevFoundCoordLoc = null;
    private long lostCoordsTimestamp = 0;
    
    public CoordCropFinder()
    {
        // default
    }

    public boolean onCooldown() {
        return lostCoordsTimestamp != 0
                && (System.currentTimeMillis() - lostCoordsTimestamp) < COORD_CROP_SEARCH_COOLDOWN;
    }

    public void resetPrevFoundCoordLoc() {
        prevFoundCoordLoc = null;

        if (lostCoordsTimestamp == 0) {
            lostCoordsTimestamp = System.currentTimeMillis();
        }
    }

    private boolean shouldSearchCoords() {
        boolean shouldSearch = lostCoordsTimestamp == 0
                || (System.currentTimeMillis() - lostCoordsTimestamp) >= COORD_CROP_SEARCH_COOLDOWN;

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
            found = findCoordCropFromCompass(ss);

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
    

    public static Rectangle findCoordCropFromCompass(BufferedImage screenshot) {
        try {
            Point compassLoc = CompassFinder.findCompassInImageBackwards(screenshot);

            if (compassLoc != null) {
                BufferedImage coordCrop = screenshot.getSubimage(compassLoc.x + COMPASS_TO_COORD_OFFSET_X, compassLoc.y + COMPASS_TO_COORD_OFFSET_Y,
                        COORD_CROP_WIDTH, COORD_CROP_HEIGHT);

                Point pcoord = CoordExtractor.getPoint(coordCrop);

                if (pcoord != null) {
                    return new Rectangle(compassLoc.x + COMPASS_TO_COORD_OFFSET_X, compassLoc.y + COMPASS_TO_COORD_OFFSET_Y, COORD_CROP_WIDTH, COORD_CROP_HEIGHT);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CoordNotFoundException();
        }

        throw new CoordNotFoundException();
    }
}
