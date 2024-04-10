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

class OpenCVHandler {
    public Point run() {

        Point retp = new Point(-1, -1);

        System.out.println("Running Template Matching");

        int match_method = Imgproc.TM_SQDIFF;

        String outFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/matched.png";
        String screenShotFileName = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/screenshot.png";
        String compass = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/surveycompass2.png";
        String compassMask = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/surveycompass2mask.png";

        BufferedImage screenShot = null;
        try {
            Robot robot = new Robot();
            screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            // TODO: In final version we don't want to write it
            ImageIO.write(screenShot, "PNG", new File(screenShotFileName));
            System.out.println("Screenshot taken");
        }
        catch (IOException | AWTException ex) {
            ex.printStackTrace();
        }

        String toSearch = screenShotFileName;
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

        int cropX = (int) (matchLoc.x + 66);
        int cropY = (int) (matchLoc.y + 6);
        int width = 60;
        int height = Digit.HEIGHT;
        int digitWidth = Digit.WIDTH;

        BufferedImage crop = cropImage(screenShot, new Rectangle(cropX, cropY, width, height));

        String cropPath = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/crop.png";

        try {
            ImageIO.write(crop, "PNG", new File(cropPath));
            System.out.println("Wrote crop.png");
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("AllDigits");
        String allString = "";
        for (int i = 0; i < crop.getWidth() / digitWidth; i++) {
            BufferedImage digitPixels = cropImage(crop, new Rectangle(i * digitWidth, 0, digitWidth, height));
            Digit d = new Digit(digitPixels);
            allString += d.getValueString();
            // d.printDigit();
        }
        System.out.println(allString);

        int xVal = Integer.parseInt(allString.split(",")[0]);
        int yVal = Integer.parseInt(allString.split(",")[1]);

        String digitParsed = xVal + ", " + yVal;
        System.out.println("Digitparsed: " + digitParsed);

        Point actualCoords = new Point(xVal, yVal);
        retp = actualCoords;

        return retp;
    }

    private BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }
}
