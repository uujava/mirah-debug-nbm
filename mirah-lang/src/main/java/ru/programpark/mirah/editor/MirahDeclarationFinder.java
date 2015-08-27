/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.programpark.mirah.editor;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.cc.AstSupport;
import ca.weblite.netbeans.mirah.hyperlinks.HyperlinkElement;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.AnnotationList;
import mirah.lang.ast.Call;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Import;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.ModifierList;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Script;
import mirah.lang.ast.SimpleString;
import mirah.lang.ast.TypeName;
import mirah.lang.ast.TypeNameList;
import mirah.lang.ast.TypeRefImpl;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.java.ElementDeclaration;
import ru.programpark.mirah.editor.java.ElementSearch;
import ru.programpark.mirah.editor.utils.MirahUtils;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;
import ru.programpark.mirah.index.elements.IndexedElement;
import ru.programpark.mirah.index.elements.IndexedMethod;
import ru.programpark.mirah.tests.LexUtilities;

/**
 *
 * @author savushkin
 */
public class MirahDeclarationFinder implements DeclarationFinder {

    public static HashMap<String,String> primitivesMap = new HashMap<String,String>();
    private static final String t_boolean = "boolean";
    private static final String t_int = "int";
    private static final String t_short = "short";
    private static final String t_long = "long";
    private static final String t_char = "char";
    private static final String t_float = "float";
    private static final String t_double = "double";
            
    static {
        primitivesMap.put(t_boolean,null);
        primitivesMap.put(t_int,null);
        primitivesMap.put(t_short,null);
        primitivesMap.put(t_long,null);
        primitivesMap.put(t_char,null);
        primitivesMap.put(t_float,null);
        primitivesMap.put(t_double,null);
    }
    
    public static FileObject getFileObject(Document doc) {
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        return od != null ? od.getPrimaryFile() : null;
    }
    
    private String getCurrentPackage( Node root ) {
        if (root == null) return null;
        
        final String[] packages = new String[1];
        root.accept(new NodeScanner() {
//            @Override
            public boolean enterPackage(Package node, Object arg) {
                packages[0] = node.getName();
                return false;
            }
        }, null);
        return packages[0];
    }
    
    public void findSuperClass( ClassDefinition classDef )
    {
        Class cl = classDef.getClass();
        TypeName type = classDef.superclass();
        Node script = classDef.findAncestor(Script.class);
        if ( script != null )
        {
            LinkedList<String> imports = AstSupport.collectImports(script);
        }
    }

    // Check name as imported class
    public DeclarationLocation checkImportClasses( MirahParser.NBMirahParserResult parsed, SimpleString ss )
    {
        Node root = parsed.getRoot();
        String name = ss.identifier();
        if ( root != null )
        {
            LinkedList<String> imports = AstSupport.collectImports(root);
            for( String imp : imports )
            if ( imp.endsWith(name) && imp.charAt(imp.length() - name.length() - 1) == '.' )
            {    
                FileObject fo = parsed.getSnapshot().getSource().getFileObject();
                BaseDocument doc = (BaseDocument)parsed.getSnapshot().getSource().getDocument(false);
                return findType(imp, OffsetRange.NONE, doc, parsed, MirahIndex.get(fo));
            }
        }
        return DeclarationLocation.NONE;
    }
   
    private DeclarationLocation processCall( Call call, ClassDefinition classDef, MirahParser.NBMirahParserResult parsed, int caretOffset, AstPath path, MirahIndex index ) // Р’С‹Р·РѕРІ РјРµС‚РѕРґР°?
    {
        ResolvedType type = parsed.getResolvedType(call);
        String name = call.name().identifier();
        String returnedType = type.name();
        String className = classDef.name().identifier();
        String superName = null;
        if ( classDef.superclass() != null )
        {
            superName = classDef.superclass().typeref().name();
        }
        DeclarationLocation location = findMethod(name, null, returnedType, call, parsed, caretOffset, caretOffset, path, call,  index);
        return location == null ? DeclarationLocation.NONE : location;
    }
    
    private DeclarationLocation processImport( Import imp, BaseDocument doc, MirahParser.NBMirahParserResult info, MirahIndex index )
    {
        String fqName = imp.fullName().identifier();
        return findType(fqName, OffsetRange.NONE, doc, info, index);
    }
    
    // ссылка на имя класса
    private DeclarationLocation processRef( TypeRefImpl ref, BaseDocument doc, MirahParser.NBMirahParserResult parsed, MirahIndex index )
    {
        String name = ref.name();
        String fqn = null;
        
        // пытаюсь вычислить квалифицированное имя класса в списке импорта
        LinkedList<String> includes = AstSupport.collectImports(parsed.getRoot());
        for( String incl : includes )
        {
            if ( incl.length() > name.length() && incl.endsWith(name) && incl.charAt(incl.length() - name.length() - 1) == '.' ) 
            {
                fqn = incl; break;
            }
        }
        
        DeclarationLocation location = DeclarationLocation.NONE;
        if ( fqn != null ) location = findType(fqn, OffsetRange.NONE, doc, parsed, index);
        // не получилось, попробую найти по имени класса
        if ( location == DeclarationLocation.NONE )
        {
            Set<IndexedClass> classes = index.getClasses(name, QuerySupport.Kind.EXACT);
            for( IndexedClass iclass : classes )        
            {
                FileObject fileObject = iclass.getFileObject();
                if (fileObject == null) break;
                int offset = iclass.getOffset();
                location = new DeclarationLocation(fileObject, offset, iclass);
            }
        }
        return location;        
    }
    
