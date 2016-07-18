/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package ca.weblite.netbeans.mirah.antproject.base;

import ca.weblite.netbeans.mirah.LOG;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.source.ui.ScanDialog;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Base ActionProvider for each Ant based project. Support few basic operations:
 * - Compile File
 * - Run File
 * - Test File
 * - Debug File
 * - Debug Test File
 *
 * Implementors should create own map of the supported actions for a certain
 * type of project. The reason why this cannot be done in general is that we
 * have different ant target names in a different types of project (e.g. for
 * debugging single file there is a "debug-single-main" command in the Web
 * project, but "debug-single" command in the J2SE project).
 *
 * @author Martin Janicek
 */
public abstract class AbstractMirahActionProvider implements ActionProvider {

    // from J2SEProjectProperties
    public static final String BUILD_SCRIPT = "buildfile";               // NOI18N
    // from J2SEConfigurationProvider
    public static final String PROP_CONFIG = "config";                  // NOI18N
    /** Map from commands to mirah targets */
    private final Map<String, String> supportedActions;
    private final Project project;
    private boolean overrideTestTarget = true;

    
    public AbstractMirahActionProvider(Project project) {
        this.project = project;
        FileObject destDirFO = project.getProjectDirectory().getFileObject("nbproject"); // NOI18N
        FileObject projectFO = project.getProjectDirectory();
        try {
            GeneratedFilesHelper helper = new GeneratedFilesHelper(project.getProjectDirectory());
            // Check if this is a codename one project
            FileObject cn1PropertiesFO = projectFO.getFileObject("codenameone_settings", "properties");
            FileObject cn1LibraryPropertiesFO = projectFO.getFileObject("codenameone_library", "properties");
            boolean isCodename1Lib = cn1LibraryPropertiesFO != null;
            boolean isCodename1Proj = cn1PropertiesFO != null;

            if ( isCodename1Lib || isCodename1Proj){
                overrideTestTarget = false;
            }
        } catch ( Exception ex){}
        this.supportedActions = new HashMap<String, String>();

        supportedActions.put(COMMAND_COMPILE_SINGLE, "compile-single"); // NOI18N
        supportedActions.put(COMMAND_TEST_SINGLE, "test-single");       // NOI18N
        supportedActions.put(COMMAND_DEBUG_TEST_SINGLE, "debug-test");  // NOI18N
        if ( overrideTestTarget ){
            supportedActions.put(COMMAND_TEST, "test");
        }                     // NOI18N
        addProjectSpecificActions(supportedActions);
    }

    /**
     * Implementors should create own map of the supported actions for a certain
     * type of project. The reason why this cannot be done in general is that we
     * have different ant target names in a different types of project (e.g. for
     * debugging single file there is a "debug-single-main" command in the Web
     * project, but "debug-single" command in the J2SE project)
     *
     * @return map where the key is command name and the value is a mirah target
     */
    protected abstract void addProjectSpecificActions(Map<String, String> actionMap);

    @Override
    public String[] getSupportedActions() {
        FileObject destDirFO = project.getProjectDirectory().getFileObject("nbproject"); // NOI18N
        if (destDirFO != null) {
            FileObject mirahBuild = destDirFO.getFileObject("mirah-build.xml"); // NOI18N
            if (mirahBuild == null) {
                supportedActions.remove(COMMAND_TEST);
            } else {
                if (!supportedActions.containsKey(COMMAND_TEST)) {
                    if ( overrideTestTarget ){
                        supportedActions.put(COMMAND_TEST, "test"); // NOI18N
                    }
                }
            }
        } else {
            supportedActions.remove(COMMAND_TEST);
        }
        return supportedActions.keySet().toArray(new String[0]);
    }

