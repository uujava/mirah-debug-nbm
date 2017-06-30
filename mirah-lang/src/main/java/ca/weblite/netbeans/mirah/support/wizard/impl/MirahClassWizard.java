/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */

package ca.weblite.netbeans.mirah.support.wizard.impl;

import java.util.Arrays;
import java.util.List;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.api.templates.TemplateRegistrations;
import ca.weblite.netbeans.mirah.support.wizard.AbstractMirahWizard;
import org.openide.util.NbBundle;

/**
 *
 * @author Martin Janicek
 */
@NbBundle.Messages(value = {
    "LBL_DisplayName_MirahClass=VRuby Класс",
    "LBL_DisplayName_MirahScript=VRuby Скрипт"
})
@TemplateRegistrations(value = {
    @TemplateRegistration(
        folder = "Mirah",
        position = 100,
        content = "/ca/weblite/netbeans/mirah/support/resources/MirahClass.mirah",
        scriptEngine = "freemarker",
        displayName = "#LBL_DisplayName_MirahClass",
        iconBase = "ca/weblite/netbeans/mirah/support/resources/MirahFile16x16.png",
        description = "/ca/weblite/netbeans/mirah/support/resources/MirahClass.html",
        category = {
            "mirah",
            "java-main-class"
        }
    ),

    @TemplateRegistration(
        folder = "Mirah",
        position = 110,
        content = "/ca/weblite/netbeans/mirah/support/resources/MirahScript.mirah",
        scriptEngine = "freemarker",
        displayName = "#LBL_DisplayName_MirahScript",
        iconBase = "ca/weblite/netbeans/mirah/support/resources/MirahFile16x16.png",
        description = "/ca/weblite/netbeans/mirah/support/resources/MirahScript.html",
        category = {
            "mirah",
            "java-main-class"
        }
    )
})
public class MirahClassWizard extends AbstractMirahWizard {

    @Override
    protected List<SourceGroup> getSourceGroups() {
        List<SourceGroup> sourceGroups = retrieveGroups();

        if (!strategy.existsMirahSourceFolder(sourceGroups)) {
            strategy.createMirahSourceFolder();

            // Retrieve the source groups again, but now with a newly created /test/mirah folder
            sourceGroups = retrieveGroups();
        }
//        return strategy.moveSourceFolderAsFirst(sourceGroups);
        return sourceGroups;
    }
}
