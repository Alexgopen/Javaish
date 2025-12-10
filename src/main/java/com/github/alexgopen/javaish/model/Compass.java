package com.github.alexgopen.javaish.model;

import java.awt.image.BufferedImage;

import com.github.alexgopen.javaish.utils.CompassFinder;

public class Compass {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 20;

    // Encodes bits representing which pixels to color compare in the top of the image
    private static final long MASK_HI = 3431314065703692L;
    // Encodes bits representing which pixels to color compare in the bottom of the image
    private static final long MASK_LO = 3517061108995993665L;

    // Encodes the colors to compare against the specified pixels for Compass matching
    public static final int[] COLOR_VALUES = { 0xFFE4DBC5, 0xFFE4DBC5, 0xFFE4DBC5, 0xFFBDA36A, 0xFFE4DBC5, 0xFFE4DBC5,
            0xFFF1ECE2, 0xFFE4DBC5, 0xFF926D17, 0xFFF9F6F2, 0xFFE4DBC5, 0xFFA9883D, 0xFFFCFAF8, 0xFFF1ECE2, 0xFFBDA36A,
            0xFFFCFAF8, 0xFFF9F6F2, 0xFFE4DBC5, 0xFFE4DBC5, 0xFF926D17, 0xFF926D17, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17,
            0xFFE4DBC5, 0xFF926D17, 0xFFF1ECE2, 0xFF926D17, 0xFFF9F6F2, 0xFF926D17, 0xFFFDFDFB, 0xFFF9F6F2, 0xFFF1ECE2,
            0xFFF1ECE2, 0xFFF1ECE2, 0xFFE4DBC5, 0xFFF9F6F2, 0xFF926D17, 0xFF926D17, 0xFF926D17, 0xFF926D17, 0xFF926D17,
            0xFFF1ECE2, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17, 0xFFE4DBC5, 0xFF926D17, 0xFFE4DBC5,
            0xFFA9883D };

    private boolean match;

    public Compass(BufferedImage compassCrop) {
        if (compassCrop.getWidth() != WIDTH || compassCrop.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Bad compass image dims: " + compassCrop.getWidth() + ", " + compassCrop.getHeight());
        }
        this.match = CompassFinder.isImageCompass(compassCrop);
    }

    /** Returns true if all mask-covered pixels match the reference colors. */
    public boolean isMatch() {
        return match;
    }
    
    public static boolean isBitMasked(int bitIndex) {
        return (bitIndex < 64) ? ((Compass.MASK_LO >>> bitIndex) & 1L) != 0
                : ((Compass.MASK_HI >>> (bitIndex - 64)) & 1L) != 0;
    }
}
