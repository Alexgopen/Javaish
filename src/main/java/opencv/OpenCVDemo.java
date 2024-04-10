package opencv;

import org.opencv.core.Point;

import nu.pattern.OpenCV;

public class OpenCVDemo {
    public static Point main(String[] args) {
        // System.loadLibrary("opencv_java246");
        OpenCV.loadLocally();
        Point p = new OpenCVHandler().run();

        return p;
    }

    public static Point getCoord() {
        return OpenCVDemo.main(null);
    }
}