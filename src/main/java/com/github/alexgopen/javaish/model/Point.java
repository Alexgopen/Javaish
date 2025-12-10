package com.github.alexgopen.javaish.model;

public class Point {
    public int x = Integer.MIN_VALUE;
    public int y = Integer.MIN_VALUE;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(int x, int y, int index) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return x + ", " + y;
    }
}
