package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.Script;
import org.mirah.jvm.compiler.*;
import org.mirah.typer.Typer;
import org.mirah.util.Context;
import ru.programpark.mirah.compiler.CacheConsumer;

/**
 * Created by kozyr on 27.06.2016.
 */
public class TypedBackend extends Backend  {

    public TypedBackend(Context context) {
       super(context);
    }

    public Object generate(CacheConsumer consumer){
        return super.generate(consumer);
    }
}
