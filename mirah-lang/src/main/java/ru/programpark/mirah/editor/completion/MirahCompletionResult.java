package ru.programpark.mirah.editor.completion;

import ru.programpark.mirah.editor.completion.context.CompletionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mirah.lang.ast.Import;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.spi.DefaultCompletionResult;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.utils.ImportHelper;
import ru.programpark.mirah.editor.utils.ImportUtils;

/**
 * Mirah specific implementation of {@link CodeCompletionResult}. This implementation
 * is needed for fast import (post processing insert item).
 *
 * @author Martin Janicek
 */
public class MirahCompletionResult extends DefaultCompletionResult {

    private final CompletionContext context;
    private final Node root;
    private final FileObject fo;

    public MirahCompletionResult(List<CompletionProposal> list, CompletionContext context) {
        super(list, false);

        this.context = context;
        this.root = ASTUtils.getRoot(context.getParserResult());
        this.fo = context.getSourceFile();
    }

    @Override
    public void afterInsert(@NonNull CompletionProposal item) {
        // See issue #235175
        if (root == null) {
            return;
        }

        // Don't add import statement if we are completing import statement - see #228587
        if (context.isBehindImportStatement()) {
            return;
        }

        if (item instanceof CompletionItem.TypeItem) {
            CompletionItem.TypeItem typeItem = (CompletionItem.TypeItem) item;
            
            // Don't add import statement for default imports
            if (ImportUtils.isDefaultlyImported(typeItem.getFqn())) {
                return;
            }
        }

        final ElementKind kind = item.getKind();
        final String name = item.getName();
        
        if (kind == ElementKind.CLASS || kind == ElementKind.INTERFACE || kind == ElementKind.CONSTRUCTOR) {
            List<String> imports = ImportCollector.collect(root);

            if (!imports.contains(name)) {
//                ImportHelper.resolveImport(fo, root.getPackageName(), name);
            }
        }
    }
    
    private static final class ImportCollector extends NodeScanner {

        private final Node scriptNode;
        private final List<String> imports;

        private ImportCollector(Node scriptNode) {
            this.scriptNode = scriptNode;
            this.imports = new ArrayList<String>();
        }

        public static List<String> collect(Node root) {
            ImportCollector collector = new ImportCollector(root);
            
            //??root.getScript().accept(collector, null);
//            collector.visitImports(root);
            return collector.imports;
        }

//        @Override
//        protected SourceUnit getSourceUnit() {
//            return scriptNode.getContext();
//        }

        public boolean enterImport(Import node, Object arg) {
            imports.add(node.fullName().identifier());
            return true;
        }
        
//        @Override
//        public void visitImports(ScriptNode node) {
//            for (ImportNode importNode : node.getImports()) {
//                imports.add(importNode.getType().getNameWithoutPackage());
//            }
//            super.visitImports(node);
//        }
    }
}
