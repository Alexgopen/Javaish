package utils;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public class WindowCapture {
    public static void main(String[] args) throws AWTException, IOException {
        BufferedImage crop = getCoordCrop();
        ImageIO.write(crop, "png", new File("crop.png"));
        System.out.println("Wrote crop");
    }

    public static BufferedImage getCoordCrop() throws AWTException, IOException {
        BufferedImage coordCrop = null;

        BufferedImage ss = getUwoWindowScreenShot();
        ImageIO.write(ss, "png", new File("uwoss.png"));
        
        // This is not a safe way to crop the coordinates
        int arbitraryLeftCrop = 63;
        int arbitraryUpCrop = 266;
        int coordSectionWidth = 60;
        int coordSectionHeight = 10;
        Rectangle rect = new Rectangle(ss.getWidth() - arbitraryLeftCrop, ss.getHeight() - arbitraryUpCrop, coordSectionWidth, coordSectionHeight);

        try {
            BufferedImage crop = cropImage(ss, rect);
            coordCrop = crop;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return coordCrop;
    }

    public static BufferedImage getUwoWindowScreenShot() throws AWTException {
    	X11WindowUtil.WindowInfo w = X11WindowUtil.findWindowByTitle("Uncharted Waters Online");
    	if (w == null) {
    	    throw new RuntimeException("Window not found!");
    	}

        // TODO: this is undesireable
        int errX1 = 5;
        int errX2 = 5;
        int errY1 = 1;
        int errY2 = 5;
        int x = w.x + errX1;
        int y = w.y + errY1;
        int width  = w.width  - errX1 - errX2;
        int height = w.height - errY1 - errY2;
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