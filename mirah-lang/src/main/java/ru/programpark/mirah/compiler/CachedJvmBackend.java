package ru.programpark.mirah.compiler;

import mirah.lang.ast.Node;
import mirah.lang.ast.Script;
import org.mirah.macros.JvmBackend;
import org.mirah.typer.Typer;

import static ru.programpark.mirah.compiler.CompilerUtil.logInferred;

/**
 * Created by user on 6/22/2016.
 */
public class CachedJvmBackend implements JvmBackend {

    private final Typer macro_typer;

    public CachedJvmBackend(Typer macro_typer) {
        this.macro_typer = macro_typer;
    }

    @Override
    public Class compileAndLoadExtension(Script ast) {
        logInferred(script, macro_typer);
        processInferenceErrors(ast, macro_context);
        failIfErrors();

        macro_backend.clean(ast, null);
        processInferenceErrors(ast, macro_context);
        failIfErrors();

        macro_backend.compile(ast, nil);

        class_name = Backend.write_out_file(
                macro_backend, extension_classes, macro_destination);

        return extension_loader.loadClass(class_name);
    }

    @Override
    public void logAst(Node node) {
// do nothing
    }
}
