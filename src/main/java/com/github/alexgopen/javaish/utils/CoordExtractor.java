package com.github.alexgopen.javaish.utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Digit;
import com.github.alexgopen.javaish.model.Point;

public class CoordExtractor {

    public static final int COORD_SECTION_WIDTH = Digit.WIDTH * 10;
    public static final int COORD_SECTION_HEIGHT = Digit.HEIGHT;

    public static Point getPoint(BufferedImage coordCrop) throws IOException {
        Point p = null;
        int digitWidth = Digit.WIDTH;
        int height = Digit.HEIGHT;
        StringBuilder allString = new StringBuilder();

        try {
            for (int i = 0; i < coordCrop.getWidth() / digitWidth; i++) {
                BufferedImage digitPixels = ImageUtils.cropImage(coordCrop,
                        new Rectangle(i * digitWidth, 0, digitWidth, height));

                Digit d = new Digit(digitPixels);

                if (d.isValid()) {
                    allString.append(d.getString());
                }

            }

            if (!allString.toString().isEmpty()) {
                String[] coordParts = allString.toString().split(",");
                if (coordParts.length != 2) {
                    throw new CoordNotFoundException("Invalid coordParts length: " + coordParts.length);
                }

                int xVal = Integer.parseInt(coordParts[0]);
                int yVal = Integer.parseInt(coordParts[1]);

                p = new Point(xVal, yVal);
            }
        }
        catch (Exception e) {
            throw new CoordNotFoundException(
                    "CoordExtractor failed to extract coord. Exception=" + e.getClass().getSimpleName());
        }

        if (p == null) {
            throw new CoordNotFoundException("CoordExtractor failed to extract coord. p=null");
        }

        return p;
    }
}
