package ru.programpark.mirah.editor.completion.inference;

import java.util.Collections;
import java.util.Set;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Node;
import org.netbeans.editor.BaseDocument;
import ru.programpark.mirah.editor.ast.AstPath;

public class MirahTypeAnalyzer {

    private final BaseDocument document;

    public MirahTypeAnalyzer(BaseDocument document) {
        this.document = document;
    }

    public Set<ClassDefinition> getTypes(AstPath path, int astOffset) {
        Node caller = path.leaf();
/*            
        if (caller instanceof VariableExpression) {
            Script script = (Script) path.root();
            TypeInferenceVisitor typeVisitor = new TypeInferenceVisitor(script.getContext(), path, document, astOffset);
            typeVisitor.collect();
            
            ClassDefinition guessedType = typeVisitor.getGuessedType();
            if (guessedType != null) {
                return Collections.singleton(guessedType);
            }
        }
*/        
        
//        if (caller instanceof MethodCallExpression) {
//            return Collections.singleton(MethodInference.findCallerType(caller));
//        }
        
        return Collections.emptySet();
    }
}