    @Override
    public DeclarationLocation findDeclaration(ParserResult info, int caretOffset) 
    {
        MirahParser.NBMirahParserResult parsed = (MirahParser.NBMirahParserResult)info;
        FileObject fo = info.getSnapshot().getSource().getFileObject();
        BaseDocument bdoc = (BaseDocument)info.getSnapshot().getSource().getDocument(false);
//        AstPath path = ASTUtils.getPath(info, (BaseDocument)doc, caretOffset);
        
        String packg = getCurrentPackage(parsed.getRoot());
        
//        LinkedList<String> includes = AstSupport.collectImports(parsed.getRoot());
        
        Node leaf = ASTUtils.findLeaf(parsed, bdoc, caretOffset);
        if (leaf == null) return DeclarationLocation.NONE;

        ClassDefinition classDef = null;
        MethodDefinition methodDef = null;
        Node node = leaf;
        while( node != null) {
            if (node instanceof ClassDefinition && classDef == null)
                classDef = (ClassDefinition) node;

            if (node instanceof MethodDefinition && methodDef == null)
                methodDef = (MethodDefinition) node;
            
            node = node.parent();
        }

        String name = null;
        node = leaf;
        while (node != null) {

            if (node instanceof Import) {
                return processImport((Import) node, bdoc, parsed, MirahIndex.get(fo));
            }

            if (node instanceof Call) {
                return processCall((Call) node, classDef, parsed, caretOffset, null, MirahIndex.get(fo));
            }

            if (node instanceof SimpleString) {
                name = ((SimpleString) node).identifier();
            }
            
            if (node instanceof TypeRefImpl) {
                return processRef((TypeRefImpl) node, bdoc, parsed, MirahIndex.get(fo));
            }
            
            if ( false )
            { // goto resolved type
                ResolvedType type = parsed.getResolvedType(node);
                if (type == null) {
                    continue;
                }
                String fqn = type.name();
                if (primitivesMap.containsKey(fqn)) {
                    fqn = null;
                    continue;
                }
                return findType(fqn, OffsetRange.NONE, bdoc, info, MirahIndex.get(fo));
            }
            node = node.parent();
        }
  
        /*
        if ( path == null ) return DeclarationLocation.NONE;

        ClassDefinition classDef = null;
        MethodDefinition methodDef = null;
        for (Iterator<Node> it = path.iterator(); it.hasNext();) 
        {
            Node node = it.next();
            if ( node instanceof ClassDefinition && classDef == null )
                classDef = (ClassDefinition)node;

            if ( node instanceof MethodDefinition && methodDef == null ) //test
                methodDef = (MethodDefinition)node;
        }
        
        String name = null;
        for (Iterator<Node> it = path.iterator(); it.hasNext();) 
        {
            Node node = it.next();
            if ( node instanceof Import ) 
                return processImport((Import) node,(BaseDocument) doc, parsed, MirahIndex.get(fo));
            
            if ( node instanceof Call ) 
                return processCall((Call) node, classDef, parsed, caretOffset, path, MirahIndex.get(fo));
            
            if ( node instanceof SimpleString )
            {
                name = ((SimpleString)node).identifier();
            }
            if ( node instanceof TypeRefImpl )
                return processRef((TypeRefImpl) node, (BaseDocument) doc, parsed, MirahIndex.get(fo));
            
            { // goto resolved type
                ResolvedType type = parsed.getResolvedType(node);
                if ( type == null ) continue;
                String fqn = type.name();
                if ( primitivesMap.containsKey(fqn) ) 
                {
                    fqn = null; continue;
                }
                return findType(fqn, OffsetRange.NONE, (BaseDocument)doc, info, MirahIndex.get(fo));
            }
        }
        */
        return DeclarationLocation.NONE;
    }

