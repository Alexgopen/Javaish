package com.github.alexgopen.javaish.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.github.alexgopen.javaish.application.JavaishV3;

public class MapLoader {
    public static BufferedImage loadMap() throws IOException {
        boolean error = false;
        // Try loading map.png from current working directory
        File mapFile = new File("map.png");
        BufferedImage tmp = null;

        if (mapFile.exists()) {
            try {
                tmp = ImageIO.read(mapFile);
                if (tmp == null) {
                    throw new IOException();
                }
            }
            catch (IOException e) {
                tmp = null;
                String message = "Failed to read map.png (unsupported/invalid image). Falling back to default map.";
                System.err.println(message);
                JOptionPane.showMessageDialog(null, message, "Map load error", JOptionPane.WARNING_MESSAGE);
                error = true;
            }

            if (tmp != null) {
                // validate size
                if (tmp.getWidth() != 4096 || tmp.getHeight() != 2048) {
                    String message = String.format(
                            "map.png has wrong dimensions: %dx%d (expected 4096x2048). Falling back to default map.",
                            tmp.getWidth(), tmp.getHeight());
                    System.err.println(message);
                    JOptionPane.showMessageDialog(null, message, "Map size error", JOptionPane.WARNING_MESSAGE);
                    tmp = null;
                    error = true;
                }
            }
        }

        // Couldn't load a valid external map, load bundled default
        if (tmp == null) {
            if (error) {
                System.err.println("Failed to load map, falling back to default map.");
            }
            else {
                String message = "Map not found at " + mapFile.getAbsolutePath() + ", falling back to default map.";
                System.err.println(message);
                JOptionPane.showMessageDialog(null, message, "Map not found", JOptionPane.INFORMATION_MESSAGE);
            }
            tmp = ImageIO.read(JavaishV3.class.getResource("/defaultmap.png"));
            if (tmp == null) {
                throw new IOException("Default map resource missing or unreadable.");
            }
        }

        return tmp;
    }
}
