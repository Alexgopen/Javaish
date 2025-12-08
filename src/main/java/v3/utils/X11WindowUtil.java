package v3.utils;

import com.sun.jna.*;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.IntByReference;

import java.util.ArrayList;
import java.util.List;

public class X11WindowUtil {

    private static final X11 x11 = X11.INSTANCE;
    private static final X11.Display display = x11.XOpenDisplay(null);

    public static class WindowInfo {
        public X11.Window window;
        public int x, y, width, height;
        public String title;

        @Override
        public String toString() {
            return String.format("Window[%s] @ (%d,%d) %dx%d", title, x, y, width, height);
        }
    }

    public static List<WindowInfo> getAllWindows() {
        List<WindowInfo> result = new ArrayList<>();
        X11.Window root = x11.XDefaultRootWindow(display);
        walkWindows(root, result);
        return result;
    }

    private static void walkWindows(X11.Window win, List<WindowInfo> out) {
        // Try to read title
        PointerByReference nameRef = new PointerByReference();
        x11.XFetchName(display, win, nameRef);
        Pointer namePtr = nameRef.getValue();

        String title = null;
        if (namePtr != null) {
            title = namePtr.getString(0);
            x11.XFree(namePtr);
        }

        // Add window if it has a title
        if (title != null && !title.trim().isEmpty()) {
            X11.XWindowAttributes attrs = new X11.XWindowAttributes();
            x11.XGetWindowAttributes(display, win, attrs);

            WindowInfo info = new WindowInfo();
            info.window = win;
            info.width = attrs.width;
            info.height = attrs.height;
            info.title = title;

            // Translate coordinates to root (screen) coordinates
            IntByReference absX = new IntByReference();
            IntByReference absY = new IntByReference();
            X11.WindowByReference childRef = new X11.WindowByReference(); 
            x11.XTranslateCoordinates(display, win, x11.XDefaultRootWindow(display),
                    0, 0, absX, absY, childRef);

            info.x = absX.getValue();
            info.y = absY.getValue();

            out.add(info);
        }

        // Recursively walk children
        PointerByReference childrenRef = new PointerByReference();
        IntByReference countRef = new IntByReference();
        X11.WindowByReference rootRef = new X11.WindowByReference();
        X11.WindowByReference parentRef = new X11.WindowByReference();

        x11.XQueryTree(display, win, rootRef, parentRef, childrenRef, countRef);
        int count = countRef.getValue();
        Pointer childrenPtr = childrenRef.getValue();

        if (childrenPtr != null) {
            long[] windows = childrenPtr.getLongArray(0, count);
            for (long w : windows) {
                walkWindows(new X11.Window(w), out);
            }
            x11.XFree(childrenPtr);
        }
    }


    public static WindowInfo findWindowByTitle(String needle) {
        for (WindowInfo w : getAllWindows()) {
            if (w.title.contains(needle)) {
                return w;
            }
        }
        return null;
    }
}