    @Override
    public OffsetRange getReferenceSpan(Document doc, int offset) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if (getFileObject(doc) == null) {
            //do nothing if FO is not attached to the document - the goto would not work anyway:
            return null;
        }
        final OffsetRange[] ret = new OffsetRange[] { OffsetRange.NONE }; 
        final Document fdoc = doc;
        final int foffset = offset;
        doc.render(new Runnable() {
            @Override
            public void run() {
//                TokenHierarchy th = TokenHierarchy.get(fdoc);
//                TokenSequence<JavaTokenId> ts = SourceUtils.getJavaTokenSequence(th, foffset);
                TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(fdoc, foffset);
                if (ts == null)
                    return;
                
                ts.move(foffset);
                if (!ts.moveNext())
                    return;

                Token<MirahTokenId> t = ts.token();

                if ( t.id().is(Tokens.tJavaDoc)) {
                    // javadoc hyperlinking (references + param names)
                    /*
                    TokenSequence<JavadocTokenId> jdts = ts.embedded(JavadocTokenId.language());
                    if (false) { //JavadocImports.isInsideReference(jdts, offset) || JavadocImports.isInsideParamName(jdts, offset)) {
                        jdts.move(offset);
                        jdts.moveNext();
                        if (token != null) {
                            token[0] = t;
                        }
                        ret[0] = new int [] {jdts.offset(), jdts.offset() + jdts.token().length()};
                    }
                    */
                    return;
                } else { //if ( true /*!USABLE_TOKEN_IDS.contains(t.id())* ) {
                    ts.move(foffset - 1);
                    if (!ts.moveNext())
                        return;
                    t = ts.token();
//                    if (!USABLE_TOKEN_IDS.contains(t.id()))
//                        return;
                }

//                if (token != null)
//                    token[0] = t;

                if ( t.id().is(Tokens.tClassVar) ||
                    t.id().is(Tokens.tIDENTIFIER) ||
                    t.id().is(Tokens.tSelf) ||
                    t.id().is(Tokens.tSuper) ||
                    t.id().is(Tokens.tSuper) ||
                    t.id().is(Tokens.tCONSTANT) ||
                    t.id().is(Tokens.tInstVar) ) {
                    ret[0] = new OffsetRange(ts.offset(), ts.offset() + t.length());
                }
            }
        });
        return ret[0];
        
    }

    /*
    @Override
    public OffsetRange getReferenceSpan(Document document, int lexOffset) {
        final TokenHierarchy<Document> th = TokenHierarchy.get(document);
        final TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(document, lexOffset);
        if (ts == null) {
            return OffsetRange.NONE;
        }

        ts.move(lexOffset);

        if (!ts.moveNext() && !ts.movePrevious()) {
            return OffsetRange.NONE;
        }

        // Determine whether the caret position is right between two tokens
        final boolean isBetween = (lexOffset == ts.offset());
        OffsetRange range = getReferenceSpan(ts, th, lexOffset);

        if ((range == OffsetRange.NONE) && isBetween) {
            // The caret is between two tokens, and the token on the right
            // wasn't linkable. Try on the left instead.
            if (ts.movePrevious()) {
                range = getReferenceSpan(ts, th, lexOffset);
            }
        }
        return range;
    }
    */
    private DeclarationLocation findJavaClass(String fqName, 
            /*BaseDocument doc,*/ ParserResult info ) throws BadLocationException {
        
            FileObject fileObject = info.getSnapshot().getSource().getFileObject();

            if (fileObject != null) {
                final ClasspathInfo cpi = ClasspathInfo.create(fileObject);

                if (cpi != null) {
                    JavaSource javaSource = JavaSource.create(cpi);

                    if (javaSource != null) {
                        CountDownLatch latch = new CountDownLatch(1);
                        SourceLocator locator = new SourceLocator(fqName, cpi, latch);
                        try {
                            javaSource.runUserActionTask(locator, false);
                        } catch (IOException ex) {
//                            LOG.log(Level.FINEST, "Problem in runUserActionTask :  {0}", ex.getMessage());
                            return DeclarationLocation.NONE;
                        }
                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return DeclarationLocation.NONE;
                        }
                        return locator.getLocation();
                    } else {
//                        LOG.log(Level.FINEST, "javaSource == null"); // NOI18N
                    }
                } else {
//                    LOG.log(Level.FINEST, "classpathinfo == null"); // NOI18N
                }
            } else {
//                LOG.log(Level.FINEST, "fileObject == null"); // NOI18N
            }

            return DeclarationLocation.NONE;
    }

    private DeclarationLocation findType(String fqName, OffsetRange range,
            BaseDocument doc, ParserResult info, MirahIndex index) {

//        LOG.log(Level.FINEST, "Looking for type: {0}", fqName); // NOI18N
        if (doc != null && range != null) {
/*
            String text = doc.getText(range.getStart(), range.getLength());

            if(!MirahUtils.stripPackage(fqName).equals(text)){
                // check for inner classes
                String[] parts = fqName.split(Pattern.quote("$")); // NOI18N
                if (parts.length < 2) {
                    return DeclarationLocation.NONE;
                }

                boolean found = false;
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(part).append("$");
                    if (MirahUtils.stripPackage(part).equals(text)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return DeclarationLocation.NONE;
                }

                builder.setLength(builder.length() - 1);
                fqName = builder.toString();

//                int sepIndex = fqName.indexOf("$"); // NOI18N
//                if (sepIndex <= 0 || !NbUtilities.stripPackage(fqName.substring(0, sepIndex)).equals(text)) {
//                    LOG.log(Level.FINEST, "fqName != text");
//                    return DeclarationLocation.NONE;
//                } else {
//                    fqName = fqName.substring(0, sepIndex);
//                }
            }
*/
//          Set<IndexedClass> classes = index.getClasses(fqName, QuerySupport.Kind.EXACT);
            Set<IndexedClass> classes = index.findClassesByFqn(fqName); //.getClasses(fqName, QuerySupport.Kind.EXACT);
            if ( ! classes.isEmpty() )
            {
                IndexedClass indexedClass = classes.iterator().next();
                return new DeclarationLocation(indexedClass.getFileObject(), indexedClass.getOffset());
            }
/*            
            for (IndexedClass indexedClass : classes) {
                Node node = ASTUtils.getForeignNode(indexedClass);
                if (node != null) {
                    OffsetRange defRange = null;
                    try {
                        defRange = ASTUtils.getRange(node, (BaseDocument) indexedClass.getDocument());
                    } catch (IOException ex) {
//                        LOG.log(Level.FINEST, "IOException while getting destination range : {0}", ex.getMessage()); // NOI18N
                    }
                    if (defRange != null) {
//                        LOG.log(Level.FINEST, "Found decl. for : {0}", text); // NOI18N
//                        LOG.log(Level.FINEST, "Foreign Node    : {0}", node); // NOI18N
//                        LOG.log(Level.FINEST, "Range start     : {0}", defRange.getStart()); // NOI18N

                        return new DeclarationLocation(indexedClass.getFileObject(), defRange.getStart());
                    }
                }
            }
*/
            // so - we haven't found this class using the groovy index,
            // then we have to search it as a pure java type.

            // simple sanity-check that the literal string in the source document
            // matches the last part of the full-qualified name of the type.
            // e.g. "String" means "java.lang.String"

            FileObject fileObject = info.getSnapshot().getSource().getFileObject();

            if (fileObject != null) {
                final ClasspathInfo cpi = ClasspathInfo.create(fileObject);

                if (cpi != null) {
                    JavaSource javaSource = JavaSource.create(cpi);

                    if (javaSource != null) {
                        CountDownLatch latch = new CountDownLatch(1);
                        SourceLocator locator = new SourceLocator(fqName, cpi, latch);
                        try {
                            javaSource.runUserActionTask(locator, false);
                        } catch (IOException ex) {
//                            LOG.log(Level.FINEST, "Problem in runUserActionTask :  {0}", ex.getMessage());
                            return DeclarationLocation.NONE;
                        }
                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return DeclarationLocation.NONE;
                        }
                        return locator.getLocation();
                    } else {
//                        LOG.log(Level.FINEST, "javaSource == null"); // NOI18N
                    }
                } else {
//                    LOG.log(Level.FINEST, "classpathinfo == null"); // NOI18N
                }
            } else {
//                LOG.log(Level.FINEST, "fileObject == null"); // NOI18N
            }

            return DeclarationLocation.NONE;
        }
        return DeclarationLocation.NONE;
    }

    private String getFqNameForNode(Node node) {
        /*
        if (node instanceof DeclarationExpression) {
            return ((BinaryExpression) node).getLeftExpression().getType().getName();
        } else if (node instanceof Expression) {
            return ((Expression) node).getType().getName();
        } else if (node instanceof PropertyNode) {
            return ((PropertyNode) node).getField().getType().getName();
        } else if (node instanceof FieldNode) {
            return ((FieldNode) node).getType().getName();
        } else if (node instanceof Parameter) {
            return ((Parameter) node).getType().getName();
        }
*/
        return "";
    }

    /**
     * Locates and opens in Editor the Java Element given as Full-Qualified name in fqName.
     */
    private class SourceLocator implements Task<CompilationController> {

        private final String fqName;

        private final ClasspathInfo cpi;

        private final CountDownLatch latch;

        private DeclarationLocation location = DeclarationLocation.NONE;
//        private Object ElementDeclaration;

        public SourceLocator(String fqName, ClasspathInfo cpi, CountDownLatch latch) {
            this.fqName = fqName;
            this.cpi = cpi;
            this.latch = latch;
        }

        public void run(CompilationController info) throws Exception {
            Elements elements = info.getElements();

            if (elements != null) {
                final javax.lang.model.element.TypeElement typeElement = ElementSearch.getClass(elements, fqName);

                if (typeElement != null) {
                    DeclarationLocation found = ElementDeclaration.getDeclarationLocation(cpi, typeElement);
                    synchronized (this) {
                        location = found;
                    }
                } else {
//                    LOG.log(Level.FINEST, "typeElement == null"); // NOI18N
                }
            } else {
//                LOG.log(Level.FINEST, "elements == null"); // NOI18N
            }
            latch.countDown();
        }

        public synchronized DeclarationLocation getLocation() {
            return location;
        }
    }
    private OffsetRange getReferenceSpan(TokenSequence<?> ts, TokenHierarchy<Document> th, int lexOffset) {
        Token<?> token = ts.token();
        MirahTokenId id = (MirahTokenId) token.id();

        if (id.is(Tokens.tIDENTIFIER) ) {
            if (token.length() == 1 && id.is(Tokens.tIDENTIFIER) && token.text().toString().equals(",")) {
                assert false : "Never planned to be here";
                return OffsetRange.NONE;
            }
        }

        // TODO: Tokens.SUPER, Tokens.THIS, Tokens.SELF ...
        if ( id.is(Tokens.tIDENTIFIER) ) {
            return new OffsetRange(ts.offset(), ts.offset() + token.length());
        }

        return OffsetRange.NONE;
    }

    private DeclarationLocation getClassDeclaration(MirahParser.NBMirahParserResult info, Set<IndexedClass> classes,
            AstPath path, Node closest, MirahIndex index, BaseDocument doc) {
        final IndexedClass candidate =
            findBestClassMatch(classes, path, closest, index);

        if (candidate != null) {
            IndexedElement com = candidate;
            Node node = ASTUtils.getForeignNode(com);

            DeclarationLocation loc = new DeclarationLocation(com.getFileObject(),
                ASTUtils.getOffset(doc, node.position().startLine(), node.position().startColumn()), com);

            return loc;
        }

        return DeclarationLocation.NONE;
    }

    private DeclarationLocation findMethod(String name, String possibleFqn, String type, Call call,
        MirahParser.NBMirahParserResult info, int caretOffset, int lexOffset, AstPath path, Node closest, MirahIndex index) {
        Set<IndexedMethod> methods = getApplicableMethods(name, possibleFqn, type, call, index);

        int astOffset = caretOffset;
        DeclarationLocation l = getMethodDeclaration(info, name, methods,
             path, closest, index, astOffset, lexOffset);

        if ( l == DeclarationLocation.NONE )
        {
            FileObject fo = info.getSnapshot().getSource().getFileObject();
            if (fo != null) {
                ClasspathInfo cpi = ClasspathInfo.create(fo);
                l = findJavaMethod(cpi,possibleFqn, name);
            }
        }
        return l;
    }

    private Set<IndexedMethod> getApplicableMethods(String name, String possibleFqn,
            String type, Call call, MirahIndex index) {
//        Set<IndexedMethod> methods = new HashSet<IndexedMethod>();
        Set<IndexedMethod> methods = index.getMethods(name,possibleFqn,QuerySupport.Kind.EXACT);
        String fqn = possibleFqn;
        /*
        if (type == null && possibleFqn != null && call.getLhs() == null && call != Call.UNKNOWN) {
            fqn = possibleFqn;

            // methods directly from fqn class
            if (methods.isEmpty()) {
                methods = index.getMethods(name, fqn, QuerySupport.Kind.EXACT);
            }

            methods = index.getInheritedMethods(fqn, name, QuerySupport.Kind.EXACT);
        }
*/
        if (type != null && methods.isEmpty()) {
            fqn = possibleFqn;

            if (methods.isEmpty()) {
                methods = index.getInheritedMethods(fqn + "." + type, name, QuerySupport.Kind.EXACT);
            }

            if (methods.isEmpty()) {
                // Add methods in the class (without an FQN)
                methods = index.getInheritedMethods(type, name, QuerySupport.Kind.EXACT);

                if (methods.isEmpty() && type.indexOf(".") == -1) {
                    // Perhaps we specified a class without its FQN, such as "TableDefinition"
                    // -- go and look for the full FQN and add in all the matches from there
                    Set<IndexedClass> classes = index.getClasses(type, QuerySupport.Kind.EXACT);
                    Set<String> fqns = new HashSet<String>();
                    for (IndexedClass cls : classes) {
                        String f = cls.getFqn();
                        if (f != null) {
                            fqns.add(f);
                        }
                    }
                    for (String f : fqns) {
                        if (!f.equals(type)) {
                            methods.addAll(index.getInheritedMethods(f, name, QuerySupport.Kind.EXACT));
                        }
                    }
                }
            }

            // Fall back to ALL methods across classes
            // Try looking at the libraries too
            if (methods.isEmpty()) {
                fqn = possibleFqn;
                while ((methods.isEmpty()) && fqn != null && (fqn.length() > 0)) {
                    methods = index.getMethods(name, fqn + "." + type, QuerySupport.Kind.EXACT);

                    int f = fqn.lastIndexOf(".");

                    if (f == -1) {
                        break;
                    } else {
                        fqn = fqn.substring(0, f);
                    }
                }
            }
        }

        if (methods.isEmpty()) {
            methods = index.getMethods(name, type, QuerySupport.Kind.EXACT);
            if (methods.isEmpty() && type != null) {
                methods = index.getMethods(name, null, QuerySupport.Kind.EXACT);
            }
        }

        return methods;
    }

    private DeclarationLocation getMethodDeclaration(MirahParser.NBMirahParserResult info, String name, Set<IndexedMethod> methods,
            AstPath path, Node closest, MirahIndex index, int astOffset, int lexOffset) {
        BaseDocument doc = LexUtilities.getDocument(info, false);
        if (doc == null) {
            return DeclarationLocation.NONE;
        }

        IndexedMethod candidate =
            findBestMethodMatch(name, methods, doc,
                astOffset, lexOffset, path, closest, index);

        if (candidate != null) {
            FileObject fileObject = candidate.getFileObject();
            if (fileObject == null) {
                return DeclarationLocation.NONE;
            }

            /*
            Node node = ASTUtils.getForeignNode(candidate);
            // negative line/column can happen due to bugs in groovy parser
            int nodeOffset = (node != null && node.position().startLine() > 0 && node.position().startColumn() > 0)
                    ? ASTUtils.getOffset(doc, node.position().startLine(), node.position().startColumn())
                    : 0;
            */
            int nodeOffset = candidate.getOffset();
            
            DeclarationLocation loc = new DeclarationLocation(
                fileObject, nodeOffset, candidate);

            return loc;
        }

        return DeclarationLocation.NONE;
    }

    IndexedClass findBestClassMatch(Set<IndexedClass> classSet,
        AstPath path, Node reference, MirahIndex index) {
        // Make sure that the best fit method actually has a corresponding valid source location
        // and parse tree
        Set<IndexedClass> classes = new HashSet<IndexedClass>(classSet);

        while (!classes.isEmpty()) {
            IndexedClass clz = findBestClassMatchHelper(classes, path, reference, index);
            if (clz == null) {
                return null;
            }
            Node node = ASTUtils.getForeignNode(clz);

            if (node != null) {
                return clz;
            }

            // TODO: Sort results, then pick candidate number modulo methodSelector
            if (!classes.contains(clz)) {
                // Avoid infinite loop when we somehow don't find the node for
                // the best class and we keep trying it
                classes.remove(classes.iterator().next());
            } else {
                classes.remove(clz);
            }
        }

        return null;
    }

    private IndexedClass findBestClassMatchHelper(Set<IndexedClass> classes,
        AstPath path, Node reference, MirahIndex index) {
        return null;
    }

    IndexedMethod findBestMethodMatch(String name, Set<IndexedMethod> methodSet,
        BaseDocument doc, int astOffset, int lexOffset, AstPath path, Node call, MirahIndex index) {
        // Make sure that the best fit method actually has a corresponding valid source location
        // and parse tree

        Set<IndexedMethod> methods = new HashSet<IndexedMethod>(methodSet);

        while (!methods.isEmpty()) {
            IndexedMethod method =
                findBestMethodMatchHelper(name, methods, doc, astOffset, lexOffset, path, call, index);
            Node node = method == null ? null : ASTUtils.getForeignNode(method);

            if (node != null) {
                return method;
            }

            if (!methods.contains(method)) {
                // Avoid infinite loop when we somehow don't find the node for
                // the best method and we keep trying it
                methods.remove(methods.iterator().next());
            } else {
                methods.remove(method);
            }
        }

        // Dynamic methods that don't have source (such as the TableDefinition methods "binary", "boolean", etc.
        if (methodSet.size() > 0) {
            return methodSet.iterator().next();
        }

        return null;
    }

    private IndexedMethod findBestMethodMatchHelper(String name, Set<IndexedMethod> methods,
        BaseDocument doc, int astOffset, int lexOffset, AstPath path, Node callNode, MirahIndex index) {

        Set<IndexedMethod> candidates = new HashSet<IndexedMethod>();

        if(path == null) {
            return null;
        }

        Node parent = path.leafParent();
/*
        if (callNode instanceof ConstantExpression && parent instanceof MethodCallExpression) {

            String fqn = null;

            MethodCallExpression methodCall = (MethodCallExpression) parent;
            Expression objectExpression = methodCall.getObjectExpression();
            if (objectExpression instanceof VariableExpression) {
                VariableExpression variable = (VariableExpression) objectExpression;
                if ("this".equals(variable.getName())) { // NOI18N
                    fqn = ASTUtils.getFqnName(path);
                } else {
                    fqn = variable.getType().getName();
                }
            }
            if (fqn != null) {
                for (IndexedMethod method : methods) {
                    if (fqn.equals(method.getIn())) {
                        candidates.add(method);
                    }
                }
            }
        }
*/
        if (candidates.size() == 1) {
            return candidates.iterator().next();
        } else if (!candidates.isEmpty()) {
            methods = candidates;
        }

        return null;
    }

