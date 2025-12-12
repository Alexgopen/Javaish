package com.github.alexgopen.javaish.model;

public class PixelCoord extends Point {
    
    public int x;
    public int y;
    
    public PixelCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return x + ", " + y;
    }
}
