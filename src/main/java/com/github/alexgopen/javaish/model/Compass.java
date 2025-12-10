package com.github.alexgopen.javaish.model;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.utils.CoordExtractor;

public class Compass {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 20;

    static final long MASK_LO = 3517061108995993665L;
    static final long MASK_HI = 3431314065703692L;
    static final int[] COLOR_VALUES = { 0xFFE4DBC5, 0xFFE4DBC5, 0xFFE4DBC5, 0xFFBDA36A, 0xFFE4DBC5, 0xFFE4DBC5,
            0xFFF1ECE2, 0xFFE4DBC5, 0xFF926D17, 0xFFF9F6F2, 0xFFE4DBC5, 0xFFA9883D, 0xFFFCFAF8, 0xFFF1ECE2, 0xFFBDA36A,
            0xFFFCFAF8, 0xFFF9F6F2, 0xFFE4DBC5, 0xFFE4DBC5, 0xFF926D17, 0xFF926D17, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17,
            0xFFE4DBC5, 0xFF926D17, 0xFFF1ECE2, 0xFF926D17, 0xFFF9F6F2, 0xFF926D17, 0xFFFDFDFB, 0xFFF9F6F2, 0xFFF1ECE2,
            0xFFF1ECE2, 0xFFF1ECE2, 0xFFE4DBC5, 0xFFF9F6F2, 0xFF926D17, 0xFF926D17, 0xFF926D17, 0xFF926D17, 0xFF926D17,
            0xFFF1ECE2, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17, 0xFFE4DBC5,
            0xFFA9883D };

    private boolean match;

    private static boolean test;

    public Compass(BufferedImage img) {
        if (img.getWidth() != WIDTH || img.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Bad compass image dims: " + img.getWidth() + ", " + img.getHeight());
        }
        this.match = scan(img);
    }

    public static Rectangle findCoordCropFromCompass(BufferedImage img) {
        try {
            Point p2 = findInImageBackwards(img);

            if (p2 != null) {
                int compassToCoordOffsetX = 62;
                int compassToCoordOffsetY = 6;
                BufferedImage coordCrop = img.getSubimage(p2.x + compassToCoordOffsetX, p2.y + compassToCoordOffsetY,
                        60, 10);

                Point pcoord = CoordExtractor.getPoint(coordCrop);

                // System.out.println("Is coordinate?: "+pcoord.toString());

                if (pcoord != null) {
                    return new Rectangle(p2.x + compassToCoordOffsetX, p2.y + compassToCoordOffsetY, 60, 10);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CoordNotFoundException();
        }

        throw new CoordNotFoundException();
    }

    /**
     * Search the given image for a match to the reference compass starting from the
     * bottom-right corner, scanning right-to-left, bottom-to-top. Returns the
     * top-left coordinates if found, or null if not found.
     */
    public static Point findInImageBackwards(BufferedImage img) {
        int firstPixel = COLOR_VALUES[0] & 0xFFFFFF;

        for (int y = img.getHeight() - HEIGHT; y >= 0; y--) {
            for (int x = img.getWidth() - WIDTH; x >= 0; x--) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                if (!colorsClose(rgb, firstPixel, 20))
                    continue;

                // Candidate match: check full 6x20 area
                BufferedImage sub = img.getSubimage(x, y, WIDTH, HEIGHT);
                if (scan(sub)) {
                    return new Point(x, y);
                }
            }
        }
        return null; // not found
    }

    /** Returns true if all mask-covered pixels match the reference colors. */
    public boolean isMatch() {
        return match;
    }

    static boolean scan(BufferedImage img) {
        int colorIndex = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {

                int bitIndex = y * WIDTH + x;
                boolean masked = (bitIndex < 64) ? ((MASK_LO >>> bitIndex) & 1L) != 0
                        : ((MASK_HI >>> (bitIndex - 64)) & 1L) != 0;

                if (!masked)
                    continue;

                int actual = img.getRGB(x, y) & 0xFFFFFF;
                int expected = COLOR_VALUES[colorIndex++] & 0xFFFFFF;

                if (!colorsClose(actual, expected, 20)) {
                    if (test) {
                        System.out.printf("actual %d does not match expected %d at position %d, %d\n", actual, expected,
                                x, y);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean colorsClose(int rgb1, int rgb2, int tolerance) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int dr = r1 - r2;
        int dg = g1 - g2;
        int db = b1 - b2;

        return dr * dr + dg * dg + db * db <= tolerance * tolerance;
    }
}
