package ru.programpark.mirah.index;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import mirah.lang.ast.Arguments;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.ConstructorDefinition;
import mirah.lang.ast.FieldAssign;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Import;
import mirah.lang.ast.InterfaceDeclaration;
import mirah.lang.ast.MacroDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.ModifierList;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.RequiredArgument;
import mirah.lang.ast.StaticMethodDefinition;
import mirah.lang.ast.Super;
import mirah.lang.ast.TypeName;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexer;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.support.IndexDocument;
import org.netbeans.modules.parsing.spi.indexing.support.IndexingSupport;
import mirah.objectweb.asm.tree.ClassNode;
import org.mirah.typer.MethodType;
import org.mirah.typer.ResolvedType;

public class MirahIndexer extends EmbeddingIndexer {

    public static final String FQN_NAME = "fqn"; // class
    public static final String CLASS_NAME = "class";
    public static final String CASE_INSENSITIVE_CLASS_NAME = "class-ig";
    public static final String IN = "in"; // not indexed
    /** Attributes: hh;nnnn where hh is a hex representing flags in IndexedClass, and nnnn is the documentation length */
    public static final String CLASS_ATTRS = "attrs";
    public static final String METHOD_NAME = "method";
    public static final String CONSTRUCTOR = "ctor";
    public static final String FIELD_NAME = "field";
    public static final String CLASS_OFFSET = "offset";
    public static final String SUPER_CLASS = "suprclass";
    /** Attributes: "i" -> private, "o" -> protected, ", "s" - static/notinstance, "d" - documented */
    //static final String ATTRIBUTE_NAME = "attribute";

    private static FileObject preindexedDb;

    // some statistics about the indexer

    private static long indexerRunTime = 0;
    private static long indexerFirstRun = 0;
    private static long filesIndexed = 0;

    
    private ClassNode lastFoundClass = null;
    
/*    
    static {
            OpenProjects.getDefault().addPropertyChangeListener(
//                new OpenedProjectChangeListener() { 
//                    public void propertyChanged(OpenProjectChangeEvent evt) { 
//                            Set<Project> openedProjects = evt.getOpenProjects()
//                    }
//                }
   new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
//            LOG.info(MirahIndexer.class, "PropertyChangeEvent = "+evt);
            if (OpenProjects.PROPERTY_OPEN_PROJECTS.equals(evt.getPropertyName())) {
                List<Project> oldOpenProjectsList = Arrays.asList((Project[])evt.getOldValue());
                List<Project> newOpenProjectsList = Arrays.asList((Project[])evt.getNewValue());
                
                Set<Project> closedProjectsSet = new LinkedHashSet<Project>(oldOpenProjectsList);
                closedProjectsSet.removeAll(newOpenProjectsList);
                for (Project project : closedProjectsSet) {
                    // Process closed projects
                LOG.info(MirahIndexer.class, "closedProjectsSet = "+project);
                }
                
                Set<Project> openedProjectsSet = new LinkedHashSet<Project>(newOpenProjectsList);
                openedProjectsSet.removeAll(oldOpenProjectsList);
                for (Project project : openedProjectsSet) {
                    // Process opened projects
                LOG.info(MirahIndexer.class, "openedProjectsSet = "+project);
                }
            }
        }       
    });
            
            
        
    }
 */   
    
    //todo индексирование валится при выходе из IDE
    
//    private static final Logger LOG = Logger.getLogger(MirahIndexer.class.getName());

    @Override
    protected void index(Indexable indexable, Result parserResult, Context context) {
        long indexerThisStartTime = System.currentTimeMillis();

        try
        {
            if (indexerFirstRun == 0) {
                indexerFirstRun = indexerThisStartTime;
            }
            Node ast = null; 
            if ( parserResult instanceof MirahParser.NBMirahParserResult )
            {
//                ast = ((MirahParser.NBMirahParserResult)parserResult).getRoot();
//            }
//            if ( ast != null )
//            {
                IndexingSupport support;
                try {
                    support = IndexingSupport.getInstance(context);
                } catch (IOException ioe) {
    //                LOG.exception(this, ioe);
                    return;
                }
                IndexScanner scanner = new IndexScanner((MirahParser.NBMirahParserResult)parserResult,support,indexable);
                scanner.analyze();
                for (IndexDocument doc : scanner.getDocuments()) {
                    support.addDocument(doc);
                }
            }
            filesIndexed++;
            long indexerThisStopTime = System.currentTimeMillis();
            long indexerThisRunTime = indexerThisStopTime - indexerThisStartTime;
            indexerRunTime += indexerThisRunTime;

            LOG.info(null, "Indexed File: " + parserResult.getSnapshot().getSource().getFileObject().getNameExt()+" Time = "+indexerThisRunTime+" ms");
            /*
            LOG.info(this, "---------------------------------------------------------------------------------");
            LOG.info(this,"Indexed File                : "+parserResult.getSnapshot().getSource().getFileObject().getPath());
            LOG.info(this,"Indexing time (ms)          : "+indexerThisRunTime);
            LOG.info(this,"Number of files indexed     : "+filesIndexed);
            LOG.info(this,"Time spend indexing (ms)    : "+indexerRunTime);
            LOG.info(this,"Avg indexing time/file (ms) : "+indexerRunTime/filesIndexed);
            LOG.info(this,"Time betw. 1st and Last idx : "+(indexerThisStopTime - indexerFirstRun));
            LOG.info(this, "---------------------------------------------------------------------------------");
            */
        }
        catch( Exception e )
        {
//            LOG.exception(this, e);
            e.printStackTrace();
        }
    }

