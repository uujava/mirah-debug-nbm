package ru.programpark.mirah.editor.completion.handler;

import ru.programpark.mirah.lexer.MirahParserResult;
import ru.programpark.mirah.editor.completion.context.CompletionContext;
import ru.programpark.mirah.editor.completion.context.CaretLocation;

import java.util.List;
import java.util.logging.Level;
import mirah.lang.ast.Node;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.completion.CompletionItem;
import ru.programpark.mirah.editor.navigator.VariablesCollector;

/**
 *
 * @author Martin Janicek
 */
public class LocalVarCompletion extends BaseCompletion {

    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor) {
        LOG.log(Level.FINEST, "-> completeLocalVars"); // NOI18N

        if (!(request.location == CaretLocation.INSIDE_CLOSURE || request.location == CaretLocation.INSIDE_METHOD)
                // handle $someprefix in string
            && !(request.location == CaretLocation.INSIDE_STRING && request.getPrefix().matches("\\$[^\\{].*"))) {
            LOG.log(Level.FINEST, "Not inside method, closure or in-string variable, bail out."); // NOI18N
            return false;
        }

        // If we are right behind a dot, there's no local-vars completion.
        if (request.isBehindDot()) {
            LOG.log(Level.FINEST, "We are invoked right behind a dot."); // NOI18N
            return false;
        }

//        VariableFinderVisitor vis = new VariableFinderVisitor(((ModuleNode) request.path.root()).getContext(),
//                request.path, request.doc, request.astOffset);
//        vis.collect();

//        VariablesCollector vc = new VariablesCollector(ASTUtils.findLeaf(parsed, bdoc, caretOffset), bdoc, caretOffset);
        Node leaf = ASTUtils.findLeaf(request.getParserResult(), request.doc, request.lexOffset);
        VariablesCollector vc = new VariablesCollector(
                (MirahParserResult)request.getParserResult(),
                leaf,
                request.doc, request.lexOffset);
//            VariablesCollector vc = new VariablesCollector(path,bdoc,caretOffset);
        vc.collect();
        
        boolean updated = false;

        // If we are dealing with GStrings, the prefix is prefixed ;-)
        // ... with the dollar sign $ See # 143295
        int anchorShift = 0;
        String varPrefix = request.getPrefix();

        for (String name : vc.getVariables()) {
            if (varPrefix.length() < 1) {
                proposals.add(new CompletionItem.LocalVarItem(name, anchor + anchorShift));
                updated = true;
            } 
            else if (!name.equals(varPrefix) && (name.startsWith(varPrefix) 
                || (name.charAt(0) == '@' && name.startsWith("@"+varPrefix))) ) 
            {
                proposals.add(new CompletionItem.LocalVarItem(name, anchor + anchorShift));
                updated = true;
            }
//            proposals.add(new CompletionItem.LocalVarItem(name, anchor));
        }

        if (request.getPrefix().startsWith("$")) {
            varPrefix = request.getPrefix().substring(1);
            anchorShift = 1;
        }
        /*
        ArrayList<Variable> list = new ArrayList<Variable>();
        for (Variable node : list /*vis.getVariables()* ) {
            String varName = node.getName();
            LOG.log(Level.FINEST, "Node found: {0}", varName); // NOI18N

            if (varPrefix.length() < 1) {
                proposals.add(new CompletionItem.LocalVarItem(node, anchor + anchorShift));
                updated = true;
            } else if (!varName.equals(varPrefix) && varName.startsWith(varPrefix)) {
                proposals.add(new CompletionItem.LocalVarItem(node, anchor + anchorShift));
                updated = true;
            }
        }
        */
        return updated;
    }
}
