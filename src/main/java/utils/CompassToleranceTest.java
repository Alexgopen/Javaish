package utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class CompassToleranceTest {

    private static final int TOLERANCE = 20;

    public static void main(String[] args) throws Exception {
        BufferedImage baseline = generateBaselineImage();
        List<Result> results = new ArrayList<>();

        // directions: -1 = down, 1 = up
        int[] dirs = {-1, 1};

        // RGB channel masks
        int[][] channelCombos = {
                {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, // single
                {1, 1, 0}, {1, 0, 1}, {0, 1, 1}, // pairs
                {1, 1, 1}                          // all three
        };

        for (int[] combo : channelCombos) {
            for (int drDir : dirs) {
                for (int dgDir : dirs) {
                    for (int dbDir : dirs) {
                        int dr = combo[0] == 1 ? drDir : 0;
                        int dg = combo[1] == 1 ? dgDir : 0;
                        int db = combo[2] == 1 ? dbDir : 0;

                        Result res = findToleranceShift(baseline, dr, dg, db);
                        results.add(res);
                        System.out.println(res);
                    }
                }
            }
        }

        saveCompositeImage(results, "compass_tolerance_test.png");
    }

    private static BufferedImage generateBaselineImage() {
        BufferedImage img = new BufferedImage(Compass.WIDTH, Compass.HEIGHT, BufferedImage.TYPE_INT_RGB);
        int colorIndex = 0;
        for (int y = 0; y < Compass.HEIGHT; y++) {
            for (int x = 0; x < Compass.WIDTH; x++) {
                boolean masked = (y * Compass.WIDTH + x < 64)
                        ? ((Compass.MASK_LO >>> (y * Compass.WIDTH + x)) & 1L) != 0
                        : ((Compass.MASK_HI >>> (y * Compass.WIDTH + x - 64)) & 1L) != 0;
                if (!masked) {
                    img.setRGB(x, y, 0xFFFFFF);
                } else {
                    img.setRGB(x, y, Compass.COLOR_VALUES[colorIndex++] & 0xFFFFFF);
                }
            }
        }
        return img;
    }

    private static Result findToleranceShift(BufferedImage baseline, int drDir, int dgDir, int dbDir) {
        BufferedImage testImg = deepCopy(baseline);
        int shiftR = 0, shiftG = 0, shiftB = 0;

        while (true) {
            shiftImage(testImg, drDir, dgDir, dbDir);
            if (!Compass.scan(testImg)) {
                shiftImage(testImg, -drDir, -dgDir, -dbDir);
                break;
            }
            shiftR += drDir;
            shiftG += dgDir;
            shiftB += dbDir;
        }

        return new Result(testImg, shiftR, shiftG, shiftB);
    }

    private static void shiftImage(BufferedImage img, int dr, int dg, int db) {
        int colorIndex = 0;
        for (int y = 0; y < Compass.HEIGHT; y++) {
            for (int x = 0; x < Compass.WIDTH; x++) {
                int bitIndex = y * Compass.WIDTH + x;
                boolean masked = (bitIndex < 64)
                        ? ((Compass.MASK_LO >>> bitIndex) & 1L) != 0
                        : ((Compass.MASK_HI >>> (bitIndex - 64)) & 1L) != 0;
                if (!masked) continue;

                int rgb = img.getRGB(x, y);
                int r = clamp(((rgb >> 16) & 0xFF) + dr);
                int g = clamp(((rgb >> 8) & 0xFF) + dg);
                int b = clamp((rgb & 0xFF) + db);
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
                colorIndex++;
            }
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static void saveCompositeImage(List<Result> results, String filename) throws Exception {
        int spacing = 10;
        int labelHeight = 16;
        int leftMargin = 5;
        int interImageSpacing = 10;
        int textOffset = 12;

        int width = Compass.WIDTH * 2 + interImageSpacing + 110;
        int height = Compass.HEIGHT + spacing + (Compass.HEIGHT + spacing + labelHeight) * results.size() + 10;

        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = composite.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 12));

        BufferedImage baseline = generateBaselineImage();
        g.drawImage(baseline, leftMargin, 5, null);
        g.drawString("Baseline", leftMargin + Compass.WIDTH + 5, 5 + Compass.HEIGHT / 2);

        for (int i = 0; i < results.size(); i++) {
            int y = Compass.HEIGHT + spacing + i * (Compass.HEIGHT + spacing + labelHeight);
            Result r = results.get(i);
            g.drawImage(baseline, leftMargin, y, null);
            g.drawImage(r.img, leftMargin + Compass.WIDTH + interImageSpacing, y, null);
            g.drawString(r.label(), leftMargin + Compass.WIDTH + interImageSpacing + textOffset, y + Compass.HEIGHT - 2);
        }

        g.dispose();
        ImageIO.write(composite, "PNG", new File(filename));
        System.out.println("Saved composite image: " + filename);
    }

    private static class Result {
        BufferedImage img;
        int rShift, gShift, bShift;

        Result(BufferedImage img, int rShift, int gShift, int bShift) {
            this.img = deepCopy(img);
            this.rShift = rShift;
            this.gShift = gShift;
            this.bShift = bShift;
        }

        public String label() {
            return String.format("R:%+d G:%+d B:%+d", rShift, gShift, bShift);
        }

        public String toString() {
            return label();
        }
    }
}