    @Override
    public boolean isActionEnabled(String command, Lookup context) {
        if (supportedActions.keySet().contains(command)) {
            if (COMMAND_TEST.equals(command)) {
                return true;
            }

            FileObject[] testSources = findTestSources(context);
            FileObject[] sources = findSources(context);

            // Action invoked on file from "test" folder
            if (testSources != null) {
                return true;
            }

            // Action invoked on file from "src" folder
            if (sources != null && sources.length == 1) {
                if (COMMAND_TEST_SINGLE.equals(command) || COMMAND_DEBUG_TEST_SINGLE.equals(command)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void invokeAction(final String command, final Lookup context) {
        //System.out.println("Invoking action "+command);
        LOG.info(this,"Invoking action "+command);
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                Properties p = new Properties();
                String[] targetNames = getTargetNames(command, context, p);

                if (targetNames.length == 0) {
                    targetNames = null;
                }
                if (p.keySet().isEmpty()) {
                    p = null;
                }
                try {
                    FileObject buildFo = findBuildXml();
                    if (buildFo == null || !buildFo.isValid()) {
                        //The build.xml was deleted after the isActionEnabled was called
                        NotifyDescriptor nd = new NotifyDescriptor.Message(NbBundle.getMessage(AbstractMirahActionProvider.class,
                                "LBL_No_Build_XML_Found"), NotifyDescriptor.WARNING_MESSAGE);
                        DialogDisplayer.getDefault().notify(nd);
                    } else {
                        ActionUtils.runTarget(buildFo, targetNames, p);
                    }
                } catch (IOException e) {
                    ErrorManager.getDefault().notify(e);
                }
            }
        };

        if (supportedActions.containsKey(command)) {
            ScanDialog.runWhenScanFinished(action, NbBundle.getMessage(AbstractMirahActionProvider.class,"ACTION_"+command));   //NOI18N
        } else {
            action.run();
        }
    }

    private String[] getTargetNames(String command, Lookup context, Properties p) {
        
//        LOG.info("getTargetNames "+command);
        
        if (supportedActions.keySet().contains(command)) {
            if (command.equals(COMMAND_TEST)) {
                return setupTestAll(p);
            }

            FileObject[] testSources = findTestSources(context);
            if (testSources != null) {
                if (command.equals(COMMAND_RUN_SINGLE) || command.equals(COMMAND_TEST_SINGLE)) {
                     return setupTestSingle(p, testSources);
                } else if (command.equals(COMMAND_DEBUG_SINGLE) || (command.equals(COMMAND_DEBUG_TEST_SINGLE))) {
                    return setupDebugTestSingle(p, testSources);
                } else if (command.equals(COMMAND_COMPILE_SINGLE)) {
                    return setupCompileSingle(p, testSources);
                }
            } else {
                FileObject file = findSources(context)[0];
                Sources sources = ProjectUtils.getSources(project);
                SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                String clazz = FileUtil.getRelativePath(getRoot(sourceGroups, file), file);
                p.setProperty("javac.includes", clazz); // NOI18N
                // Convert foo/FooTest.java -> foo.FooTest
                if (clazz.endsWith(".mirah")) { // NOI18N
                    clazz = clazz.substring(0, clazz.length() - 6);
                }
                clazz = clazz.replace('/','.');

                String[] targets = loadTargetsFromConfig().get(command);
                if (command.equals(COMMAND_RUN_SINGLE)) {
                    p.setProperty("run.class", clazz); // NOI18N
                } else if (command.equals(COMMAND_DEBUG_SINGLE)) {
                    p.setProperty("debug.class", clazz); // NOI18N
                } else if (command.equals(COMMAND_COMPILE_SINGLE)) {
                    p.setProperty("compile.class", clazz); // NOI18N
                }
                return getTargetNamesForCommand(targets, command);
            }
        }
        return new String[0];
    }


    private String[] getTargetNamesForCommand(String[] targetsFromConfig, String commandName) {
        if (targetsFromConfig != null) {
            return targetsFromConfig;
        } else {
            return new String[] {supportedActions.get(commandName)};
        }
    }

    private FileObject getRoot(SourceGroup[] groups, FileObject file) {
        assert file != null : "File can't be null";   //NOI18N
        FileObject srcDir = null;
        for (SourceGroup sourceGroup : groups) {
            FileObject root = sourceGroup.getRootFolder();
            assert root != null : "Source Path Root can't be null"; //NOI18N
            if (FileUtil.isParentOf(root, file) || root.equals(file)) {
                srcDir = root;
                break;
            }
        }
        return srcDir;
    }

    private FileObject getRoot(FileObject[] groups, FileObject file) {
        assert file != null : "File can't be null";   //NOI18N
        FileObject srcDir = null;
        for (FileObject root : groups) {
            if (FileUtil.isParentOf(root, file) || root.equals(file)) {
                srcDir = root;
                break;
            }
        }
        return srcDir;
    }

    private FileObject[] findSources(Lookup context) {
        
//        LOG.info("findSources context="+context);
        Sources sources = ProjectUtils.getSources(project);
        for (SourceGroup sourceGroup : sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
            FileObject[] files = ActionUtils.findSelectedFiles(context, sourceGroup.getRootFolder(), ".vrb", true); // NOI18N
            if (files != null) {
                return files;
            }
        }
        return null;
    }

    /**
     * Find either selected tests or tests which belong to selected source files
     */
    private FileObject[] findTestSources(Lookup context) {
        for (FileObject testSourceRoot : getTestSourceRoots(project)) {
            FileObject[] files = ActionUtils.findSelectedFiles(context, testSourceRoot, ".vrb", true); // NOI18N
            if (files != null) {
                return files;
            }
        }
        return null;
    }

    private FileObject findBuildXml() {
        return getBuildXml(project);
    }

    public static String getBuildXmlName(final Project project) {
        assert project != null;
        String buildScriptPath = evaluateProperty(project, BUILD_SCRIPT);
        if (buildScriptPath == null) {
            buildScriptPath = GeneratedFilesHelper.BUILD_XML_PATH;
        }
        return buildScriptPath;
    }

    public static FileObject getBuildXml(final Project project) {
        return project.getProjectDirectory().getFileObject(getBuildXmlName(project));
    }

    private static FileObject[] getTestSourceRoots(Project project) {
        List<String> names = getTestRootsNames(project);
        List<FileObject> result = new ArrayList<FileObject>();
        for (String name : names) {
            // FileObject JavaDoc says that path delimited should be always '/'
            // See issue #238330 for more details
            FileObject root = project.getProjectDirectory().getFileObject(name.replace('\\', '/')); // NOI18N
            if (root != null) {
                result.add(root);
            }
        }
        return result.toArray(new FileObject[result.size()]);
    }

    private static List<String> getTestRootsNames(Project project) {
        List<String> result = new ArrayList<String>();

        FileObject projectProperties = getPropertiesFO(project);
        if (projectProperties != null) {
            Map<String, String> map = getPropertiesMap(FileUtil.toFile(projectProperties));

            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().startsWith("test.") && entry.getKey().endsWith(".dir")) { // NOI18N
                    result.add(entry.getValue());
                }
            }
        }

        return result;
    }

