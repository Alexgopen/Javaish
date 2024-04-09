package opencv;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Digit {
    // A list of points may be more efficient
    boolean[][] pixels;

    public Digit(BufferedImage img) {
        if (img.getWidth() != 6 || img.getHeight() != 13) {
            throw new IllegalArgumentException("Bad digit image dimms: " + img.getWidth() + ", " + img.getHeight());
        }

        pixels = new boolean[6][13];

        this.fillPixels(img);
    }

    private void fillPixels(BufferedImage img) {
        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 6; x++) {
                if (img.getRGB(x, y) == Color.WHITE.getRGB()) {
                    pixels[x][y] = true;
                }
            }
        }
    }

    public void printDigit() {
        System.out.println("Printing digit:");
        String line = "";

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 6; x++) {
                if (pixels[x][y]) {
                    line += "1";
                }
                else {
                    line += "0";
                }
            }
            System.out.println(line);
            line = "";
        }
    }
}
