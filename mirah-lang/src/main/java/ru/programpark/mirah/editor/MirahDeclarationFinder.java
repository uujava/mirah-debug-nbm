/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.programpark.mirah.editor;

import ru.programpark.mirah.lexer.MirahLanguageHierarchy;
import ru.programpark.mirah.lexer.MirahTokenId;
import ru.programpark.mirah.lexer.MirahParserResult;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import mirah.impl.Tokens;
import mirah.lang.ast.*;
import mirah.lang.ast.Package;
import org.mirah.typer.MethodType;
import org.mirah.typer.ProxyNode;
import org.mirah.typer.ResolvedType;
import org.mirah.typer.TypeFuture;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.*;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.ast.AstSupport;
import ru.programpark.mirah.editor.java.ElementDeclaration;
import ru.programpark.mirah.editor.java.ElementSearch;
import ru.programpark.mirah.editor.utils.LexUtilities;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;
import ru.programpark.mirah.index.elements.IndexedElement;
import ru.programpark.mirah.index.elements.IndexedMethod;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.swing.text.Document;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

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
            @Override
            public boolean enterPackage(Package node, Object arg) {
                packages[0] = node.name().identifier();
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
    public DeclarationLocation checkImportClasses( MirahParserResult parsed, SimpleString ss, MirahIndex index )
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
//                return findType(imp, OffsetRange.NONE, doc, parsed, MirahIndex.get(fo));
                return findType(imp, OffsetRange.NONE, doc, parsed, index);
            }
        }
        return DeclarationLocation.NONE;
    }

    public String findClassFqn( MirahParserResult parsed, String className )
    {
        Node root = parsed.getRoot();
        if ( root != null )
        {
            LinkedList<String> imports = AstSupport.collectImports(root);
            for( String imp : imports )
            if ( imp.endsWith(className) && imp.charAt(imp.length() - className.length() - 1) == '.' )
            {    
                FileObject fo = parsed.getSnapshot().getSource().getFileObject();
                BaseDocument doc = (BaseDocument)parsed.getSnapshot().getSource().getDocument(false);
                return imp;
            }
        }
        return null;
    }
   
    public ArrayList<String> findAsteriskClassFqns( MirahParserResult parsed, String className )
    {
        ArrayList<String> fqns = new ArrayList<String>();
        Node root = parsed.getRoot();
        if ( root != null )
        {
            LinkedList<String> imports = AstSupport.collectAsteriskImports(root);
            for( String imp : imports )
            {    
                fqns.add(imp+"."+className);
            }
        }
        return fqns.size() == 0 ? null : fqns;
    }
    
    // это вызов метода
    //todo - добавить проверку имени класса и дерева наследования
    //todo - добавить проверку сигнатуры!!!
    //todo - добавить обработку конструкторов
    private DeclarationLocation processCall( Call call, ClassDefinition classDef, MirahParserResult parsed, int caretOffset, AstPath path, MirahIndex index ) // Р’С‹Р·РѕРІ РјРµС‚РѕРґР°?
    {
        ResolvedType type = parsed.getResolvedType(call);
        String methodName = call.name().identifier();
        String returnType = type == null ? null : type.name();
        String thisClassName = classDef.name().identifier();
        Node target = call.target();
        if ( target instanceof Self ) {
            
        }
        
        ArrayList<String> parameterTypes = new ArrayList<String>();
        NodeList parameters = call.parameters();
        for (Iterator<Node> it = parameters.iterator(); it.hasNext();) {
            Node pnode = (Node) it.next();
            ResolvedType rtype = parsed.getResolvedType(pnode);
            String ptype = rtype == null ? null : rtype.name();
            parameterTypes.add(ptype);
        }
        /*
        String superFqn = null;
        if ( classDef.superclass() != null )
        {
//            superClassName = classDef.superclass().typeref().name();
            ResolvedType stype = parsed.getResolvedType(classDef);
            if ( stype != null ) superFqn = stype.name();
        }
        */
        ResolvedType ttype = parsed.getResolvedType(target);
        String fqn = (ttype != null) ? ttype.name() : null;
        DeclarationLocation location = null;
        // цикл по суперклассам
        while( true ) {
            location = findMethod(methodName, fqn, returnType, parameterTypes, parsed, index);
            if ( location != null && location != DeclarationLocation.NONE ) break;
           
            // пытаюсь найти в суперклассе
            fqn = index.findSuperClassByFqn(fqn);
            if ( fqn == null || fqn.isEmpty() ) break;
        }
        return location == null ? DeclarationLocation.NONE : location;
    }

    private DeclarationLocation processFunctionalCall( FunctionalCall call, ClassDefinition classDef, MirahParserResult parsed, int caretOffset, AstPath path, MirahIndex index ) // Р’С‹Р·РѕРІ РјРµС‚РѕРґР°?
    {
        ResolvedType type = parsed.getResolvedType(call);
        String methodName = call.name().identifier();
        String returnType = type == null ? null : type.name();
        String thisClassName = classDef.name().identifier();
        Node target = call.target();
        NodeList parameters = call.parameters();
//        while( parameters.iterator().hasNext() )
//        {
//        }

        ResolvedType classType = parsed.getResolvedType(classDef);
        String fqn = classType == null ? null : classType.name();
        
        if ( target instanceof Self ) {
            
        }
        
        ArrayList<String> parameterTypes = new ArrayList<String>();
        for (Iterator<Node> it = parameters.iterator(); it.hasNext();) {
            Node pnode = (Node) it.next();
            ResolvedType rtype = parsed.getResolvedType(pnode);
            if ( rtype == null ) { // не найден тип для mapping @button
                if ( pnode instanceof ProxyNode )
                    rtype = parsed.getResolvedType(((ProxyNode)pnode).get(0));
            }
            String ptype = rtype == null ? null : rtype.name();
            parameterTypes.add(ptype);
        }
//        TypeRef typeRef = call.typeref();
        String superClassName = null;
        if ( classDef.superclass() != null )
        {
            superClassName = classDef.superclass().typeref().name();
        }
//        DeclarationLocation location = findMethod(methodName, fqn, returnType, parameterTypes, parsed, index);
        DeclarationLocation location = null;
        // цикл по суперклассам
        while (true) {
            location = findMethod(methodName, fqn, returnType, parameterTypes, parsed, index);
            if (location != null && location != DeclarationLocation.NONE) {
                break;
            }

            // пытаюсь найти в суперклассе
            fqn = index.findSuperClassByFqn(fqn);
            if (fqn == null || fqn.isEmpty()) {
                break;
            }
        }
        return location == null ? DeclarationLocation.NONE : location;
    }

    //todo - если в конце *, пропустить
    private DeclarationLocation processImport( Import imp, BaseDocument doc, MirahParserResult info, MirahIndex index )
    {
        String fqName = imp.fullName().identifier();
        if ( fqName.endsWith(".*") ) return DeclarationLocation.NONE;
        return findType(fqName, OffsetRange.NONE, doc, info, index);
    }
    
    // ссылка на имя класса
    private DeclarationLocation processRef( TypeRefImpl ref, BaseDocument doc, MirahParserResult parsed, MirahIndex index )
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
    
    DeclarationLocation tryToFindClass( BaseDocument bdoc, MirahParserResult parsed, FileObject fo, String packg, String className, MirahIndex index )
    {
        DeclarationLocation location = null;
    
        // ищу полное имя класса в списке импорта
        String fqn = findClassFqn(parsed, className);
        if (fqn != null) {
            location = findType(fqn, OffsetRange.NONE, bdoc, parsed, index);
//            location = findType(fqn, OffsetRange.NONE, bdoc, parsed, MirahIndex.get(fo));
            if (location != DeclarationLocation.NONE) return location;
        }
        // ищу в текущем пакете
        fqn = packg + "." + className;
//        location = findType(fqn, OffsetRange.NONE, bdoc, parsed, MirahIndex.get(fo));
        location = findType(fqn, OffsetRange.NONE, bdoc, parsed, index);
        if (location != DeclarationLocation.NONE) return location;

        // ищу среди импортированных пакетов
        ArrayList<String> fqns = findAsteriskClassFqns(parsed, className);
        if ( fqns != null ) {
            for (String fqnn : fqns) {
                location = findType(fqnn, OffsetRange.NONE, bdoc, parsed, index);
//                location = findType(fqnn, OffsetRange.NONE, bdoc, parsed, MirahIndex.get(fo));
                if (location != DeclarationLocation.NONE) return location;
            }
        }
        return null;
    }

    
    @Override
    public DeclarationLocation findDeclaration(ParserResult info, int caretOffset) 
    {
        MirahParserResult parsed = (MirahParserResult)info;
        FileObject fo = info.getSnapshot().getSource().getFileObject();
        BaseDocument bdoc = (BaseDocument)info.getSnapshot().getSource().getDocument(false);
//        AstPath path = ASTUtils.getPath(info, (BaseDocument)doc, caretOffset);
        
        String packg = getCurrentPackage(parsed.getRoot());
        
//        LinkedList<String> includes = AstSupport.collectImports(parsed.getRoot());
        
        MirahIndex index = MirahIndex.get(fo);
        
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

        //todo сделать переходы по классам в списке параметров функций. нет подсветки как гиперссылок
        String name = null;
        node = leaf;
        while (node != null) {

            if (node instanceof ProxyNode) {
                ProxyNode proxy = (ProxyNode)node;
                TypeFuture tf = proxy.inferChildren(true);
                ResolvedType resolved = tf.resolve();
                if ( resolved != null && resolved instanceof MethodType )
                {
                    ResolvedType type = ((MethodType)resolved).returnType();
                    if (type != null) {
                        return findType(type.name(), OffsetRange.NONE, bdoc, info, index);
                    }
                }
            }
        
            if (node instanceof Import) {
//                return processImport((Import) node, bdoc, parsed, MirahIndex.get(fo));
                return processImport((Import) node, bdoc, parsed, index);
            }

            if (node instanceof Call) {
                return processCall((Call) node, classDef, parsed, caretOffset, null, index);
//                return processCall((Call) node, classDef, parsed, caretOffset, null, MirahIndex.get(fo));
            }

            if (node instanceof FunctionalCall) {
//                return processFunctionalCall((FunctionalCall) node, classDef, parsed, caretOffset, null, MirahIndex.get(fo));
                return processFunctionalCall((FunctionalCall) node, classDef, parsed, caretOffset, null, index);
            }

            if (node instanceof SimpleString) {
                name = ((SimpleString) node).identifier();
            }
            
            if ( node instanceof Super )
            {
                //todo - переход к методу суперкласса
                //todo - анализ сигнатуры
                if (methodDef == null ) return DeclarationLocation.NONE;
                
                ArrayList<String> parameterTypes = new ArrayList<String>();
                RequiredArgumentList args = methodDef.arguments().required();
                for (int i = 0; i < args.size(); i++) {
                    RequiredArgument argument = args.get(i);
                    TypeName typeName = argument.type();
                    parameterTypes.add(typeName.typeref().name());
                }
                OptionalArgumentList opts = methodDef.arguments().optional();
                for (int i = 0; i < opts.size(); i++) {
                    OptionalArgument argument = opts.get(i);
                    TypeName typeName = argument.type();
                    parameterTypes.add(typeName.typeref().name());
                }
                ResolvedType type = parsed.getResolvedType(node);
                String fqn = (type != null) ? type.name() : null;
                return findMethod(methodDef.name().identifier(), fqn, methodDef.type().typeref().name(), parameterTypes, parsed, index);
//                return findMethod(methodDef.name().identifier(), fqn, methodDef.type().typeref().name(), parameterTypes, parsed, MirahIndex.get(fo));
            }
            
            // это, скорее всего,                                название класса в операторе приведения типов
            if (node instanceof TypeRefImpl) {
                return processRef((TypeRefImpl) node, bdoc, parsed, index);
//                return processRef((TypeRefImpl) node, bdoc, parsed, MirahIndex.get(fo));
            }
            
            if ( node instanceof Constant )
            {
                Constant cnst = (Constant)node;
//                cnst.typeref()
                ResolvedType type = parsed.getResolvedTypes().get(node);
                if ( type == null && node.parent() != null )
                {
                    // проверяю ссылку на суперкласс
                    if ( node.parent() instanceof ClassDefinition ) { //&& ((ClassDefinition) node.parent())..s) {
                       TypeName typeName = ((ClassDefinition) node.parent()).superclass();
                       if ( typeName == node ) {
                           DeclarationLocation location = tryToFindClass(bdoc,parsed,fo,packg,cnst.identifier(),index);
                           if (location != DeclarationLocation.NONE) return location;
                       }
                    }
                    // список интерфейсов в описании класса
                    if ( node.parent() instanceof TypeNameList && node.parent().parent() != null && node.parent().parent() instanceof ClassDefinition) {
                        DeclarationLocation location = tryToFindClass(bdoc, parsed, fo, packg, cnst.identifier(),index);
                        if (location != DeclarationLocation.NONE) return location;
                    }
                    // это тип аргумента функции
                    /*
                    if (node.parent() instanceof RequiredArgument) {
                        type = parsed.getResolvedTypes().get(node.parent());
                    }
                    // это тип возвращаемого значения
                    if (node.parent() instanceof MethodDefinition) {
                        type = parsed.getResolvedTypes().get(node.parent());
                    }
                    */
                    type = parsed.getResolvedTypes().get(node.parent());
                }
                
                if ( type != null )
                {
//                    boolean b1 = type.isBlock();
//                    boolean b2 = type.isInterface();
//                    boolean b3 = type.isError();

                    String fqn = type.name();
                    
                    // возвращаемое значение для метода
                    if ( type instanceof MethodType )
                    {
                        fqn = ((MethodType)type).returnType().name();
                    }
                    if (!primitivesMap.containsKey(fqn)) {
                        return findType(fqn, OffsetRange.NONE, bdoc, info, index);
//                        return findType(fqn, OffsetRange.NONE, bdoc, info, MirahIndex.get(fo));
                    }
                }
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
//                return findType(fqn, OffsetRange.NONE, bdoc, info, MirahIndex.get(fo));
                return findType(fqn, OffsetRange.NONE, bdoc, info, index);
            }
            node = node.parent();
        }
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
                Token<MirahTokenId> tprev = null;

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
//                    if ( ts.movePrevious() && ts.movePrevious() )
//                    {
//                        tprev = ts.token();
//                    }
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
                    t.id().is(Tokens.tInstVar) ||
                    t.id().ordinal() == MirahLanguageHierarchy.TYPE_HINT ||
                    t.id().ordinal() == MirahLanguageHierarchy.CLASS_DECLARATION) {
                    ret[0] = new OffsetRange(ts.offset(), ts.offset() + t.length());
                }
            }
        });
        return ret[0];
        
    }

    private DeclarationLocation findJavaClass(String fqName, ParserResult info ) 
    {
            FileObject fileObject = info.getSnapshot().getSource().getFileObject();
            if (fileObject == null) return DeclarationLocation.NONE;

            final ClasspathInfo cpi = ClasspathInfo.create(fileObject);
            if (cpi != null) {
                JavaSource javaSource = JavaSource.create(cpi);
                if (javaSource != null) {
                    CountDownLatch latch = new CountDownLatch(1);
                    SourceLocator locator = new SourceLocator(fqName, cpi, latch);
                    try {
                        javaSource.runUserActionTask(locator, true);
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
                }
            }
            return DeclarationLocation.NONE;
    }

    private DeclarationLocation findType(String fqName, 
            OffsetRange range,
            BaseDocument doc, 
            ParserResult info, 
            MirahIndex index) 
    {
//        LOG.log(Level.FINEST, "Looking for type: {0}", fqName); // NOI18N
        if (doc != null && range != null) {
            Set<IndexedClass> classes = index.findClassesByFqn(fqName); //.getClasses(fqName, QuerySupport.Kind.EXACT);
            if ( ! classes.isEmpty() )
            {
                IndexedClass indexedClass = classes.iterator().next();
                return new DeclarationLocation(indexedClass.getFileObject(), indexedClass.getOffset());
            }
            return this.findJavaClass(fqName, info);
        }
        return DeclarationLocation.NONE;
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
                    ElementHandle el = ElementHandle.create(typeElement);
                    FileObject fo = SourceUtils.getFile(el, cpi);
                    fo = SourceUtils.getFile(typeElement, cpi);
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
    
    private OffsetRange getReferenceSpan(TokenSequence<?> ts, TokenHierarchy<Document> th, int lexOffset) 
    {
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

    private DeclarationLocation getClassDeclaration(MirahParserResult info, Set<IndexedClass> classes,
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

    static Class findClass(FileObject fo, String name ) 
    {
        ClassPath[] paths = new ClassPath[]{
            ClassPath.getClassPath(fo, ClassPath.SOURCE),
            ClassPath.getClassPath(fo, ClassPath.EXECUTE),
            ClassPath.getClassPath(fo, ClassPath.COMPILE),
            ClassPath.getClassPath(fo, ClassPath.BOOT),};

        for (int i = 0; i < paths.length; i++) {
            ClassPath cp = paths[i];
            try {
                Class c = cp.getClassLoader(true).loadClass(name);
                if (c != null) return c;
            } catch (ClassNotFoundException ex) {
                int t = 0;
            }
        }
        return null;
    }

    private boolean compareFQNs( String type, String fqn, MirahIndex index ) 
    {
        // цикл по суперклассам
        while (true) {
            if ( type.equals(fqn) ) return true;
            
            // пытаюсь найти в суперклассе
            //todo проверять интерфейсы
            
            fqn = index.findSuperClassByFqn(fqn);
            if (fqn == null || fqn.isEmpty()) {
                break;
            }
        }
        return false;
    }

    private boolean compareFQNs( String type, String fqn, FileObject fo ) 
    {
        // цикл по суперклассам
        while (true) {
            if ( type.equals(fqn) ) return true;
            
            // пытаюсь найти в суперклассе
            //todo проверять интерфейсы
            Class cls = findClass(fo,fqn);
            if ( cls == null ) break;
            fqn = cls.getSuperclass().getName();
        }
        return false;
    }

    
    private IndexedMethod findBestMethodMatch(String methodName, 
            Set<IndexedMethod> methodSet,
            String possibleFqn,
            String returnType,
            ArrayList<String> parameterTypes,
            MirahParserResult parsed,
            MirahIndex index)
    {
        // Make sure that the best fit method actually has a corresponding valid source location
        // and parse tree

        Set<IndexedMethod> methods = new HashSet<IndexedMethod>(methodSet);

        // простейший способ проверки сигнатуры метода
        //todo нужно проверять интерфейсы
        StringBuffer sb = new StringBuffer(methodName);
        sb.append("(");
        if ( parameterTypes != null )
        {
            for( int i = 0 ; i < parameterTypes.size() ; i++ )
            {
                if (i != 0) sb.append(",");
                
                String type = parameterTypes.get(i).toString();
                // замыкания заменяю на суперклассы
                if (type.indexOf("$Closure") != -1 || type.indexOf("$ZBinding") != -1) {
                    type = index.findSuperClassByFqn(type);
                }
                // теперь в сигнатуре используются квалифицированные имена классов
                //if ( type.lastIndexOf('.') != -1 ) type = type.substring(type.lastIndexOf('.')+1);
                sb.append(type);
            }
        }
        sb.append(")");

        // проверка на точное совпадение сигнатуры
        String signature = sb.toString();
        for( IndexedMethod method : methods )
        {
            if ( method.getSignature().startsWith(signature))
                return method;
        }
        // проверка дерева наследования у параметров методов
        for (IndexedMethod method : methods) {
            String sign = method.getSignature();
            int open = sign.indexOf('(');
            int close = sign.indexOf(')');
            if ( open == -1 || close == -1 ) continue;
            String [] params = sign.substring(open+1,close).split(",");
            if ( params.length != parameterTypes.size() ) continue;
            // сравниваю FQN типов параметров
            int i = 0;
            for( ; i < params.length ; i++ )
            {
                // если не совпадают, пытыюсь проверить
//                if ( ! compareFQNs(params[i],parameterTypes.get(i),index) ) break;
                if ( ! compareFQNs(params[i],parameterTypes.get(i),parsed.getSnapshot().getSource().getFileObject()) ) break;
            }
            if ( i >= params.length ) return method;
        }
        return null;
    }

    private DeclarationLocation findMethod( String methodName, 
            String possibleFqn, 
            String returnType, 
            ArrayList<String> parameterTypes, 
            MirahParserResult parsed,
            MirahIndex index) 
    {
        Set<IndexedMethod> methods = getApplicableMethods(methodName, possibleFqn, returnType, parameterTypes, index);

        IndexedMethod candidate = findBestMethodMatch(methodName, methods, possibleFqn, returnType, parameterTypes, parsed, index);

        if (candidate != null) {
            FileObject fileObject = candidate.getFileObject();
            if (fileObject != null) {
                return new DeclarationLocation( fileObject, candidate.getOffset(), candidate);
            }
        }
        // Если ничего не найдено, попробуем найти этот метод в Java классе
        {
            FileObject fo = parsed.getSnapshot().getSource().getFileObject();
            if (fo != null) {
                ClasspathInfo cpi = ClasspathInfo.create(fo);
                DeclarationLocation location = findJavaMethod(cpi,possibleFqn, methodName);
                if ( location != DeclarationLocation.NONE ) return location;
            }
        }
        return DeclarationLocation.NONE;
    }

    private Set<IndexedMethod> getApplicableMethods(String methodName, String possibleFqn,
            String returnType, ArrayList<String> parameters, MirahIndex index) 
    {
        Set<IndexedMethod> methods = index.getMethods(methodName,possibleFqn,QuerySupport.Kind.EXACT);
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
        if (returnType != null && methods.isEmpty()) {
            fqn = possibleFqn;

            if (methods.isEmpty()) {
                methods = index.getInheritedMethods(fqn + "." + returnType, methodName, QuerySupport.Kind.EXACT);
            }

            if (methods.isEmpty()) {
                // Add methods in the class (without an FQN)
                methods = index.getInheritedMethods(returnType, methodName, QuerySupport.Kind.EXACT);

                if (methods.isEmpty() && returnType.indexOf(".") == -1) {
                    // Perhaps we specified a class without its FQN, such as "TableDefinition"
                    // -- go and look for the full FQN and add in all the matches from there
                    Set<IndexedClass> classes = index.getClasses(returnType, QuerySupport.Kind.EXACT);
                    Set<String> fqns = new HashSet<String>();
                    for (IndexedClass cls : classes) {
                        String f = cls.getFqn();
                        if (f != null) {
                            fqns.add(f);
                        }
                    }
                    for (String f : fqns) {
                        if (!f.equals(returnType)) {
                            methods.addAll(index.getInheritedMethods(f, methodName, QuerySupport.Kind.EXACT));
                        }
                    }
                }
            }

            // Fall back to ALL methods across classes
            // Try looking at the libraries too
            if (methods.isEmpty()) {
                fqn = possibleFqn;
                while ((methods.isEmpty()) && fqn != null && (fqn.length() > 0)) {
                    methods = index.getMethods(methodName, fqn + "." + returnType, QuerySupport.Kind.EXACT);

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
            methods = index.getMethods(methodName, returnType, QuerySupport.Kind.EXACT);
            if (methods.isEmpty() && returnType != null) {
                methods = index.getMethods(methodName, null, QuerySupport.Kind.EXACT);
            }
        }

        return methods;
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
    //todo - search for superclass
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
                        TypeMirror mirror = typeElement.getSuperclass();
                        TypeKind kind = mirror.getKind();
                        for (ExecutableElement javaMethod : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
//                            if (Methods.isSameMethod(javaMethod, methodCall)) {
                            Name name = javaMethod.getSimpleName();
                            String simpleName = name.toString();
//                            if ( javaMethod.getSimpleName().equals(methodName)) {
                            if (simpleName.equals(methodName)) {
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
