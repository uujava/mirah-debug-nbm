package ru.programpark.mirah.editor.completion.inference;

import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import ru.programpark.mirah.editor.ast.Variable;

/**
 *
 * @author Martin Janicek
 */
public final class MethodInference {

    private MethodInference() {
    }
    
    /**
     * Tries to infer correct {@link ClassDefinition} representing type of the caller for
     * the given expression. Typically the given parameter is instance of {@link MethodCallExpression}
     * and in that case the return type of the method call is returned.<br/><br/>
     * 
     * The method also handles method chain and in such case the return type of the
     * last method call should be return.
     * 
     * @param expression
     * @return class type of the caller if found, {@code null} otherwise
     */
    @CheckForNull
    public static ClassDefinition findCallerType(@NonNull Node expression) {
        // In case if the method call is chained with another method call
        // For example: someInteger.toString().^
        /*
        if (expression instanceof MethodCallExpression) {
            MethodCallExpression methodCall = (MethodCallExpression) expression;
            
            ClassDefinition callerType = findCallerType(methodCall.getObjectExpression());
            if (callerType != null) {
                return findReturnTypeFor(callerType, methodCall.getMethodAsString(), methodCall.getArguments());
            }
        }
        */
        /*
        // In case if the method call is directly on a variable
        if (expression instanceof VariableExpression) {
            Variable variable = ((VariableExpression) expression).getAccessedVariable();
            if (variable != null) {
                return variable.getType();
            }
        }
                */
        return null;
    }
    
    @CheckForNull
    private static ClassDefinition findReturnTypeFor(
            @NonNull ClassDefinition callerType, 
            @NonNull String methodName,
            @NonNull String[] arguments) {
//            @NonNull Expression arguments) {
        
        MethodDefinition possibleMethod = null; //callerType.tryFindPossibleMethod(methodName, arguments);
        if (possibleMethod != null) {
            return null; //possibleMethod.getReturnType();
        }
        return null;
    }
}
