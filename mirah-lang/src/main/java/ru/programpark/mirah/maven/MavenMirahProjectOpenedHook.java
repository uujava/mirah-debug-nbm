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

package ru.programpark.mirah.maven;



import ru.programpark.mirah.RecompileQueue;
import org.netbeans.api.project.Project;
import ru.programpark.mirah.support.spi.MirahExtenderImplementation;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;

/**
 *
 * @author Martin Janicek
 */
@ProjectServiceProvider(
    service =
        ProjectOpenedHook.class,
    projectType = {
        "org-netbeans-modules-maven"
    }
)
public class MavenMirahProjectOpenedHook extends ProjectOpenedHook {
    private static final Logger logger = Logger.getLogger(MavenMirahProjectOpenedHook.class.getName());
    private final Project project;

    public MavenMirahProjectOpenedHook(Project project) {
        this.project = project;
        
    }

    @Override
    protected void projectOpened() {
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "projectOpened project=" + project);
        project.getProjectDirectory().addRecursiveListener(new FileChangeAdapter(){

            @Override
            public void fileChanged(FileEvent fe) {
                if ( fe.getFile().getPath().endsWith(".java")){
                    if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "file " + fe.getFile().getNameExt() + " changed in project " + project.getProjectDirectory().getNameExt());
                    //System.out.println("Adding to compile queue "+fe.getFile());
                    RecompileQueue.getProjectQueue(project).addChanged(fe.getFile());
                }
                super.fileChanged(fe); 
            }
            
        });
        
        //BuildScriptHelper.refreshBuildScript(project, BuildScriptType.J2SE.getStylesheet(), true);
        MavenMirahExtender ext = new MavenMirahExtender(project);
        
        //if ( !ext.isCurrent() ){
        //    ext.activate();
        //}
        Preferences prefs = ProjectUtils.getPreferences(project, MirahExtenderImplementation.class, true);
        prefs.put("project_type", "maven");
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Setting project type in prefs to maven");

        //svd - force maven project indexing
//        IndexingManager.getDefault().refreshIndex(project.getProjectDirectory().toURL(),null,false);
        
    }

    @Override
    protected void projectClosed() {
    }
}
