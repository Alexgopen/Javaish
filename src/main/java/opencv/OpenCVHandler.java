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

import tess.TessUtil;

class OpenCVHandler {
    public void run(String inFile, String templateFile, String outFile, int match_method) {
        System.out.println("\nRunning Template Matching");

        inFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/examplesurvey.png";
        templateFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/comma.png";
        outFile = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/matched.png";

        String ssFileName = "sstest.png";
        String compass = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/surveycompass2.png";
        String compassMask = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/surveycompass2mask.png";

        BufferedImage screenShot = null;
        try {
            Robot robot = new Robot();
            screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
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

        int cropX = (int) (matchLoc.x + 66);
        int cropY = (int) (matchLoc.y + 5);
        int width = 60;
        int height = 13;

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

        try {
            int commaWidth = 6;
            BufferedImage commaImg;
            commaImg = ImageIO
                    .read(new File("C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/comma.png"));
            Point commaOffset = findXYOfBWithinA(crop, commaImg);

            BufferedImage coordX = cropImage(crop, new Rectangle(0, 0, (int) commaOffset.x - 1, crop.getHeight()));
            BufferedImage coordY = cropImage(crop, new Rectangle((int) commaOffset.x + commaWidth - 1, 0,
                    (int) (crop.getWidth() - (commaOffset.x + 6)) + 1, crop.getHeight()));

            ImageIO.write(coordX, "PNG",
                    new File("C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/coordX.png"));
            ImageIO.write(coordY, "PNG",
                    new File("C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/coordY.png"));

            String ocrx = TessUtil.parseImage(coordX);
            String ocry = TessUtil.parseImage(coordY);

            System.out.printf("Split ocr: %s, %s\r\n", ocrx.trim(), ocry.trim());

            int xWidth = coordX.getWidth();
            int yWidth = coordY.getWidth();
            BufferedImage tempX = coordX;
            BufferedImage tempY = coordY;
            System.out.println("X digits");
            String xString = "";
            for (int i = 0; i < xWidth / 6; i++) {
                BufferedImage digitPixels = cropImage(tempX, new Rectangle(i * 6, 0, 6, 13));
                Digit d = new Digit(digitPixels);
                xString += d.getValue();
                // d.printDigit();
            }

            System.out.println("Y digits");
            String yString = "";
            for (int i = 0; i < yWidth / 6; i++) {
                BufferedImage digitPixels = cropImage(tempY, new Rectangle(i * 6, 0, 6, 13));
                Digit d = new Digit(digitPixels);
                yString += d.getValue();
                // d.printDigit();
            }

            int xVal = Integer.parseInt(xString);
            int yVal = Integer.parseInt(yString);

            String digitParsed = xVal + ", " + yVal;
            System.out.println("Digitparsed: " + digitParsed);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Find comma within crop, split it, and OCR each half

        String ocred = TessUtil.parseImage(crop);
        System.out.println("Combined ocr: " + ocred);

    }

    private Point findXYOfBWithinA(BufferedImage toSearch, BufferedImage toFind) {
        Point p = new Point(-1, -1);
        String toSearchPath = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/toSearch.png";
        String toFindPath = "C:/Users/Alex/eclipse-workspace/gvojavaish/src/main/java/opencv/toFind.png";

        try {
            ImageIO.write(toSearch, "PNG", new File(toSearchPath));
            ImageIO.write(toFind, "PNG", new File(toFindPath));

            Mat img = Imgcodecs.imread(toSearchPath, Imgcodecs.IMREAD_COLOR);
            Mat templ = Imgcodecs.imread(toFindPath, Imgcodecs.IMREAD_COLOR);

            // / Create the result matrix
            int result_cols = img.cols() - templ.cols() + 1;
            int result_rows = img.rows() - templ.rows() + 1;
            Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

            // / Do the Matching and Normalize
            int match_method = Imgproc.TM_SQDIFF;
            Imgproc.matchTemplate(img, templ, result, match_method);
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

            System.out.printf("Comma at %d, %d\r\n", (int) matchLoc.x, (int) matchLoc.y);

            p.x = (int) matchLoc.x;
            p.y = (int) matchLoc.y;
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return p;
    }

    private BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }
}
