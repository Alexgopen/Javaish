package opencv;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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

    private static int[] zeroEncodes = { 206120082, 306782976 };
    private static int[] oneEncodes = { 70271236, 68174080 };
    private static int[] twoEncodes = { 206119044, 136382336 };
    private static int[] threeEncodes = { 206119052, 38347520 };
    private static int[] fourEncodes = { 35152522, 310124672 };
    private static int[] fiveEncodes = { 507578130, 38347520 };
    private static int[] sixEncodes = { 206119964, 306782976 };
    private static int[] sevenEncodes = { 503849220, 69239296 };
    private static int[] eightEncodes = { 206120076, 306782976 };
    private static int[] nineEncodes = { 206120078, 38347520 };
    private static int[] commaEncodes = { 0, 114968 };

    private static final long zeroLong = 221319753116492544L;
    private static final long oneLong = 75453165185548544L;
    private static final long twoLong = 221318638402078592L;
    private static final long threeLong = 221318646893978368L;
    private static final long fourLong = 37744733400604800L;
    private static final long fiveLong = 545007867167056640L;
    private static final long sixLong = 221319626414957312L;
    private static final long sevenLong = 541003980573016576L;
    private static final long eightLong = 221319746674041600L;
    private static final long nineLong = 221319748553089792L;
    private static final long commaLong = 114968L;
    private static final long blankLong = 0L;

    private static Map<Long, String> lookup;

    static {
        lookup = new HashMap<>();
        lookup.put(zeroLong, "0");
        lookup.put(oneLong, "1");
        lookup.put(twoLong, "2");
        lookup.put(threeLong, "3");
        lookup.put(fourLong, "4");
        lookup.put(fiveLong, "5");
        lookup.put(sixLong, "6");
        lookup.put(sevenLong, "7");
        lookup.put(eightLong, "8");
        lookup.put(nineLong, "9");
        lookup.put(commaLong, ",");
        lookup.put(blankLong, "");
    }

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
    private long longValue = -1;

    boolean test = true;

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
        long l = -1;
        String digitChain = "";

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (img.getRGB(x, y) == Color.WHITE.getRGB()) {
                    pixels[x][y] = true;
                    digitChain += "1";
                }
                else {
                    digitChain += "0";
                }
            }
        }

        l = Long.parseLong(digitChain, 2);
        this.longValue = l;
    }

    private String getString() {
        return lookup.get(this.getLong());
    }

    private long getLong() {

        if (this.longValue != -1) {
            return this.longValue;
        }

        long l = -1;

        String digitChain = "";

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (pixels[x][y]) {
                    digitChain += "1";
                }
                else {
                    digitChain += "0";
                }
            }
        }

        l = Long.parseLong(digitChain, 2);

        this.longValue = l;

        return l;
    }

    public void printDigit() {
        System.out.println("Printing digit:");
        System.out.println(this.getValueString());
        System.out.println(this.getLong());
        String line = "";

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (pixels[x][y]) {
                    line += "#";
                }
                else {
                    line += ".";
                }
            }

            System.out.println(line);
            line = "";
        }

    }

    public static void printAllDigits() {
        zero.printDigit();
        one.printDigit();
        two.printDigit();
        three.printDigit();
        four.printDigit();
        five.printDigit();
        six.printDigit();
        seven.printDigit();
        eight.printDigit();
        nine.printDigit();
        comma.printDigit();
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

        if (test) {
            return this.getString();
        }

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
