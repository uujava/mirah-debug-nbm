package ru.programpark.mirah.compiler.impl;

/**
 * Created by kozyr on 06.09.2016.
 */
public class TooManyErrors extends RuntimeException {
    public TooManyErrors(int count) {
        super("too many errors: " + count);
    }
}
