package com.github.alexgopen.javaish.utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageUtils {

    public static void saveImage(BufferedImage img, String name) throws IOException {
        ImageIO.write(img, "png", new File(name + ".png"));
    }

    public static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }
}
