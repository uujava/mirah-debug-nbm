package ru.programpark.mirah.lexer;


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
import org.openide.util.Exceptions;
import ru.programpark.mirah.compiler.IncrementalCompiler;
import ru.programpark.mirah.support.spi.MirahExtenderImplementation;

import javax.swing.event.ChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author shannah
 */
public class MirahLanguageParser extends Parser {

    private static final Logger logger = Logger.getLogger(MirahLanguageParser.class.getName());
    private SoftReference<CharSequence> prev = new SoftReference<>(null);
    private ParseErrorListener diag;
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

    public void reparse(Snapshot snapshot) throws ParseException {
        try {
            if (snapshot == null || snapshot.getText() == null) return;
            reparse(snapshot, snapshot.getText().toString());
        } catch (Exception ex) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "####### PARSE EXCEPTION File: " + snapshot.getSource().getFileObject().getName() + " ex: " + ex + " #######", ex);
        }
    }

    public void appendClassPath(ClassPath cp, StringBuffer sb, HashMap<Entry, Entry> map) {
        for (Entry e : cp.entries())
            if (!map.containsKey(e)) {
                map.put(e, null);
                if (sb.length() != 0) sb.append(File.pathSeparator);
                File f = FileUtil.archiveOrDirForURL(e.getURL());
                sb.append(f.getAbsolutePath());
            }
    }

    private void reparse(Snapshot snapshot, String content) throws ParseException {

        diag = new ParseErrorListener(snapshot.getSource().getFileObject());
        result = new MirahParserResult(snapshot, diag);

        IncrementalCompiler compiler = new IncrementalCompiler();

        FileObject src = snapshot.getSource().getFileObject();

        Project project = FileOwnerQuery.getOwner(src);

        if (project != null) {
            FileObject projectDirectory = project.getProjectDirectory();
            FileObject buildDir = projectDirectory.getFileObject("build");
            Preferences projPrefs = ProjectUtils.getPreferences(project, MirahExtenderImplementation.class, true);
            String projectType = projPrefs.get("project_type", "unknown");
            if ("maven".equals(projectType)) {
                try {
                    // It's a maven project so we want to build our sources to a different location
                    FileObject cacheDir = ProjectUtils.getCacheDirectory(project, MirahExtenderImplementation.class);
                    buildDir = cacheDir.getFileObject("build");
                    if (buildDir == null) {
                        buildDir = cacheDir.createFolder("build");
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            ClassPath compileClassPath = ClassPath.getClassPath(src, ClassPath.COMPILE);
            ClassPath buildClassPath = ClassPath.getClassPath(src, ClassPath.EXECUTE);
            ClassPath srcClassPath = ClassPath.getClassPath(src, ClassPath.SOURCE);
            ClassPath bootClassPath = ClassPath.getClassPath(src, ClassPath.BOOT);

            // для скриптов
            if (compileClassPath == null)
                compileClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.COMPILE);
            if (buildClassPath == null)
                buildClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.EXECUTE);
            if (srcClassPath == null)
                srcClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.SOURCE);
            if (bootClassPath == null)
                bootClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.BOOT);
            compiler.setSourcePath(srcClassPath == null ? "" : srcClassPath.toString());

            String dest = buildClassPath == null ? "" : buildClassPath.toString();
            FileObject mirahDir = null;
            try {
                if (buildDir == null) {
                    buildDir = projectDirectory.createFolder("build");
                }
                mirahDir = buildDir.getFileObject("mirah");
                if (mirahDir == null) {
                    mirahDir = buildDir.createFolder("mirah");
                }
                dest = mirahDir.getPath();
            } catch (IOException ex) {
            }
            compiler.setDestinationDirectory(new File(dest));
            compiler.setDiagnostics(diag);


            //todo убрать повторы в classpath
            HashMap<Entry, Entry> map = new HashMap<Entry, Entry>();
            StringBuffer sb = new StringBuffer();
            appendClassPath(compileClassPath, sb, map);
            appendClassPath(buildClassPath, sb, map);
            appendClassPath(bootClassPath, sb, map);
            String cp = sb.toString();
            compiler.setClassPath(cp);
        }

        String srcText = content;
        FileObject fakeFileRoot = getRoot(src);
        if (fakeFileRoot == null) {
            return;
        }
        String relPath = FileUtil.getRelativePath(fakeFileRoot, src);
        relPath = relPath.substring(0, relPath.lastIndexOf("."));
        compiler.addFakeFile(relPath, srcText);

        try {
            compiler.compile();
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "REPARSE ex = " + ex);
        }

        // сохраняю карту распознанныч типов
        result.setResolvedTypes(compiler.getResolvedTypes());

        // Сохраняю дерево разбора - это список List<Node>
        if (compiler.getParsedNodes() != null)
            result.setParsedNodes(compiler.getParsedNodes());

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
