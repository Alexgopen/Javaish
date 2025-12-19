package com.github.alexgopen.javaish.provider.internal.coords;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

public class ScreenCapture {

    private Robot robot;
    
    public ScreenCapture() throws AWTException
    {
        this.robot = new Robot();
    }

    public BufferedImage getScreenshotOfRectangle(Rectangle bounds) throws AWTException {
        BufferedImage screenshot = robot.createScreenCapture(bounds);

        return screenshot;
    }

    public BufferedImage getAllMonitorScreenshot() throws AWTException {
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