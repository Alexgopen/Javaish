package opencv;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Digit {

    private static String zeroString = "000000001100010010010010010010010010010010010010010010001100000000000000000000";
    private static String oneString = "000000000100001100000100000100000100000100000100000100000100000000000000000000";
    private static String twoString = "000000001100010010010010000010000100001000001000010000011110000000000000000000";
    private static String threeString = "000000001100010010010010000010001100000010010010010010001100000000000000000000";
    private static String fourString = "000000000010000110000110001010001010010010011111000010000010000000000000000000";
    private static String fiveString = "000000011110010000010000011100010010000010010010010010001100000000000000000000";
    private static String sixString = "000000001100010010010010010000011100010010010010010010001100000000000000000000";
    private static String sevenString = "000000011110000010000010000100000100000100001000001000001000000000000000000000";
    private static String eightString = "000000001100010010010010010010001100010010010010010010001100000000000000000000";
    private static String nineString = "000000001100010010010010010010001110000010010010010010001100000000000000000000";

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

    // A list of points may be more efficient
    boolean[][] pixels;

    private Digit() {
        pixels = new boolean[6][13];
    }

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
        System.out.println(this.getDigitString());
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

    public String getDigitString() {
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
        }
        return line;
    }

    public int getValue() {
        int ret = -1;

        if (this.equals(zero)) {
            return 0;
        }
        if (this.equals(one)) {
            return 1;
        }
        if (this.equals(two)) {
            return 2;
        }
        if (this.equals(three)) {
            return 3;
        }
        if (this.equals(four)) {
            return 4;
        }
        if (this.equals(five)) {
            return 5;
        }
        if (this.equals(six)) {
            return 6;
        }
        if (this.equals(seven)) {
            return 7;
        }
        if (this.equals(eight)) {
            return 8;
        }
        if (this.equals(nine)) {
            return 9;
        }

        return ret;
    }

    private static Digit fromString(String digitString) {
        Digit d = new Digit();

        if (digitString.length() != 6 * 13) {
            throw new IllegalArgumentException(
                    "Invalid length digit string: " + digitString.length() + ". Expected: " + 6 * 13);
        }

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 6; x++) {
                if ('1' == digitString.charAt(6 * y + x)) {
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

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 6; x++) {
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
