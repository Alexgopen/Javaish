package javishv2;

public class Point {
    public int x = Integer.MIN_VALUE;
    public int y = Integer.MIN_VALUE;
    public int index;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.index = 0;
    }

    public Point(int x, int y, int index) {
        this.x = x;
        this.y = y;
        this.index = index;
    }
}