    public FileObject getPreindexedDb() {
        return preindexedDb;
    }

    public static final class Factory extends EmbeddingIndexerFactory {

        public static final String NAME = "mirah"; // NOI18N
        public static final int VERSION = 8;

        @Override
        public EmbeddingIndexer createIndexer(Indexable indexable, Snapshot snapshot) {
//            LOG.info(MirahIndexer.class,"createIndexer indexable="+indexable);

            if (isIndexable(indexable, snapshot)) {
//                LOG.info(MirahIndexer.class, "return new MirahIndexer()");
                return new MirahIndexer();
            } else {
//                LOG.info(MirahIndexer.class, "return NULL");
                return null;
            }
        }

        @Override
        public int getIndexVersion() {
            return VERSION;
        }

        @Override
        public String getIndexerName() {
            return NAME;
        }

        private boolean isIndexable(Indexable indexable, Snapshot snapshot) {
            String extension = snapshot.getSource().getFileObject().getExt();

//            LOG.info(MirahIndexer.class,"isIndexable extension="+extension);
            if (extension.equals("mirah")) { // NOI18N
                return true;
            }
            return false;
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
//            LOG.info(MirahIndexer.class, "filesDeleted deleted=" + deleted);
            try {
                IndexingSupport support = IndexingSupport.getInstance(context);
                for (Indexable indexable : deleted) {
                    support.removeDocuments(indexable);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void rootsRemoved(final Iterable<? extends URL> removedRoots) {

        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
            try {
                IndexingSupport is = IndexingSupport.getInstance(context);
                for(Indexable i : dirty) {
                    is.markDirtyDocuments(i);
                }
            } catch (IOException ioe) {
//                LOG.log(Level.WARNING, null, ioe);
            }
        }

        @Override
        public boolean scanStarted(Context context) {
            //ClassNodeCache.createThreadLocalInstance();
//            if ( ! context.getRoot().getPath().isEmpty() ) LOG.info(MirahIndexer.class, "scanStarted context=" + context.getRoot().getPath());
            return super.scanStarted(context);
        }

        @Override
        public void scanFinished(Context context) {
            //ClassNodeCache.clearThreadLocalInstance();
//            if ( ! context.getRoot().getPath().isEmpty() ) 
//                LOG.info(MirahIndexer.class, "scanFinished context=" + context.getRoot().getPath());
            super.scanFinished(context);
        }
        
    }
    public class IndexScanner extends NodeScanner
    {

        private final FileObject file;
        private final IndexingSupport support;
        private final Indexable indexable;
        private final List<IndexDocument> documents = new ArrayList<IndexDocument>();
        private final List<IndexDocument> nestedDocuments = new ArrayList<IndexDocument>();
//        private IndexDocument document = null;
        private String packageName = null;
        private ClassNode lastFoundClass = null;
        private final List<String> classNames = new ArrayList<String>();
        private MirahParser.NBMirahParserResult parsed;

        private HashMap fields = new HashMap();

        public IndexScanner( MirahParser.NBMirahParserResult parsed, IndexingSupport support, Indexable indexable )
        {
            this.parsed = parsed;
            this.file = parsed.getSnapshot().getSource().getFileObject();
            this.support = support;
            this.indexable = indexable;
        }

        List<IndexDocument> getDocuments() 
        {
            return documents;
        }
        
        private String getClassName() 
        {
            return classNames.isEmpty() ? null : classNames.get(0);
        }

        private IndexDocument getCurrentDocument() 
        {
            return nestedDocuments.isEmpty() ? null : nestedDocuments.get(0);
        }
        private IndexDocument addDocument() 
        {
            IndexDocument document = support != null ? support.createDocument(indexable) : null;
            if ( document != null ) 
            {
                documents.add(document);
                nestedDocuments.add(0,document);
            }
            return document;
        }
        
        public void analyze()
        {
            Node node = parsed.getRoot();
            if ( node != null ) node.accept(this, null);
        }

        // В индекс записывается строка #{method_name}(#{arguments});#{returned_value};#{modifiers};#{line_number}
        // поиск возможен только по префиксу (QuerySupport.Kind.PREFIX), а не точный QuerySupport.Kind.EXACT
        @Override
        public boolean enterMethodDefinition( MethodDefinition node, Object arg )
        {
            StringBuilder sb = new StringBuilder(); //ASTUtils.getDefSignature(childNode));
            sb.append(node.name().identifier());
    //            sb.append(';').append(org.netbeans.modules.groovy.editor.java.Utilities.translateClassLoaderTypeName(
    //                    childNode.getReturnType().getName()));
            
            if ( "call".equals(node.name().identifier()) )
            {
                int _t = 0;
            }
            if ( "build".equals(node.name().identifier()) )
            {
                int _t = 0;
            }
            prepareArguments(node.arguments(), sb);
            sb.append(';');
//            if ( node.type() != null && node.type().typeref() != null ) sb.append(node.type().typeref().name());
//            sb.append("^" + node.type());
            
            ResolvedType type = this.parsed.getResolvedType(node);
//            sb.append("*"+type);
            if ( type != null && type instanceof MethodType ) {
                sb.append(((MethodType)type).returnType().name());
            }
            else if (node.type() != null && node.type().typeref() != null) {
                sb.append(node.type().typeref().name());
            }
            sb.append(';');
            prepareModifiers(node.modifiers(), sb);
            
            sb.append(';');
            prepareLocation(node, sb);
//            if ( document != null ) document.addPair(METHOD_NAME, sb.toString(), true, true);
            if (getCurrentDocument() != null)
                getCurrentDocument().addPair(METHOD_NAME, sb.toString(), true, true);
            
//      LOG.info(MirahIndexer.class, "enterMethodDefinition name=" + node.name().identifier()+" file="+file.getName()+" node="+node);
//            LOG.info(null, ""+getClassName()+": METHOD_NAME="+sb.toString());
//            if ( node.type() != null && node.type().typeref() != null )
//            LOG.info(MirahIndexer.class, "return="+node.type().typeref().name()+" array="+node.type().typeref().isArray()+" static="+node.type().typeref().isStatic());
            return true;
    //            return enterMethodDefinition(node, arg);
        }
        @Override
        public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg)
        {
            return enterMethodDefinition(node,arg);
        }
        
        @Override
        public boolean enterPackage( mirah.lang.ast.Package node, Object arg )
        {
            if (lastFoundClass != null) {
                return true;
            }
            packageName = node.name().identifier();
    //            LOG.info(MirahIndexer.class, "enterClassDefinition packageName=" + packageName);
    //                ClassFinder scope = new ClassFinder(
    //                        context.get(ca.weblite.asm.ClassLoader.class),
    //                        null
    //                );
    //                if (packageName != null) {
    //                    scope.addImport(packageName + ".*");
    //                }
    //                if (!scopeStack.isEmpty()) {
    //                    scopeStack.pop();
    //                }
    //                scopeStack.push(scope);
            return super.enterPackage(node, arg);
        }

        @Override
        public boolean enterInterfaceDeclaration( InterfaceDeclaration node, Object arg ) 
        {
            return enterClassDefinition(node, arg);
        }

        @Override
        public Object exitInterfaceDeclaration( InterfaceDeclaration node, Object arg ) 
        {
            return exitClassDefinition(node, arg);
        }
       
        @Override
        public boolean enterClosureDefinition(ClosureDefinition node, Object arg ) 
        { 
            return enterClassDefinition(node, arg);
        }

        @Override
        public Object exitClosureDefinition(ClosureDefinition node, Object arg) {
            return exitClassDefinition(node, arg);
        }

        @Override
        public boolean enterClassDefinition( ClassDefinition node, Object arg ) 
        {
            if (lastFoundClass != null) {
                return true;
            }
            String className = "";
            if ( packageName != null ) className = packageName + ".";
            className += node.name().identifier();

//            LOG.info(null, "Add document " + className);
            
            for( Iterator it = node.interfaces().iterator() ; it.hasNext() ; )
            {
                TypeName typeName = (TypeName)it.next();
                int _t = 0;
            }
            fields.clear();
            addDocument();
            if ( getCurrentDocument() != null )
            {
                getCurrentDocument().addPair(FQN_NAME, className, true, true);
                getCurrentDocument().addPair(CLASS_NAME, node.name().identifier(), true, true);
                getCurrentDocument().addPair(CLASS_OFFSET, ""+node.position().startChar(), true, true);
                getCurrentDocument().addPair(CASE_INSENSITIVE_CLASS_NAME, node.name().identifier().toLowerCase(), true, true);
                if ( node instanceof ClosureDefinition ) {
                    TypeName typeName = node.superclass();
                    if (typeName != null) {
                        String nn = typeName.typeref().name();
                        getCurrentDocument().addPair(SUPER_CLASS, typeName.typeref().name(), true, true);    
                    }
                }
            }
            classNames.add(0,className);
            return super.enterClassDefinition(node, arg);
        }
        
        @Override
        public Object exitClassDefinition( ClassDefinition node,  Object arg ) 
        {
            classNames.remove(0);
            nestedDocuments.remove(0);
            return super.exitClassDefinition(node, arg);
        }

        @Override
        public boolean enterImport( Import node, Object arg ) 
        {
    //          if (scopeStack.isEmpty()) {
    //             ClassFinder scope = new ClassFinder(
    //             context.get(ca.weblite.asm.ClassLoader.class),
    //             null
    //          );
    //          scopeStack.push(scope);
    //          }
    //          scopeStack.peek().addImport(node.fullName().identifier());
//            System.out.println("Entering import: "+node.fullName().identifier());
            
            return super.enterImport(node, arg);
        }

        // единственный известный мне способ получить полное имя суперкласса
        @Override
        public boolean enterSuper(Super node, Object arg)
        {
            ResolvedType type = parsed.getResolvedType(node);
            if ( type != null ) {
                String typeName = type.name();
                getCurrentDocument().addPair(SUPER_CLASS, typeName, true, true);    
            }
            return super.enterSuper(node, arg);
        }

        
    //    public boolean enterAnnotation(Annotation node, Object arg) {
    //    public boolean enterAnnotationList(AnnotationList node, Object arg) {
    //    @Override
    //    public boolean enterArguments(Arguments node, Object arg) {
    //        
    //        return super.enterArguments(node, arg);
    //    }
    //    public boolean enterBlock(Block node, Object arg) {
    //    public boolean enterBlockArgument(BlockArgument node, Object arg) {
    //    public boolean enterBlockPass(BlockPass node, Object arg) {
    //    public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
        // into index saved string: #{method_name}(#{arguments});#{modifiers};#{line_number}
        @Override
        public boolean enterConstructorDefinition(ConstructorDefinition node, Object arg) 
        {
            if (node.position() == null || node.position().startChar() == 0 ) {
                // это конструктор, вставленный компилятором - его не надо включать в индекс
                return super.enterConstructorDefinition(node, arg);
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(node.name().identifier());
//            sb.append(';');

    //            for( int i = 0 ; i < node.arguments().required().size() ; i++ )
    //            {
    //                RequiredArgument a = node.arguments().required().get(i);
    //                sb.append(a.name());
    //                sb.append(':');
    //                sb.append(a.type().typeref().name());
    //            }

            prepareArguments(node.arguments(), sb);
            sb.append(';');
            prepareModifiers(node.modifiers(), sb);
            sb.append(';');
            prepareLocation(node, sb);

//            System.out.println("CONSTRUCTOR="+sb.toString());
            if (getCurrentDocument() != null)
                getCurrentDocument().addPair(CONSTRUCTOR, sb.toString(), true, true);

//            LOG.info(MirahIndexer.class, "" + getClassName() + ": CONSTRUCTOR=" + sb.toString());

            return super.enterConstructorDefinition(node, arg);
        }
    //    public boolean enterFieldAccess(FieldAccess node, Object arg) {
        public boolean enterFieldAssign(FieldAssign node, Object arg) {

            if ( ! fields.containsKey(node.name().identifier()) )
            {
                StringBuilder sb = new StringBuilder(node.name().identifier());
                sb.append(';');
                prepareModifiers(node.modifiers(), sb);
                prepareLocation(node, sb);
//                System.out.println("FIELD_ASSIGN="+sb.toString());

                // TODO - gather documentation on fields? naeh
                if ( getCurrentDocument() != null ) getCurrentDocument().addPair(FIELD_NAME, sb.toString(), true, true);
                fields.put(node.name().identifier(), null);
            }
            return super.enterFieldAssign(node, arg);
        }

        @Override
        public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) 
        {
            StringBuilder sb = new StringBuilder(node.name().identifier());

    //        sb.append(';').append(org.netbeans.modules.groovy.editor.java.Utilities.translateClassLoaderTypeName(
    //                node.getType().getName()));

            prepareModifiers(node.modifiers(), sb);
            prepareLocation(node, sb);

    //            int flags = getFieldModifiersFlag(child.getModifiers());
    //            if (flags != 0 || child.isProperty()) {
    //                sb.append(';');
    //                sb.append(IndexedElement.flagToFirstChar(flags));
    //                sb.append(IndexedElement.flagToSecondChar(flags));
    //            }
    //
    //            if (child.isProperty()) {
    //                sb.append(';');
    //                sb.append(child.isProperty());
    //            }
//                System.out.println("FIELD_NAME="+sb.toString());

            // TODO - gather documentation on fields? naeh
            if (getCurrentDocument() != null)
                getCurrentDocument().addPair(FIELD_NAME, sb.toString(), true, true);
            return super.enterFieldDeclaration(node, arg);
        }
    //    public boolean enterInclude(Include node, Object arg) {
        @Override
        public boolean enterMacroDefinition(MacroDefinition node, Object arg) 
        {
            /*
            try {
                StringBuilder sb = new StringBuilder(); //ASTUtils.getDefSignature(childNode));
                sb.append(node.name().identifier());
                prepareArguments(node.arguments(), sb);
                sb.append(';');
                sb.append(';');
    //            prepareModifiers(node.modifiers(), sb);

                sb.append(';');
                prepareLocation(node, sb);
                if (getCurrentDocument() != null) {
                    getCurrentDocument().addPair(METHOD_NAME, sb.toString(), true, true);
                }
                LOG.info(null, "" + getClassName() + ": MACRO_NAME=" + sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info(null, "Add macro " + node.name().identifier() + " EXCEPTION=" + e);
            }
            */
            return super.enterMacroDefinition(node, arg);
        }
        
        @Override
        public Object exitMacroDefinition(MacroDefinition node, Object arg) {
//            classNames.remove(0);
//            nestedDocuments.remove(0);
            return super.exitMacroDefinition(node, arg);
        }
    //    public boolean enterModifier(Modifier node, Object arg) {
    //    public boolean enterOptionalArgument(OptionalArgument node, Object arg) {
    //    public boolean enterOptionalArgumentList(OptionalArgumentList node, Object arg) {
    //    public boolean enterRequiredArgument(RequiredArgument node, Object arg) {
    //    public boolean enterRequiredArgumentList(RequiredArgumentList node, Object arg) {
    //    @Override
    //    public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) 
    //    {
    //        return super.enterStaticMethodDefinition(node, arg);
    //    }
        private void prepareLocation(Node node, StringBuilder sb)
        {
    //        sb.append(file.getPath());
    //        sb.append(':');
            if ( node.position() != null ) sb.append(node.position().startChar());
//            sb.append(';');
        }

        private void prepareArguments(Arguments arguments, StringBuilder sb)
        {
            sb.append('(');
            for( int i = 0 ; i < arguments.required().size() ; i++ )
            {
                if (i != 0) sb.append(',');
                
                RequiredArgument a = arguments.required().get(i);
//                sb.append(a.name().identifier());
                ResolvedType type = this.parsed.getResolvedType(a);
                if (type != null) {
                    sb.append(type.name());
                } 
                else if ( a.type() != null )
                {
                    sb.append(a.type().typeref().name());
                }
            }
            sb.append(")");
        }
        private void prepareModifiers(ModifierList modifiers, StringBuilder sb)
        {
            for( int i = 0 ; i < modifiers.size() ; i++ )
            {
                if ( i != 0 ) sb.append(',');
                mirah.lang.ast.Modifier m = modifiers.get(i);
                sb.append(m.value());
    //            System.out.println("mody = "+mody.value());
            }

    //        int flags = modifiers.contains(Modifier.STATIC) ? Opcodes.ACC_STATIC : 0;
    //        if (modifiers.contains(Modifier.PRIVATE)) {
    //            flags |= Opcodes.ACC_PRIVATE;
    //        } else if (modifiers.contains(Modifier.PROTECTED)) {
    //            flags |= Opcodes.ACC_PROTECTED;
    //        }
    //
    //        return flags;
        }

    }

    
}
