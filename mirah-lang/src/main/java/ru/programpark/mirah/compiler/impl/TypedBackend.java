package ru.programpark.mirah.compiler.impl;

import org.mirah.jvm.compiler.*;
import org.mirah.util.Context;
import ru.programpark.mirah.compiler.MapCacheConsumer;

/**
 * Created by kozyr on 27.06.2016.
 */
public class TypedBackend extends Backend  {

    public TypedBackend(Context context) {
       super(context);
    }

    public Object generate(MapCacheConsumer consumer){
        return super.generate(consumer);
    }
}
