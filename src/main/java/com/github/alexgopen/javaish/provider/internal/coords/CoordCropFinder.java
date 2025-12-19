package com.github.alexgopen.javaish.provider.internal.coords;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Digit;
import com.github.alexgopen.javaish.model.PixelCoord;
import com.github.alexgopen.javaish.model.WorldCoord;
import com.github.alexgopen.javaish.utils.ImageUtils;

public class CoordCropFinder {

    private static final int COORD_CROP_SEARCH_COOLDOWN = 5000;
    
    private static final int COORD_CROP_WIDTH = Digit.WIDTH * 10;
    private static final int COORD_CROP_HEIGHT = Digit.HEIGHT;
    
    private static final int COMPASS_TO_COORD_OFFSET_X = 62;
    private static final int COMPASS_TO_COORD_OFFSET_Y = 6;
    
    
    private Rectangle prevFoundCoordLoc;
    private long lostCoordsTimestamp;
    
    private final CompassFinder compassFinder;
    private final ScreenCapture screenCapture;
    private final CoordExtractor coordExtractor;

    
    public CoordCropFinder() throws AWTException
    {
        this.prevFoundCoordLoc = null;
        this.lostCoordsTimestamp = 0;
        this.compassFinder = new CompassFinder();
        this.screenCapture = new ScreenCapture();
        this.coordExtractor = new CoordExtractor();
    }
    
    public WorldCoord extractWorldCoordFromCrop(BufferedImage crop) throws IOException {
        return coordExtractor.getWorldCoord(crop);
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
        BufferedImage screenshot = null;

        if (prevFoundCoordLoc == null && shouldSearchCoords()) {
            screenshot = screenCapture.getAllMonitorScreenshot();
            lostCoordsTimestamp = 0;
            prevFoundCoordLoc = this.findCoordCropFromCompass(screenshot);
        }

        if (prevFoundCoordLoc == null) {
            throw new CoordNotFoundException();
        }

        BufferedImage coordCrop = screenshot != null
                ? ImageUtils.cropImage(screenshot, prevFoundCoordLoc)
                : screenCapture.getScreenshotOfRectangle(prevFoundCoordLoc);

        try {
            coordExtractor.getWorldCoord(coordCrop);
        } catch (CoordNotFoundException e) {
            resetPrevFoundCoordLoc();
            coordCrop = null;
        }

        return coordCrop;
    }

    private Rectangle findCoordCropFromCompass(BufferedImage screenshot) {
        try {
            PixelCoord compassLoc = compassFinder.findCompassInImageBackwards(screenshot);

            if (compassLoc != null) {
                BufferedImage coordCrop = screenshot.getSubimage(compassLoc.x + COMPASS_TO_COORD_OFFSET_X, compassLoc.y + COMPASS_TO_COORD_OFFSET_Y,
                        COORD_CROP_WIDTH, COORD_CROP_HEIGHT);

                WorldCoord wc = coordExtractor.getWorldCoord(coordCrop);

                if (wc != null) {
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
