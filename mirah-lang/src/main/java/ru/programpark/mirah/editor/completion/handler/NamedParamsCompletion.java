package ru.programpark.mirah.editor.completion.handler;

import ru.programpark.mirah.editor.completion.context.CompletionContext;
import java.util.List;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Node;
import org.netbeans.modules.csl.api.CompletionProposal;

/**
 * Code completion handler for name parameters.
 *
 * @author Martin Janicek <mjanicek@netbeans.org>
 */
public class NamedParamsCompletion extends BaseCompletion {

    private CompletionContext context;

    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext context, int anchor) {
        this.context = context;

        Node leaf = context.path.leaf();
/*
        if (leaf instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructorCall = (ConstructorCallExpression) leaf;
            
            Expression constructorArgs = constructorCall.getArguments();
            if (constructorArgs instanceof TupleExpression) {
                List<Expression> arguments = ((TupleExpression) constructorArgs).getExpressions();

                if (arguments.isEmpty()) {
                    completeNamedParams(proposals, anchor, constructorCall, null);
                } else {
                    for (Expression argExpression : arguments) {
                        if (argExpression instanceof NamedArgumentListExpression) {
                            completeNamedParams(proposals, anchor, constructorCall, (NamedArgumentListExpression) argExpression);
                        }
                    }
                }
            }
        }

        Node leafParent = context.path.leafParent();
        Node leafGrandparent = context.path.leafGrandParent();
        
//        if (leafParent instanceof NamedArgumentListExpression &&
//            leafGrandparent instanceof ConstructorCallExpression) {
//
//            completeNamedParams(proposals, anchor, (ConstructorCallExpression) leafGrandparent, (NamedArgumentListExpression) leafParent);
//        }
*/
        return false;
    }
/*
    private void completeNamedParams(
            List<CompletionProposal> proposals,
            int anchor,
            ConstructorCallExpression constructorCall,
            NamedArgumentListExpression namedArguments) {

        ClassDefinition type = constructorCall.getType();
        String prefix = context.getPrefix();

        for (FieldDeclaration fieldNode : type.getFields()) {
            if (fieldNode.getLineNumber() < 0 || fieldNode.getColumnNumber() < 0) {
                continue;
            }

            String typeName = fieldNode.getType().getNameWithoutPackage();
            String name = fieldNode.getName();

            // If the prefix is empty, complete only missing parameters
            if ("".equals(prefix)) {
                if (isAlreadyPresent(namedArguments, name)) {
                    continue;
                }
            // Otherwise check if the field is starting with (and not equal to) the prefix
            } else {
                if (name.equals(prefix) || !name.startsWith(prefix)) {
                    continue;
                }
            }

            proposals.add(new CompletionItem.NamedParameter(typeName, name, anchor));
        }
    }
*/
    /**
     * Check if the given name is in the list of named parameters.
     *
     * @param namedArgsExpression named parameters
     * @param name name
     * @return {@code true} if the given name is in the list of named parameters, {@code false} otherwise
     */
    /*
    private boolean isAlreadyPresent(NamedArgumentListExpression namedArgsExpression, String name) {
        if (namedArgsExpression == null) {
            return false;
        }
        List<MapEntryExpression> namedArgs = namedArgsExpression.getMapEntryExpressions();

        for (MapEntryExpression namedEntry : namedArgs) {
            String namedArgument = namedEntry.getKeyExpression().getText();
            if (namedArgument != null && namedArgument.equals(name)) {
                return true;
            }
        }
        return false;
    }
*/
    /*
    private boolean isParameterPrefix(NamedArgumentListExpression namedArgsExpression, String name) {
        List<MapEntryExpression> namedArgs = namedArgsExpression.getMapEntryExpressions();

        for (MapEntryExpression namedEntry : namedArgs) {
            String namedArgument = namedEntry.getKeyExpression().getText();
            if (namedArgument != null && name.startsWith(namedArgument)) {
                return true;
            }
        }
        return false;
    }
    */
}
