package ru.programpark.mirah.compiler.impl;

import org.mirah.jvm.compiler.BytecodeConsumer;

public class NullConsumer implements BytecodeConsumer {
    @Override
    public void consumeClass(String s, byte[] bytes) {
        // do nothing
    }
}
