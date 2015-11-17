package ru.programpark.mirah.editor.completion.context;

import ru.programpark.mirah.editor.ast.AstPath;


/**
 *
 * @author Martin Janicek
 */
public class DotCompletionContext {

    private final int lexOffset;
    private final int astOffset;
    private final AstPath astPath;
    private final boolean fieldsOnly;
    private final boolean methodsOnly;

    public DotCompletionContext(
            int lexOffset,
            int astOffset,
            AstPath astPath,
            boolean fieldsOnly,
            boolean methodsOnly) {
        
        this.lexOffset = lexOffset;
        this.astOffset = astOffset;
        this.astPath = astPath;
        this.fieldsOnly = fieldsOnly;
        this.methodsOnly = methodsOnly;
    }

    public int getLexOffset() {
        return lexOffset;
    }

    public int getAstOffset() {
        return astOffset;
    }

    public AstPath getAstPath() {
        return astPath;
    }

    public boolean isMethodsOnly() {
        return methodsOnly;
    }

    public boolean isFieldsOnly() {
        return fieldsOnly;
    }
}
