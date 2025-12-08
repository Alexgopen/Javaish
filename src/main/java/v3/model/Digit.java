package v3.model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Digit {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 10;

    private static final long[] DIGIT_LONGS = {
        221319753116492544L, 75453165185548544L, 221318638402078592L,
        221318646893978368L, 37744733400604800L, 545007867167056640L,
        221319626414957312L, 541003980573016576L, 221319746674041600L,
        221319748553089792L
    };

    private static final Map<Long, String> lookup = new HashMap<>();
    static {
        for (int i = 0; i < DIGIT_LONGS.length; i++) {
            lookup.put(DIGIT_LONGS[i], String.valueOf(i));
        }
        lookup.put(114968L, ",");
        lookup.put(0L, "");
    }

    private long longValue = -1;

    public Digit(BufferedImage img) {
        if (img.getWidth() != WIDTH || img.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Bad digit image dimms: " + img.getWidth() + ", " + img.getHeight());
        }
        scanPixels(img);
    }

    public boolean isValid() {
        return lookup.containsKey(longValue);
    }

    public String getString() {
        return lookup.get(longValue);
    }

    private void scanPixels(BufferedImage img) {
        long value = 0L;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                value <<= 1;
                if (img.getRGB(x, y) == Color.WHITE.getRGB()) {
                    value |= 1;
                }
            }
        }
        longValue = value;
    }
}
