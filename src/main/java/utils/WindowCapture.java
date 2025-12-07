package utils;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public class WindowCapture {
	
	private static Rectangle prevFoundCoords = null;
	
    public static void main(String[] args) throws AWTException, IOException {
        BufferedImage crop = getCoordCrop();
        ImageIO.write(crop, "png", new File("crop.png"));
        System.out.println("Wrote crop");
    }

    public static BufferedImage getCoordCrop() throws AWTException, IOException {
        BufferedImage coordCrop = null;

        BufferedImage ss = getUwoWindowScreenShot();
        ImageIO.write(ss, "png", new File("uwoss.png"));
        
        Rectangle found = null;
        if (prevFoundCoords == null)
        {
        	// Try to locate the coordinate display by scanning the lower-right quadrant
            long startTime = System.currentTimeMillis();
            found = findCoordInLowerRightQuadrant(ss);
            long endTime = System.currentTimeMillis();
            System.out.println("Quadrant coord search took "+(endTime-startTime)+" ms");
        }
        else
        {
        	found = WindowCapture.prevFoundCoords;
        }
        
        
        if (found != null) {
            try {
                BufferedImage crop = cropImage(ss, found);
                coordCrop = crop;
                System.out.printf("Found coord crop at (%d,%d)\n", found.x, found.y);
                // optionally write debug
                ImageIO.write(coordCrop, "png", new File("found_coord_crop.png"));
                // Also attempt parsing to show result
                try {
                    Point p = CoordExtractor.getPoint(coordCrop, false);
                    System.out.printf("Parsed coords: %s\n", p);
                } catch (Exception e) {
                	WindowCapture.prevFoundCoords = null;
                    System.err.println("Parsing failed on found crop (unexpected): " + e.getMessage());
                }
            } catch (Exception e) {
            	WindowCapture.prevFoundCoords = null;
                e.printStackTrace();
            }
        } 
        else if (false) {
        	WindowCapture.prevFoundCoords = null;
            // Fallback to original arbitrary crop to preserve old behavior
            System.err.println("Coordinate display not auto-detected — using arbitrary fallback crop.");
            int arbitraryLeftCrop = 63;
            int arbitraryUpCrop = 266;
            Rectangle rect = new Rectangle(ss.getWidth() - arbitraryLeftCrop, ss.getHeight() - arbitraryUpCrop, CoordExtractor.COORD_SECTION_WIDTH, CoordExtractor.COORD_SECTION_HEIGHT);
            try {
                BufferedImage crop = cropImage(ss, rect);
                coordCrop = crop;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
        	WindowCapture.prevFoundCoords = null;
        }

        return coordCrop;
    }
    
    /**
     * Search the lower-right quadrant for a valid coordinate display.
     * Scans top->bottom, left->right within the quadrant and attempts to parse
     * a 60x10 crop at each location using CoordExtractor.getPoint().
     *
     * @param ss full screenshot of the game window
     * @return Rectangle of the first found crop, or null if not found
     * @throws IOException 
     */
    private static Rectangle findCoordInLowerRightQuadrant(BufferedImage ss) throws IOException {
        final int width = ss.getWidth();
        final int height = ss.getHeight();

        // Define lower-right quadrant bounds. We use half width/height to approximate quadrant.
        final int startX = width / 2;
        final int startY = height / 2;
        final int maxX = width - CoordExtractor.COORD_SECTION_WIDTH;
        final int maxY = height - CoordExtractor.COORD_SECTION_HEIGHT;

        System.out.printf("Scanning for coord display in region x [%d..%d], y [%d..%d]\n", startX, maxX, startY, maxY);
        ImageIO.write(ss, "png", new File("quadrant.png"));
        
        
        for (int y = startY; y <= maxY; y++) {
            for (int x = startX; x <= maxX; x++) {
                try {
                    Rectangle candidateRect = new Rectangle(x, y, CoordExtractor.COORD_SECTION_WIDTH, CoordExtractor.COORD_SECTION_HEIGHT);
                    BufferedImage candidate = cropImage(ss, candidateRect);

                    // Quick sanity: we might skip candidates that are uniformly blank or all black/white,
                    // but here we'll directly attempt to parse with your existing extractor.
                    try {
                        Point p = CoordExtractor.getPoint(candidate, true);
                        if (p != null) {
                            // success: return the location of the candidate crop
                            System.out.printf("Found parseable coordinate crop at (%d,%d) -> %s\n", x, y, p);
                            return candidateRect;
                        }
                    } catch (NumberFormatException nfe) {
                        // parsed digits but not a valid coordinate string; skip
                    } catch (IOException ioe) {
                        // Digit writing IO or similar - ignore and continue scanning
                    } catch (Exception ex) {
                        // Any other parse-time exception: ignore and continue scanning
                    }
                } catch (RasterFormatException rfe) {
                    // skip invalid subimage regions (shouldn't happen because of bounds)
                } catch (Exception e) {
                    // unexpected; print debug and continue
                    // System.err.println("Unexpected error while scanning candidate: " + e.getMessage());
                }
            }
        }
        
        System.out.println("Finished quandrant search, found nothing.");

        // nothing found
        return null;
    }

    public static BufferedImage getUwoWindowScreenShot() throws AWTException {
    	X11WindowUtil.WindowInfo w = X11WindowUtil.findWindowByTitle("Uncharted Waters Online");
    	if (w == null) {
    	    throw new RuntimeException("Window not found!");
    	}

        int x = w.x;
        int y = w.y;
        int width  = w.width;
        int height = w.height;
        Rectangle dimms = new Rectangle(x, y, width, height);

        BufferedImage createScreenCapture = new Robot().createScreenCapture(dimms);
        return createScreenCapture;
    }

    private static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }

    public static WindowInfo getWindowInfo(int hWnd) {
        RECT r = new RECT();
        User32.instance.GetWindowRect(hWnd, r);
        byte[] buffer = new byte[1024];
        User32.instance.GetWindowTextA(hWnd, buffer, buffer.length);
        String title = Native.toString(buffer);
        WindowInfo info = new WindowInfo(hWnd, r, title);
        return info;
    }

    public static interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(int hWnd, int lParam);
    }

    public static interface User32 extends StdCallLibrary {
        public static final String SHELL_TRAY_WND = "Shell_TrayWnd";
        public static final int WM_COMMAND = 0x111;
        public static final int MIN_ALL = 0x1a3;
        public static final int MIN_ALL_UNDO = 0x1a0;

        final User32 instance = Native.load("user32", User32.class);

        boolean EnumWindows(WndEnumProc wndenumproc, int lParam);

        boolean IsWindowVisible(int hWnd);

        int GetWindowRect(int hWnd, RECT r);

        void GetWindowTextA(int hWnd, byte[] buffer, int buflen);

        int GetTopWindow(int hWnd);

        int GetWindow(int hWnd, int flag);

        boolean ShowWindow(int hWnd);

        boolean BringWindowToTop(int hWnd);

        int GetActiveWindow();

        boolean SetForegroundWindow(int hWnd);

        int FindWindowA(String winClass, String title);

        long SendMessageA(int hWnd, int msg, int num1, int num2);

        final int GW_HWNDNEXT = 2;
    }

    public static class RECT extends Structure {
        public int left, top, right, bottom;

        @Override
        protected List<String> getFieldOrder() {
            List<String> order = new ArrayList<>();
            order.add("left");
            order.add("top");
            order.add("right");
            order.add("bottom");
            return order;
        }
    }

    public static class WindowInfo {
        int hwnd;
        RECT rect;
        String title;

        public WindowInfo(int hwnd, RECT rect, String title) {
            this.hwnd = hwnd;
            this.rect = rect;
            this.title = title;
        }

        @Override
        public String toString() {
            return String.format("(%d,%d)-(%d,%d) : \"%s\"", rect.left, rect.top, rect.right, rect.bottom, title);
        }
    }
}