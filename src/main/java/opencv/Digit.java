package opencv;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Digit {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 10;

    private static String zeroString = "001100010010010010010010010010010010010010010010001100000000";
    private static String oneString = "000100001100000100000100000100000100000100000100000100000000";
    private static String twoString = "001100010010010010000010000100001000001000010000011110000000";
    private static String threeString = "001100010010010010000010001100000010010010010010001100000000";
    private static String fourString = "000010000110000110001010001010010010011111000010000010000000";
    private static String fiveString = "011110010000010000011100010010000010010010010010001100000000";
    private static String sixString = "001100010010010010010000011100010010010010010010001100000000";
    private static String sevenString = "011110000010000010000100000100000100001000001000001000000000";
    private static String eightString = "001100010010010010010010001100010010010010010010001100000000";
    private static String nineString = "001100010010010010010010001110000010010010010010001100000000";
    private static String commaString = "000000000000000000000000000000000000000000011100000100011000";

    private static Digit zero = Digit.fromString(zeroString);
    private static Digit one = Digit.fromString(oneString);
    private static Digit two = Digit.fromString(twoString);
    private static Digit three = Digit.fromString(threeString);
    private static Digit four = Digit.fromString(fourString);
    private static Digit five = Digit.fromString(fiveString);
    private static Digit six = Digit.fromString(sixString);
    private static Digit seven = Digit.fromString(sevenString);
    private static Digit eight = Digit.fromString(eightString);
    private static Digit nine = Digit.fromString(nineString);
    private static Digit comma = Digit.fromString(commaString);

    // A list of points may be more efficient
    boolean[][] pixels;

    private Digit() {
        pixels = new boolean[WIDTH][HEIGHT];
    }

    public Digit(BufferedImage img) {
        if (img.getWidth() != WIDTH || img.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Bad digit image dimms: " + img.getWidth() + ", " + img.getHeight());
        }

        pixels = new boolean[WIDTH][HEIGHT];

        this.fillPixels(img);
    }

    private void fillPixels(BufferedImage img) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (img.getRGB(x, y) == Color.WHITE.getRGB()) {
                    pixels[x][y] = true;
                }
            }
        }
    }

    public String getDigitString() {
        String line = "";
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (pixels[x][y]) {
                    line += "1";
                }
                else {
                    line += "0";
                }
            }
        }
        return line;
    }

    public String getValueString() {
        String ret = "";

        if (this.equals(zero)) {
            return "" + 0;
        }
        if (this.equals(one)) {
            return "" + 1;
        }
        if (this.equals(two)) {
            return "" + 2;
        }
        if (this.equals(three)) {
            return "" + 3;
        }
        if (this.equals(four)) {
            return "" + 4;
        }
        if (this.equals(five)) {
            return "" + 5;
        }
        if (this.equals(six)) {
            return "" + 6;
        }
        if (this.equals(seven)) {
            return "" + 7;
        }
        if (this.equals(eight)) {
            return "" + 8;
        }
        if (this.equals(nine)) {
            return "" + 9;
        }
        if (this.equals(comma)) {
            return ",";
        }

        return ret;
    }

    private static Digit fromString(String digitString) {
        Digit d = new Digit();

        if (digitString.length() != WIDTH * HEIGHT) {
            throw new IllegalArgumentException(
                    "Invalid length digit string: " + digitString.length() + ". Expected: " + WIDTH * HEIGHT);
        }

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if ('1' == digitString.charAt(WIDTH * y + x)) {
                    d.pixels[x][y] = true;
                }
                else {
                    d.pixels[x][y] = false;
                }
            }
        }

        return d;
    }

    public static boolean equals(Digit a, Digit b) {
        boolean allMatch = true;

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                allMatch = allMatch && a.pixels[x][y] == b.pixels[x][y];
                if (!allMatch) {
                    break;
                }
            }
            if (!allMatch) {
                break;
            }
        }

        return allMatch;
    }

    @Override
    public boolean equals(Object o) {
        boolean ret = false;

        if (o instanceof Digit) {
            ret = Digit.equals(this, (Digit) o);
        }

        return ret;
    }
}