    private static String evaluateProperty(Project project, String key) {
        FileObject projectProperties = getPropertiesFO(project);
        if (projectProperties != null) {
            return getPropertiesMap(FileUtil.toFile(projectProperties)).get(key);
        }
        return null;
    }

    private static FileObject getPropertiesFO(Project project) {
         return project.getProjectDirectory().getFileObject("nbproject/project.properties"); // NOI18N
    }

    private static Map<String, String> getPropertiesMap(File projectPropertiesFO) {
        return PropertyUtils.propertiesFilePropertyProvider(projectPropertiesFO).getProperties();
    }

    private String[] setupTestAll(Properties p) {
        // Convert foo/FooTest.java -> foo.FooTest
        System.out.println("Setting up testAll");
        p.setProperty("test.binarytestincludes", "**/*Test.class"); // NOI18N
        p.setProperty("test.binaryexcludes", "**/*$*");             // NOI18N
        p.setProperty("test.binaryincludes", "");  
        // NOI18N
        System.out.println(p);
        return new String[] {"test-with-mirah"};                   // NOI18N
    }

    private String[] setupTestSingle(Properties p, FileObject[] files) {
        FileObject[] testSrcPath = getTestSourceRoots(project);
        FileObject root = getRoot(testSrcPath, files[0]);
        String path = FileUtil.getRelativePath(root, files[0]);
        // Convert foo/FooTest.java -> foo.FooTest
        p.setProperty("test.binarytestincludes", path.substring(0, path.length() - 6) + ".class");  // NOI18N
        p.setProperty("test.binaryexcludes", "**/*$*");                                             // NOI18N
        p.setProperty("test.binaryincludes", "");                                                   // NOI18N
        p.setProperty("javac.includes", ActionUtils.antIncludesList(files, root));                  // NOI18N
        return new String[] {"test-single-mirah"};                                                 // NOI18N
    }

