package com.github.alexgopen.javaish.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class MapLoader {

    private static final int EXPECTED_WIDTH = 4096;
    private static final int EXPECTED_HEIGHT = 2048;
    private static final String EXTERNAL_MAP = "map.png";
    private static final String DEFAULT_MAP = "/defaultmap.png";

    public static BufferedImage loadMap() throws IOException {
        BufferedImage map = loadExternalMap(new File(EXTERNAL_MAP));
        if (map == null) {
            map = loadDefaultMap();
        }
        return map;
    }

    private static BufferedImage loadExternalMap(File file) {
        if (!file.exists()) {
            showInfo("Map not found at " + file.getAbsolutePath() + ". Falling back to default map.", "Map not found");
            return null;
        }

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                showWarning("Failed to read " + file.getName()
                        + " (unsupported/invalid image). Falling back to default map.", "Map load error");
                return null;
            }

            if (!validateSize(img)) {
                showWarning(
                        String.format("%s has wrong dimensions: %dx%d (expected %dx%d). Falling back to default map.",
                                file.getName(), img.getWidth(), img.getHeight(), EXPECTED_WIDTH, EXPECTED_HEIGHT),
                        "Map size error");
                return null;
            }

            return img;
        }
        catch (IOException e) {
            showWarning("Error reading " + file.getName() + ". Falling back to default map.", "Map load error");
            return null;
        }
    }

    private static BufferedImage loadDefaultMap() {
        try {
            BufferedImage img = ImageIO.read(MapLoader.class.getResource(DEFAULT_MAP));
            if (img == null) {
                throw new IOException("Default map resource missing or unreadable.");
            }
            return img;
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load default map: " + e.getMessage(), e);
        }
    }

    private static boolean validateSize(BufferedImage img) {
        return img.getWidth() == EXPECTED_WIDTH && img.getHeight() == EXPECTED_HEIGHT;
    }

    private static void showWarning(String message, String title) {
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private static void showInfo(String message, String title) {
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
