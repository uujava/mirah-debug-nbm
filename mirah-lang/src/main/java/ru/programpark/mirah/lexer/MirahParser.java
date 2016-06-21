package ru.programpark.mirah.lexer;

import ru.programpark.mirah.LOG;
import ru.programpark.mirah.support.spi.MirahExtenderImplementation;
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

import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * @author shannah
 */
public class MirahParser extends Parser {

    private static WeakHashMap<FileObject, String> lastContent = new WeakHashMap<>();
    private Snapshot snapshot;
    private ParseErrorListener diag;
    private MirahParserResult result;

    public MirahParser() {
        System.out.println("parser = " + this + " " + System.identityHashCode(this));
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme)
            throws ParseException {

        long start = System.currentTimeMillis();

        String oldContent = lastContent.get(snapshot.getSource().getFileObject());

        String newContent = snapshot.getText().toString();
//        LOG.info(null, "----- Parsing Start: " + snapshot.getSource().getFileObject().getNameExt() + " -----");
//        LOG.putStack("***");
        boolean changed = oldContent == null || !oldContent.equals(newContent);
        if (sme.sourceChanged() && changed) {
            lastContent.put(
                    snapshot.getSource().getFileObject(),
                    newContent
            );
            reparse(snapshot);
        } else if (result == null) {
            reparse(snapshot);
        }
        LOG.info(null, "Source changed: parsing " + snapshot.getSource().getFileObject().getNameExt() + " Elapsed: " + (System.currentTimeMillis() - start) + " msec");
    }

    public void reparse(Snapshot snapshot) throws ParseException {
        try {
            if (snapshot == null || snapshot.getText() == null) return;
            reparse(snapshot, snapshot.getText().toString());
        } catch (Exception ex) {
            LOG.info(this, "####### PARSE EXCEPTION File: " + snapshot.getSource().getFileObject().getName() + " ex: " + ex + " #######");
            LOG.exception(this, ex);
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

    public void reparse(Snapshot snapshot, String content) throws ParseException {

        this.snapshot = snapshot;
        diag = new ParseErrorListener(snapshot.getSource().getFileObject());
        result = new MirahParserResult(snapshot, diag);

//        LOG.info(this,"REPARSE:\n"+content+"\n");
        Compiler compiler = new Compiler();

        FileObject src = snapshot.getSource().getFileObject();
        TypeInferenceListener debugger = new TypeInferenceListener();
        compiler.setDebugger(debugger);
        Project project = FileOwnerQuery.getOwner(src);

//        LOG.info(this,"reparse project = " + project);
        if (project != null) {
            FileObject projectDirectory = project.getProjectDirectory();
            FileObject buildDir = projectDirectory.getFileObject("build");
            Preferences projPrefs = ProjectUtils.getPreferences(project, MirahExtenderImplementation.class, true);
            String projectType = projPrefs.get("project_type", "unknown");
//        LOG.info(this,"reparse project type is "+projectType);
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
            // не надо приклеивать macroPath - это делается в WLMirahCompiler
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
//            LOG.info(this,"fakeFileRoot == NULL for src = " + src);
            return;
        }
        String relPath = FileUtil.getRelativePath(fakeFileRoot, src);
        relPath = relPath.substring(0, relPath.lastIndexOf("."));
        compiler.addFakeFile(relPath, srcText);

        try {
            compiler.compile();
        } catch (Exception ex) {
            LOG.info(this,"REPARSE ex = "+ex);
//            ex.printStackTrace();
        }
        if (debugger != null && result != null) {
            // сохраняю карту распознанныч типов
            result.setResolvedTypes(debugger.getResolvedTypes());
        }

        if (debugger.getResolvedTypes().size() > 0) {
            debugger.compiler = compiler;
            // Сохраняю дерево разбора - это список List<Node>
            if (debugger.compiler.getParsedNodes() != null)
                result.setParsedNodes(debugger.compiler.getParsedNodes());
        }

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