    private String[] setupDebugTestSingle(Properties p, FileObject[] files) {
        FileObject[] testSrcPath = getTestSourceRoots(project);
        FileObject root = getRoot(testSrcPath, files[0]);
        String path = FileUtil.getRelativePath(root, files[0]);
        // Convert foo/FooTest.java -> foo.FooTest
        p.setProperty("test.binarytestincludes", path.substring(0, path.length() - 6) + ".class");   // NOI18N
        p.setProperty("test.binaryexcludes", "**/*$*");                                              // NOI18N
        p.setProperty("test.binaryincludes", "");                                                    // NOI18N
        p.setProperty("test.class", path.substring(0, path.length() - 6).replace('/', '.'));         // NOI18N
        p.setProperty("javac.includes", ActionUtils.antIncludesList(files, root));                   // NOI18N
        return new String[] {"debug-test"};                                                          // NOI18N
    }

    private String[] setupCompileSingle(Properties p, FileObject[] files) {
//        LOG.info(this,"setupCompileSingle Properties="+p);
        
        FileObject[] testSrcPath = getTestSourceRoots(project);
        FileObject root = getRoot(testSrcPath, files[0]);
        String path = FileUtil.getRelativePath(root, files[0]);
        // Convert foo/FooTest.java -> foo.FooTest
        p.setProperty("compile.class", path.substring(0, path.length() - 6).replace('/', '.')); // NOI18N
        p.setProperty("javac.includes", ActionUtils.antIncludesList(files, root)); // NOI18N
        return new String[] {"compile-single"}; // NOI18N
    }

    // loads targets for specific commands from shared config property file
    // returns map; key=command name; value=array of targets for given command
    private HashMap<String, String[]> loadTargetsFromConfig() {
        HashMap<String, String[]> targets = new HashMap<String, String[]>(6);
        String config = evaluateProperty(project, PROP_CONFIG);
        // load targets from shared config
        FileObject propFO = project.getProjectDirectory().getFileObject("nbproject/configs/" + config + ".properties");
        if (propFO == null) {
            return targets;
        }
        Properties props = new Properties();
        try {
            InputStream is = propFO.getInputStream();
            try {
                props.load(is);
            } finally {
                is.close();
            }
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            return targets;
        }
        Enumeration propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String propName = (String) propNames.nextElement();
            if (propName.startsWith("$target.")) {
                String tNameVal = props.getProperty(propName);
                if (tNameVal != null && !tNameVal.equals("")) {
                    String cmdNameKey = propName.substring("$target.".length());
                    StringTokenizer stok = new StringTokenizer(tNameVal.trim(), " ");
                    List<String> targetNames = new ArrayList<String>(3);
                    while (stok.hasMoreTokens()) {
                        targetNames.add(stok.nextToken());
                    }
                    targets.put(cmdNameKey, targetNames.toArray(new String[targetNames.size()]));
                }
            }
        }
        return targets;
    }
}
