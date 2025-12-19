package com.github.alexgopen.javaish.exception;

public class CoordNotFoundException extends RuntimeException {

    public CoordNotFoundException(String string) {
        super(string);
    }

    public CoordNotFoundException() {
        super();
    }

    private static final long serialVersionUID = -5553461239297792505L;

}
