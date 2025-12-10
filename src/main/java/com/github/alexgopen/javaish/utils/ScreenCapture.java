package com.github.alexgopen.javaish.utils;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

public class ScreenCapture {

    private static Robot robot;

    public static BufferedImage getScreenshotOfRectangle(Rectangle bounds) throws AWTException {
        if (robot == null) {
            robot = new Robot();
        }

        BufferedImage screenshot = robot.createScreenCapture(bounds);

        return screenshot;
    }

    public static BufferedImage getAllMonitorScreenshot() throws AWTException {
        // Get the union of all monitor bounds
        Rectangle allBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            allBounds = allBounds.union(bounds);
        }

        return getScreenshotOfRectangle(allBounds);
    }
}