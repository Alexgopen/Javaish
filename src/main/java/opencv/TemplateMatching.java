package opencv;

import org.opencv.imgproc.Imgproc;

import nu.pattern.OpenCV;

public class TemplateMatching {
    public static void main(String[] args) {
        // System.loadLibrary("opencv_java246");
        OpenCV.loadLocally();
        args = new String[3];
        new MatchingDemo().run(args[0], args[1], args[2], Imgproc.TM_SQDIFF);
    }
}