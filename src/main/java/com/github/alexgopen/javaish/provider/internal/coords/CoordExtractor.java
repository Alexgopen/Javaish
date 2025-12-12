package com.github.alexgopen.javaish.provider.internal.coords;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.Digit;
import com.github.alexgopen.javaish.model.WorldCoord;
import com.github.alexgopen.javaish.utils.ImageUtils;

public class CoordExtractor {
    
    public CoordExtractor()
    {
        // default
    }

    public WorldCoord getWorldCoord(BufferedImage coordCrop) throws IOException {
        WorldCoord wc = null;
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

                wc = new WorldCoord(xVal, yVal);
            }
        }
        catch (Exception e) {
            throw new CoordNotFoundException(
                    "CoordExtractor failed to extract coord. Exception=" + e.getClass().getSimpleName());
        }

        if (wc == null) {
            throw new CoordNotFoundException("CoordExtractor failed to extract coord. p=null");
        }

        return wc;
    }
}
