package com.github.alexgopen.javaish.utils;

import com.github.alexgopen.javaish.model.Point;
import java.awt.Dimension;

public class CoordConverter {
    private static final double SCALE_FACTOR = 250.0 / 1000.0;
    private final Dimension panelSize;
    private final Point imageDimms;
    private Point offset = new Point(0,0);
    private boolean firstRender = true;

    public CoordConverter(Dimension panelSize, Point imageDimms) {
        this.panelSize = panelSize;
        this.imageDimms = imageDimms;
    }

    public Point worldToMap(Point wCoord) {
        double xW = wCoord.x * SCALE_FACTOR;
        double yW = wCoord.y * SCALE_FACTOR;

        Point mCoord = new Point((int)xW + offset.x, (int)yW + offset.y);

        if (firstRender) {
            // Center first render
            int centerX = panelSize.width / 2;
            int centerY = panelSize.height / 2;
            offset.x = centerX - (int)xW;
            offset.y = centerY - (int)yW;
            firstRender = false;
            mCoord.x = (int)xW + offset.x;
            mCoord.y = (int)yW + offset.y;
        }

        return mCoord;
    }

    public Point getOffset() {
        return offset;
    }
    
    public int wrapX(int x) {
        final int WRAP_FULL = 16384;
        while (x <= 0) x += WRAP_FULL;
        while (x > WRAP_FULL) x -= WRAP_FULL;
        return x;
    }
    
    public Point screenToWorld(Point screenPoint, Point offset) {
        float coordsPerPixel = 1000f / 250f; // same scale as before
        int wx = (int) ((screenPoint.x - offset.x) * coordsPerPixel);
        int wy = (int) ((screenPoint.y - offset.y) * coordsPerPixel);
        return new Point(wrapX(wx), wy);
    }

    public void shiftOffset(int dx, int dy) {
        offset.x += dx;
        offset.y += dy;
    }
}
