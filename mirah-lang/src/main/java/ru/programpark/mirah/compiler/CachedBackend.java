package ru.programpark.mirah.compiler;

import mirah.lang.ast.Script;
import org.mirah.jvm.compiler.*;
import org.mirah.typer.Typer;
import org.mirah.util.Context;

/**
 * Created by kozyr on 27.06.2016.
 */
public class CachedBackend  {
    private final Context context;
    private ScriptCompiler compiler;

    public CachedBackend(Context context) {
        this.context = context;
        context.add(org.mirah.macros.Compiler.class, ((Typer) context.get(Typer.class)).macro_compiler());
        context.add(AnnotationCompiler.class, new AnnotationCompiler(context));
        compiler = new ScriptCompiler(context);
    }

    public void visit(Script script, Object arg) {
        this.clean(script, arg);
        this.compile(script, arg);
    }

    public void clean(Script script, Object arg) {
        script.accept(new ProxyCleanup(), arg);
        script.accept(new ScriptCleanup(this.context), arg);
    }

    public void compile(Script script, Object arg) {
        script.accept(this.compiler, arg);
    }

    public Object generate(BytecodeConsumer consumer) {
      return this.compiler.generate(consumer);
    }
}
