package ru.programpark.mirah.lexer;


import mirah.lang.ast.StringCodeSource;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.ClassPath.Entry;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.*;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import ru.programpark.mirah.compiler.InteractiveCompiler;
import ru.programpark.mirah.compiler.MapCacheConsumer;
import ru.programpark.mirah.compiler.impl.MirahInteractiveCompiler;
import ru.programpark.mirah.compiler.loaders.IndexedResourceLoader;
import ru.programpark.mirah.compiler.loaders.SourceInjectorLoader;

import javax.swing.event.ChangeListener;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author shannah
 */
public class MirahLanguageParser extends Parser {

    private static final Logger logger = Logger.getLogger(MirahLanguageParser.class.getName());
    public Set<String> EXTENSIONS = new HashSet<>(Arrays.asList(new String[]{"mirah", "vrb"}));
    private SoftReference<CharSequence> prev = new SoftReference<>(null);
    private MirahParserResult result;

    public MirahLanguageParser() {
        logger.info("parser = " + this + " " + System.identityHashCode(this));
    }

    private static FileObject getRoot(FileObject file) {
        Project project = FileOwnerQuery.getOwner(file);
        Sources sources = ProjectUtils.getSources(project);
        for (SourceGroup sourceGroup : sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
            FileObject root = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(root, file) || root.equals(file)) {
                return root;
            }
        }
        ClassPath path = ClassPath.getClassPath(file, ClassPath.SOURCE);
        return path != null ? path.findOwnerRoot(file) : null;
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme)
            throws ParseException {

        long start = System.currentTimeMillis();
        CharSequence oldContent = prev.get();
        CharSequence newContent = snapshot.getText();
        boolean changed = oldContent == null || !oldContent.equals(newContent);
        if (sme.sourceChanged() && changed) {
            prev = new SoftReference<>(newContent);
            reparse(snapshot);
        } else if (result == null) {
            reparse(snapshot);
        }
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Source changed: parsing " + snapshot.getSource().getFileObject().getNameExt() + " Elapsed: " + (System.currentTimeMillis() - start) + " msec");
    }

    private void reparse(Snapshot snapshot) throws ParseException {
        try {
            if (snapshot == null || snapshot.getText() == null) return;
            reparse(snapshot, snapshot.getText().toString());
        } catch (Exception ex) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "####### PARSE EXCEPTION File: " + snapshot.getSource().getFileObject().getName() + " ex: " + ex + " #######", ex);
        }
    }

    private void reparse(Snapshot snapshot, String content) throws ParseException {

        ParseErrorListener diag = new ParseErrorListener(snapshot.getSource().getFileObject());
        result = new MirahParserResult(snapshot, diag);


        final MirahInteractiveCompiler compiler = new MirahInteractiveCompiler(diag);

        FileObject src = snapshot.getSource().getFileObject();

        setupPaths(compiler, src);

        String srcText = content;
        FileObject fakeFileRoot = getRoot(src);
        if (fakeFileRoot == null) {
            return;
        }
        String relPath = FileUtil.getRelativePath(fakeFileRoot, src);
        relPath = relPath.substring(0, relPath.lastIndexOf("."));
        compiler.add(new StringCodeSource(relPath, srcText));


        try {
            MapCacheConsumer cacheConsumer = new MapCacheConsumer();
            compiler.run(cacheConsumer);
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "REPARSE ex = " + ex);
        }

        // сохраняю карту распознанныч типов
        result.setResolvedTypes(compiler.getResolvedTypes());

        // Сохраняю дерево разбора - это список List<Node>
        if (compiler.getParsedNodes() != null)
            result.setParsedNodes(compiler.getParsedNodes());
    }

    private void setupPaths(InteractiveCompiler compiler, FileObject src) {
        Project project = FileOwnerQuery.getOwner(src);

        if (project != null) {
            FileObject projectDirectory = project.getProjectDirectory();
            ClassPath compileClassPath = ClassPath.getClassPath(src, ClassPath.COMPILE);
            ClassPath executeClassPath = ClassPath.getClassPath(src, ClassPath.EXECUTE);
            ClassPath srcClassPath = ClassPath.getClassPath(src, ClassPath.SOURCE);
            ClassPath bootClassPath = ClassPath.getClassPath(src, ClassPath.BOOT);

            // для скриптов
            if (compileClassPath == null)
                compileClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.COMPILE);
            if (executeClassPath == null)
                executeClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.EXECUTE);
            if (srcClassPath == null)
                srcClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.SOURCE);

            if (bootClassPath == null)
                bootClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.BOOT);

            List<Entry> entries = srcClassPath.entries();
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                FileObject root = entry.getRoot();
                if (root != null) {
                    SnapshotReader snapshotReader = new SnapshotReader(root.getPath(), getRegisteredExtensions());
                    compiler.registerLoader(new SourceInjectorLoader(compiler, snapshotReader));
                }
            }

            compiler.registerLoader(new ClassPathResourceLoader(compileClassPath));
        }

    }

    private Set<String> getRegisteredExtensions() {
        return EXTENSIONS;
    }

    @Override
    public Result getResult(Task task) {
        return result;
    }

    @Override
    public void cancel(CancelReason reason, SourceModificationEvent event) {
        super.cancel(reason, event);
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
    }


}
