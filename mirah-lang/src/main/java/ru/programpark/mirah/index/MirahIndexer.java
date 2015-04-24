package ru.programpark.mirah.index;

import ca.weblite.asm.ClassFinder;
import ca.weblite.asm.MirahClassIndex;
import ca.weblite.asm.MirahClassLoader;
import ca.weblite.asm.WLMirahCompiler;
import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Import;
import mirah.lang.ast.InterfaceDeclaration;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexer;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.support.IndexDocument;
import org.netbeans.modules.parsing.spi.indexing.support.IndexingSupport;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class MirahIndexer extends EmbeddingIndexer {

    // class
    static final String FQN_NAME = "fqn"; //NOI18N
    static final String CLASS_NAME = "class"; //NOI18N
    static final String CASE_INSENSITIVE_CLASS_NAME = "class-ig"; //NOI18N
    // not indexed
    static final String IN = "in"; //NOI18N
    /** Attributes: hh;nnnn where hh is a hex representing flags in IndexedClass, and nnnn is the documentation length */
    static final String CLASS_ATTRS = "attrs"; //NOI18N

    // method
    static final String METHOD_NAME = "method"; //NOI18N

    // constructor
    static final String CONSTRUCTOR = "ctor"; //NOI18N

    // field
    static final String FIELD_NAME = "field"; //NOI18N

    /** Attributes: "i" -> private, "o" -> protected, ", "s" - static/notinstance, "d" - documented */
    //static final String ATTRIBUTE_NAME = "attribute"; //NOI18N

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
    
//    private static final Logger LOG = Logger.getLogger(MirahIndexer.class.getName());

    @Override
    protected void index(Indexable indexable, Result parserResult, Context context) {
        long indexerThisStartTime = System.currentTimeMillis();

try
{
//        LOG.info(this,"----------------------- Start indexing -----------------------");
//        LOG.info(this,"root="+context.getRoot().getPath());
//        LOG.info(this,"file="+parserResult.getSnapshot().getSource().getFileObject().getPath());
//        LOG.putStack(null);
        
        if (indexerFirstRun == 0) {
            indexerFirstRun = indexerThisStartTime;
        }

//        WLMirahCompiler compiler = new WLMirahCompiler();
//        LOG.info(this, "compiler=" + compiler);
/*        
        compiler.setup();
//        compiler.getDebugger().
        //MirahClassIndex classIndexer = new MirahClassIndex();
        LOG.info(this, "classIndexer=" + compiler.getIndex());
        
        compiler.getIndex().indexFile(parserResult.getSnapshot().getSource().getFileObject().getPath());
        MirahParser parser = new MirahParser();
        LOG.info(this,"parser="+parser);
        LOG.info(this,"parserResult.getSnapshot()="+parserResult.getSnapshot());
//        parser.reparse(parserResult.getSnapshot());
//        
//        Object ast = parser.getResult(null);
        
        Node ast = parser.prepareAst(parserResult.getSnapshot());
        LOG.info(this, "ast=" + ast);
        
        if (ast != null && ast instanceof Node) {

            LOG.info(this,"|||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
//            this.prepareIndex((Node)ast);
        }
        
        
/*
        GroovyParserResult r = (GroovyParserResult) ASTUtils.getParseResult(parserResult);
        ASTNode root = ASTUtils.getRoot(r);

        if (root == null) {
            return;
        }
*/
        
        Node ast = null; 
        if ( parserResult instanceof MirahParser.NBMirahParserResult )
        {
            ast = ((MirahParser.NBMirahParserResult)parserResult).getRoot();
        }
        if ( ast != null )
        {
            IndexingSupport support;
            try {
                support = IndexingSupport.getInstance(context);
            } catch (IOException ioe) {
                LOG.exception(this, ioe);
                return;
            }
//            LOG.info(this,"support = "+support);

            IndexScanner scanner = new IndexScanner(parserResult.getSnapshot(),support,indexable);
            scanner.analyze(ast);
            for (IndexDocument doc : scanner.getDocuments()) {
                support.addDocument(doc);
            }
        }
        filesIndexed++;
        long indexerThisStopTime = System.currentTimeMillis();
        long indexerThisRunTime = indexerThisStopTime - indexerThisStartTime;
        indexerRunTime += indexerThisRunTime;

        LOG.info(this, "---------------------------------------------------------------------------------");
        LOG.info(this,"Indexed File                : "+parserResult.getSnapshot().getSource().getFileObject().getPath());
        LOG.info(this,"Indexing time (ms)          : "+indexerThisRunTime);
        LOG.info(this,"Number of files indexed     : "+filesIndexed);
        LOG.info(this,"Time spend indexing (ms)    : "+indexerRunTime);
        LOG.info(this,"Avg indexing time/file (ms) : "+indexerRunTime/filesIndexed);
        LOG.info(this,"Time betw. 1st and Last idx : "+(indexerThisStopTime - indexerFirstRun));
        LOG.info(this, "---------------------------------------------------------------------------------");
}
catch( Exception ee )
{
    LOG.exception(this, ee);
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

    public static class IndexScanner extends NodeScanner
    {
        private final FileObject file;
        private final IndexingSupport support;
        private final Indexable indexable;
        private final List<IndexDocument> documents = new ArrayList<IndexDocument>();
        private String packageName = null;
        private ClassNode lastFoundClass = null;

        public IndexScanner( Snapshot snapshot, IndexingSupport support, Indexable indexable )
        {
//            this.result = result;
            this.file = snapshot.getSource().getFileObject();
            this.support = support;
            this.indexable = indexable;
        }

        List<IndexDocument> getDocuments() 
        {
            return documents;
        }
        
        public void analyze( Node node )
        {
            node.accept(this, null);
        }

        @Override
        public boolean enterMethodDefinition( MethodDefinition node, Object arg )
        {
//            LOG.info(MirahIndexer.class, "enterMethodDefinition name=" + node.name().identifier()+" file="+file.getName()+" node="+node);
            return true;
//            return enterMethodDefinition(node, arg);
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
        public boolean enterInterfaceDeclaration(InterfaceDeclaration node, Object arg ) 
        {
            return enterClassDefinition(node, arg);
        }

        @Override
        public boolean enterClassDefinition( ClassDefinition node, Object arg ) 
        {
            if (lastFoundClass != null) {
                return true;
            }
            String className = packageName != null
                    ? packageName.replaceAll("\\.", "/") + "/"
                    + node.name().identifier()
                    : node.name().identifier();

//            LOG.info(MirahIndexer.class, "enterClassDefinition className=" + className);
            
            IndexDocument document = support.createDocument(indexable);
            documents.add(document);

            document.addPair(FQN_NAME, className, true, true);
            document.addPair(CLASS_NAME, node.name().identifier(), true, true);
            document.addPair(CONSTRUCTOR, "ctor()", true, true);            
            
            //document.addPair(CASE_INSENSITIVE_CLASS_NAME, name.toLowerCase(), true, true);
            return super.enterClassDefinition(node, arg);
        }

        @Override
        public Object exitClassDefinition( ClassDefinition node,  Object arg ) 
        {
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
            //System.out.println("Entering import: "+node.fullName().identifier());
            return super.enterImport(node, arg);
        }

    }
    
}
