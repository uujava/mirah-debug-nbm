/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package ru.programpark.mirah.support.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.support.GenericSources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;

/**
 * Constants useful for Mirah-based projects.
 *
 * @author Martin Adamek
 */
public class MirahSources {

    /**
     * Location of Mirah file icon (16x16)
     */
    public static final String MIRAH_FILE_ICON_16x16 = "ru/programpark/mirah/support/resources/MirahFile16x16.png"; // NOI18N

    /**
     * Mirah package root sources type.
     * @see org.netbeans.api.project.Sources
     */
    public static final String SOURCES_TYPE_MIRAH = "vrb"; // NOI18N

    private static final Map<Project, SourceGroup> SOURCE_GROUP_MIRAH = new HashMap<>();
    private static final Map<Project, SourceGroup> TEST_GROUP_MIRAH = new HashMap<>();
    
    private static SourceGroup createSourceGroup(Project project, String path, String name) {
        try {
            final File srcDir = new File(project.getProjectDirectory().getPath() + path);
            srcDir.createNewFile();
            FileObject mirahSrcDir = FileUtil.createFolder(srcDir);
            Icon icon = new ImageIcon(ImageUtilities.loadImage(MIRAH_FILE_ICON_16x16));
            SourceGroup srcGroup = GenericSources.group(project, mirahSrcDir, SOURCES_TYPE_MIRAH, name, icon, icon);
            return srcGroup;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }
    
    private static SourceGroup getSrcMainMirahGroup(Project project) {
        SourceGroup group = SOURCE_GROUP_MIRAH.get(project);
        if (group == null) {
            group = createSourceGroup(project, "/src/main/vrb", "Исходные файлы VRuby");
            SOURCE_GROUP_MIRAH.put(project, group);
        }
        return group; 
    }
    
    private static SourceGroup getSrcTestMirahGroup(Project project) {
        SourceGroup group = TEST_GROUP_MIRAH.get(project);
        if (group == null) {
            group = createSourceGroup(project, "/src/test/vrb", "Тестовые файлы VRuby");
            TEST_GROUP_MIRAH.put(project, group);
        }
        return group; 
    }

    /**
     * Searches for all source groups that can contain Mirah sources, including Grails
     * default folders and also folders added to Grails by plugins etc...
     */
    public static List<SourceGroup> getMirahSourceGroups(Sources sources, Project project) {
        List<SourceGroup> result = new ArrayList<>();
        result.addAll(Arrays.asList(sources.getSourceGroups(MirahSources.SOURCES_TYPE_MIRAH)));
        result.addAll(Arrays.asList(sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)));
       
        SourceGroup srcGroup = getSrcMainMirahGroup(project);
        SourceGroup tstGroup = getSrcTestMirahGroup(project);
        if (tstGroup != null) {
            result.add(0, tstGroup);
        }
        if (srcGroup != null) {
            result.add(0, srcGroup);
        }
        
        return result;
    }
}
