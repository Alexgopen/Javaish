package utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Digit {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 10;

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

    private long longValue = -1;

    public Digit(BufferedImage img) {
        if (img.getWidth() != WIDTH || img.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Bad digit image dimms: " + img.getWidth() + ", " + img.getHeight());
        }

        this.scanPixels(img);
    }
    
    public long getLongValue()
    {
    	return this.longValue;
    }
    
    public boolean isValid()
    {
    	return lookup.get(this.longValue) != null;
    }
    
    public String getAsciiArt()
    {
    	String art = "";
        long v = this.longValue;

        for (int y = 0; y < Digit.HEIGHT; y++)
        {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < Digit.WIDTH; x++)
            {
                // Compute which bit we are reading:
                // Most significant bit corresponds to (0,0)
                int bitIndex = (Digit.HEIGHT * Digit.WIDTH - 1) - (y * Digit.WIDTH + x);

                boolean isWhite = ((v >>> bitIndex) & 1L) == 1L;

                line.append(isWhite ? '#' : '_');
            }

            art += line.toString() + "\r\n";
        }
        
        return art;
    }
    
    public void printAsciiArt()
    {
        long v = this.longValue;

        for (int y = 0; y < Digit.HEIGHT; y++)
        {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < Digit.WIDTH; x++)
            {
                // Compute which bit we are reading:
                // Most significant bit corresponds to (0,0)
                int bitIndex = (Digit.HEIGHT * Digit.WIDTH - 1) - (y * Digit.WIDTH + x);

                boolean isWhite = ((v >>> bitIndex) & 1L) == 1L;

                line.append(isWhite ? '#' : '_');
            }

            System.out.println(line.toString());
        }
    }

    private void scanPixels(BufferedImage img) {
        String digitChain = "";

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (img.getRGB(x, y) == Color.WHITE.getRGB()) {
                    digitChain += "1";
                }
                else {
                    digitChain += "0";
                }
            }
        }

        this.longValue = Long.parseLong(digitChain, 2);
    }

    public String getString() {
        return lookup.get(this.getLong());
    }

    private long getLong() {
        return this.longValue;
    }
}
