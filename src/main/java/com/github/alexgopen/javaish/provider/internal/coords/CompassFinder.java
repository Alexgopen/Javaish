package com.github.alexgopen.javaish.provider.internal.coords;

import java.awt.image.BufferedImage;

import com.github.alexgopen.javaish.model.Compass;
import com.github.alexgopen.javaish.model.Point;
import com.github.alexgopen.javaish.utils.ImageUtils;

public class CompassFinder {

    private static final int COLOR_TOLERANCE = 20;

    public CompassFinder()
    {
        
    }
    
    public Point findCompassInImageBackwards(BufferedImage screenshot) {
        final int firstPixel = Compass.COLOR_VALUES[0] & 0xFFFFFF;

        final int searchHeight = screenshot.getHeight() - Compass.HEIGHT;
        final int searchWidth = screenshot.getWidth() - Compass.WIDTH;

        for (int y = searchHeight; y >= 0; y--) {
            for (int x = searchWidth; x >= 0; x--) {
                int rgb = screenshot.getRGB(x, y) & 0xFFFFFF;
                if (!ImageUtils.colorsClose(rgb, firstPixel, COLOR_TOLERANCE)) {
                    continue;
                }

                // Candidate match: check full 6x20 area
                BufferedImage sub = screenshot.getSubimage(x, y, Compass.WIDTH, Compass.HEIGHT);
                if (isImageCompass(sub)) {
                    return new Point(x, y);
                }
            }
        }
        return null; // not found
    }

    public boolean isImageCompass(BufferedImage compassCrop) {
        int colorIndex = 0;

        if (compassCrop.getWidth() != Compass.WIDTH || compassCrop.getHeight() != Compass.HEIGHT) {
            return false;
        }

        for (int y = 0; y < Compass.HEIGHT; y++) {
            for (int x = 0; x < Compass.WIDTH; x++) {

                int bitIndex = y * Compass.WIDTH + x;

                if (!Compass.isBitMasked(bitIndex)) {
                    continue;
                }

                int actual = compassCrop.getRGB(x, y) & 0xFFFFFF;
                int expected = Compass.COLOR_VALUES[colorIndex++] & 0xFFFFFF;

                if (!ImageUtils.colorsClose(actual, expected, COLOR_TOLERANCE)) {
                    return false;
                }
            }
        }
        return true;
    }
}
