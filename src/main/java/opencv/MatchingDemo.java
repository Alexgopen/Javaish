package opencv;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

class MatchingDemo {
    public void run(String inFile, String templateFile, String outFile, int match_method) {
        System.out.println("\nRunning Template Matching");

        inFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/examplesurvey.png";
        templateFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/comma.png";
        outFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/matched.png";

        String ssFileName = "sstest.png";
        String compass = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/surveycompass2.png";
        String compassMask = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/surveycompass2mask.png";
        try {
            Robot robot = new Robot();
            BufferedImage screenShot = robot
                    .createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            // TODO: In final version we don't want to write it
            ImageIO.write(screenShot, "PNG", new File(ssFileName));
            System.out.println("Screenshot taken");
        }
        catch (IOException | AWTException ex) {
            ex.printStackTrace();
        }

        String toSearch = ssFileName;
        String toFind = compass;
        String toFindMask = compassMask;

        Mat img = Imgcodecs.imread(toSearch, Imgcodecs.IMREAD_COLOR);
        Mat templ = Imgcodecs.imread(toFind, Imgcodecs.IMREAD_COLOR);
        Mat mask = Imgcodecs.imread(toFindMask, Imgcodecs.IMREAD_COLOR);

        // / Create the result matrix
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(img, templ, result, match_method, mask);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        // / Localizing the best match with minMaxLoc
        MinMaxLocResult mmr = Core.minMaxLoc(result);

        Point matchLoc;
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        }
        else {
            matchLoc = mmr.maxLoc;
        }

        // / Show me what you got
        Imgproc.rectangle(img, matchLoc, new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows()),
                new Scalar(0, 255, 0));

        System.out.printf("%d, %d\r\n", (int) matchLoc.x, (int) matchLoc.y);

        // Save the visualized detection.
        System.out.println("Writing " + outFile);
        Imgcodecs.imwrite(outFile, img);

    }
}