//    private DeclarationLocation fix(DeclarationLocation location, CompilationInfo info) {
//        if ((location != DeclarationLocation.NONE) && (location.getFileObject() == null) &&
//                (location.getUrl() == null)) {
//            return new DeclarationLocation(info.getFileObject(), location.getOffset(), location.getElement());
//        }
//
//        return location;
//    }

    private static DeclarationLocation findJavaField(ClasspathInfo cpInfo, final String fqn, final String fieldName) {
        final ElementHandle[] handles = new ElementHandle[1];
        final int[] offset = new int[1];
        JavaSource javaSource = JavaSource.create(cpInfo);
        try {
            javaSource.runUserActionTask(new Task<CompilationController>() {
                @Override
                public void run(CompilationController controller) throws Exception {
                    controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    TypeElement typeElement = ElementSearch.getClass(controller.getElements(), fqn);
                    if (typeElement != null) {
                        /*
                        for (VariableElement variable : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                            if (variable.getSimpleName().contentEquals(fieldName)) {
                                handles[0] = ElementHandle.create(variable);
                            }
                        }
                        */
                    }
                }
            }, true);
            if (handles[0] != null) {
                FileObject fileObject = SourceUtils.getFile(handles[0], cpInfo);
                if (fileObject != null) {
                    javaSource = JavaSource.forFileObject(fileObject);
                    javaSource.runUserActionTask(new Task<CompilationController>() {
                        @Override
                        public void run(CompilationController controller) throws Exception {
                            controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                            Element element = handles[0].resolve(controller);
                            Trees trees = controller.getTrees();
                            Tree tree = trees.getTree(element);
                            SourcePositions sourcePositions = trees.getSourcePositions();
                            offset[0] = (int) sourcePositions.getStartPosition(controller.getCompilationUnit(), tree);
                        }
                    }, true);
                    return new DeclarationLocation(fileObject, offset[0]);
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return DeclarationLocation.NONE;
    }

    //todo - need check signature
    private static DeclarationLocation findJavaMethod(ClasspathInfo cpInfo, final String fqn, final String methodName ) {
        final ElementHandle[] handles = new ElementHandle[1];
        final int[] offset = new int[1];
        JavaSource javaSource = JavaSource.create(cpInfo);
        try {
            javaSource.runUserActionTask(new Task<CompilationController>() {
                @Override
                public void run(CompilationController controller) throws Exception {
                    controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    TypeElement typeElement = ElementSearch.getClass(controller.getElements(), fqn);
                    if (typeElement != null) {
                        for (ExecutableElement javaMethod : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
//                            if (Methods.isSameMethod(javaMethod, methodCall)) {
                            Name n = javaMethod.getSimpleName();
                            String simpleName = n.toString();
                            if ( javaMethod.getSimpleName().equals(methodName)) {
                                handles[0] = ElementHandle.create(javaMethod);
                            }
                        }
                    }
                }
            }, true);
            if (handles[0] != null) {
                FileObject fileObject = SourceUtils.getFile(handles[0], cpInfo);
                if (fileObject != null) {
                    javaSource = JavaSource.forFileObject(fileObject);
                    if (javaSource != null) {
                        javaSource.runUserActionTask(new Task<CompilationController>() {
                            @Override
                            public void run(CompilationController controller) throws Exception {
                                controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                                Element element = handles[0].resolve(controller);
                                Trees trees = controller.getTrees();
                                Tree tree = trees.getTree(element);
                                SourcePositions sourcePositions = trees.getSourcePositions();
                                offset[0] = (int) sourcePositions.getStartPosition(controller.getCompilationUnit(), tree);
                            }
                        }, true);
                    }
                    return new DeclarationLocation(fileObject, offset[0]);
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return DeclarationLocation.NONE;
    }
}

/*

    @Override
    public DeclarationLocation findDeclaration(ParserResult info, int lexOffset) {
        final MirahParser.NBMirahParserResult parserResult = ASTUtils.getParseResult(info);
        final Document document = LexUtilities.getDocument(parserResult, false);
        if (document == null) {
            return DeclarationLocation.NONE;
        }

        final TokenHierarchy<Document> th = TokenHierarchy.get(document);
        final BaseDocument doc = (BaseDocument) document;
        final int astOffset = ASTUtils.getAstOffset(info, lexOffset);
        if (astOffset == -1) {
            return DeclarationLocation.NONE;
        }

        final OffsetRange range = getReferenceSpan(doc, lexOffset);
        if (range == OffsetRange.NONE) {
            return DeclarationLocation.NONE;
        }

        final Node root = ASTUtils.getRoot(info);

        try {
            // FIXME parsing API - source & binary IDs
            MirahIndex index = MirahIndex.get(QuerySupport.findRoots(info.getSnapshot().getSource().getFileObject(),
                    Collections.singleton(ClassPath.SOURCE), null, null));

            if (root == null) {
                // No parse tree - try to just use the syntax info to do a simple index lookup
                // for methods and classes
                String text = doc.getText(range.getStart(), range.getLength());

                if ((index == null) || (text.length() == 0)) {
                    return DeclarationLocation.NONE;
                }

                if (Character.isUpperCase(text.charAt(0))) {
                    // A Class or Constant?
                    Set<IndexedClass> classes = index.getClasses(text, QuerySupport.Kind.EXACT);

                    if (classes.isEmpty()) {
                        return DeclarationLocation.NONE;
                    }

                    DeclarationLocation l = getClassDeclaration(parserResult, classes, null, null, index, doc);
                    if (l != null) {
                        return l;
                    }
                } else {
                    // A method?
                    Set<IndexedMethod> methods =
                        index.getMethods(text, null, QuerySupport.Kind.EXACT);

                    DeclarationLocation l = getMethodDeclaration(parserResult, text, methods,
                         null, null, index, astOffset, lexOffset);

                    if (l != null) {
                        return l;
                    }
                } // TODO: @ - field?

                return DeclarationLocation.NONE;
            }

            int tokenOffset = lexOffset;

            // Determine the bias (if the caret is between two tokens, did we
            // click on a link for the left or the right?
            final boolean leftSide = range.getEnd() <= lexOffset;
            if (leftSide && (tokenOffset > 0)) {
                tokenOffset--;
            }

            AstPath path = new AstPath(root, astOffset, doc);
            Node closest = path.leaf();
            Node parent = path.leafParent();

            if (closest instanceof ConstantExpression && parent instanceof MethodCallExpression) {

                String name = ((ConstantExpression) closest).getText();

                Call call = Call.getCallType(doc, th, lexOffset);

                String type = call.getType();
                String lhs = call.getLhs();

                MethodCallExpression methodCall = (MethodCallExpression) parent;
                Expression objectExpression = methodCall.getObjectExpression();
                if (objectExpression instanceof ClassExpression || objectExpression instanceof VariableExpression) {
                    // TODO this and super magic ?
                    
                    String typeName = objectExpression.getType().getName();

                    // try to find it in Java
                    FileObject fo = parserResult.getSnapshot().getSource().getFileObject();
                    if (fo != null) {
                        final ClasspathInfo cpInfo = ClasspathInfo.create(fo);
                        DeclarationLocation location = findJavaMethod(cpInfo, typeName, methodCall);
                        if (location != DeclarationLocation.NONE) {
                            return location;
                        }
                    }
                }

                if ((type == null) && (lhs != null) && (closest != null) && call.isSimpleIdentifier()) {
                    assert root instanceof ModuleNode;
                    ModuleNode moduleNode = (ModuleNode) root;
                    VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(moduleNode.getContext(), path, doc, lexOffset);
                    scopeVisitor.collect();
                }
                if (type == null || call.isStatic()) {
                    String fqn = ASTUtils.getFqnName(path);
                    if (call == Call.LOCAL && fqn != null && fqn.length() == 0) {
                        fqn = "java.lang.Object"; // NOI18N
                    }

                    return findMethod(name, fqn, type, call, parserResult, astOffset, lexOffset, path, closest, index);
                }

            } else if (closest instanceof VariableExpression) {
                VariableExpression variableExpression = (VariableExpression) closest;
                Node scope = ASTUtils.getScope(path, variableExpression);
                if (scope != null) {
                    Node variable = ASTUtils.getVariable(scope, variableExpression.getName(), path, doc, lexOffset);
                    if (variable != null) {
                        // I am using getRange and not getOffset, because getRange is adding 'def_' to offset of field
                        int offset = ASTUtils.getRange(variable, doc).getStart();
                        // FIXME parsing API
                        return new DeclarationLocation(info.getSnapshot().getSource().getFileObject(), offset);
                    }
                }
            // find a field ?
            } else if (closest instanceof ConstantExpression && parent instanceof PropertyExpression) {
                PropertyExpression propertyExpression = (PropertyExpression) parent;
                Expression objectExpression = propertyExpression.getObjectExpression();
                Expression property = propertyExpression.getProperty();
                // VariableExpression - normal field access
                // ClassExpression - static field access
                if ((objectExpression instanceof ClassExpression || objectExpression instanceof VariableExpression)
                        && property instanceof ConstantExpression) {

                    if (objectExpression instanceof VariableExpression && "this".equals(((VariableExpression) objectExpression).getName())) { // NOI18N
                        Node scope = ASTUtils.getOwningClass(path);
                        if (scope == null) {
                            // we are in script?
                            scope = (ModuleNode) path.root();
                        }
                        Node variable = ASTUtils.getVariable(scope, ((ConstantExpression) property).getText(), path, doc, lexOffset);
                        if (variable != null) {
                            int offset = ASTUtils.getOffset(doc, variable.getLineNumber(), variable.getColumnNumber());
                            // FIXME parsing API
                            return new DeclarationLocation(info.getSnapshot().getSource().getFileObject(), offset);
                        }
                    } else {
                        // find variable type
                        String typeName = objectExpression.getType().getName();
                        String fieldName = ((ConstantExpression) closest).getText();

                        // try to find it in Java
                        FileObject fo = parserResult.getSnapshot().getSource().getFileObject();
                        if (fo != null) {
                            final ClasspathInfo cpInfo = ClasspathInfo.create(fo);
                            DeclarationLocation location = findJavaField(cpInfo, typeName, fieldName);
                            if (location != DeclarationLocation.NONE) {
                                return location;
                            }
                        }

                        // TODO try to find it in Groovy
                    }
                }
            } else if (closest instanceof DeclarationExpression
                || closest instanceof ConstructorCallExpression
                || closest instanceof ClassExpression
                || closest instanceof PropertyNode
                || closest instanceof FieldNode
                || closest instanceof Parameter) {

                String fqName = getFqNameForNode(closest);
                return findType(fqName, range, doc, info, index);

            // we are in class node, for example
            // Myclass extends OtherClass implements MyInterface
            } else if (closest instanceof ClassNode) {
                // move to first keyword by tokens and figure out extends, implements
                ClassNode node = (ClassNode) closest;

                TokenSequence<MirahTokenId> ts = LexUtilities.getGroovyTokenSequence(doc, lexOffset);
                if (ts == null) {
                    return DeclarationLocation.NONE;
                }

                ts.move(lexOffset);
                while (ts.isValid() && ts.movePrevious()
                        && ts.token().id() != MirahTokenId.LITERAL_class
                        && ts.token().id() != MirahTokenId.LITERAL_extends
                        && ts.token().id() != MirahTokenId.LITERAL_implements);

                if (!ts.isValid() || ts.token().id() == MirahTokenId.LITERAL_class) {
                    return DeclarationLocation.NONE;
                }

                String fqName = null;
                String text = doc.getText(range.getStart(), range.getLength());
                if (ts.token().id() == MirahTokenId.LITERAL_extends) {
                    ClassNode superClass = node.getSuperClass();
                    // perhaps this check is redundant
                    if (superClass.getName().endsWith(text)) {
                        fqName = superClass.getName();
                    }
                } else if (ts.token().id() == MirahTokenId.LITERAL_implements) {
                    for (ClassNode ifNode : node.getInterfaces()) {
                        // TODO this check won't work for interface with same name
                        // but from different packages
                        if (ifNode.getName().endsWith(text)) {
                            fqName = ifNode.getName();
                            break;
                        }
                    }
                }
                if (fqName != null) {
                    return findType(fqName, range, doc, info, index);
                }
                return DeclarationLocation.NONE;
            }
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
        }
        return DeclarationLocation.NONE;
    }
*/
