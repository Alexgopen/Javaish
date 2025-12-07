package utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

public class Compass {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 20;

    /**
     * mask[y][x] == 1 means this pixel is part of the pattern we care about.
     */
    private static final int[][] MASK = {
        {1,0,0,0,0,0},
        {1,0,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,1,0,0,0},
        {1,1,1,0,0,0},
        {1,1,1,0,0,0},
        {1,1,1,0,0,0},
        {1,1,1,1,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,1,1,1,1},
        {1,1,1,1,1,1},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0},
        {1,1,0,0,0,0}
    };

    /**
     * Expected hex color for each pixel. Initialized to "#000000".
     */
    /**
     * Expected hex color for each pixel.
     * EMPTY STRING for mask == 0
     * "#000000" (default placeholder) for mask == 1
     */
    private static final String[][] COLORS = {
	    {"#E4DBC5", "       ", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "       ", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#BDA36A", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#E4DBC5", "       ", "       ", "       ", "       "},
	    {"#F1ECE2", "#E4DBC5", "#926D17", "       ", "       ", "       "},
	    {"#F9F6F2", "#E4DBC5", "#A9883D", "       ", "       ", "       "},
	    {"#FCFAF8", "#F1ECE2", "#BDA36A", "       ", "       ", "       "},
	    {"#FCFAF8", "#F9F6F2", "#E4DBC5", "       ", "       ", "       "},
	    {"#E4DBC5", "#926D17", "#926D17", "#926D17", "       ", "       "},
	    {"#E4DBC5", "#926D17", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#926D17", "       ", "       ", "       ", "       "},
	    {"#F1ECE2", "#926D17", "       ", "       ", "       ", "       "},
	    {"#F9F6F2", "#926D17", "       ", "       ", "       ", "       "},
	    {"#FDFDFB", "#F9F6F2", "#F1ECE2", "#F1ECE2", "#F1ECE2", "#E4DBC5"},
	    {"#F9F6F2", "#926D17", "#926D17", "#926D17", "#926D17", "#926D17"},
	    {"#F1ECE2", "#926D17", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#926D17", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#926D17", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#926D17", "       ", "       ", "       ", "       "},
	    {"#E4DBC5", "#A9883D", "       ", "       ", "       ", "       "}
	};

    private boolean match;

    public Compass(BufferedImage img) {
        if (img.getWidth() != WIDTH || img.getHeight() != HEIGHT) {
            throw new IllegalArgumentException(
                "Bad compass image dims: " + img.getWidth() + ", " + img.getHeight()
            );
        }
        this.match = scan(img);
    }

    
    /**
     * MAIN METHOD FOR EXTRACTION
     */
    public static void main(String[] args) throws Exception {
        File file = new File("/home/alex/Pictures/compass.png");

        BufferedImage img = ImageIO.read(file);
        extractColors(img);
    }
    
    /**
     * Prints a ready-to-paste COLORS array from an image.
     */
    public static void extractColors(BufferedImage img) {
        if (img.getWidth() != WIDTH || img.getHeight() != HEIGHT) {
            throw new IllegalArgumentException(
                "Expected "+WIDTH+"x"+HEIGHT+" reference image, got " + img.getWidth() + "x" + img.getHeight()
            );
        }

        System.out.println("private static final String[][] COLORS = {");

        for (int y = 0; y < HEIGHT; y++) {
            System.out.print("    {");

            for (int x = 0; x < WIDTH; x++) {

                String out;

                if (MASK[y][x] == 0) {
                    out = "\"       \""; // 7 spaces
                } else {
                    int rgb = img.getRGB(x, y) & 0xFFFFFF;
                    String hex = String.format("#%06X", rgb);
                    out = "\"" + hex + "\"";
                }

                System.out.print(out);
                if (x < WIDTH - 1) System.out.print(", ");
            }

            System.out.println("},");
        }

        System.out.println("};");
    }
    
    /** Returns true if all mask-covered pixels match the reference colors. */
    public boolean isMatch() {
        return match;
    }

    private static boolean scan(BufferedImage img) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {

            	String expected = COLORS[y][x];

                // skip if mask=0 OR expected color is empty string
                if (StringUtils.isBlank(expected) || MASK[y][x] == 0) {
                    continue;
                }

                int actualRGB = img.getRGB(x, y) & 0xFFFFFF;
                int expectedRGB = parseHex(expected) & 0xFFFFFF;

                if (actualRGB != expectedRGB) {
                    return false;
                }
            }
        }
        return true;
    }

    // Converts "#RRGGBB" into java int RGB
    private static int parseHex(String hex) {
        Color c = Color.decode(hex);
        return c.getRGB();
    }
}
