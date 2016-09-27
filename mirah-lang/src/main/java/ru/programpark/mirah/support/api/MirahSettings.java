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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;
import ru.programpark.mirah.support.options.SupportOptionsPanelController;
import org.netbeans.spi.options.AdvancedOption;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 * Mirah settings
 *
 * @author Martin Adamek
 */
// FIXME separate classes ?
public final class MirahSettings extends AdvancedOption {

    public static final String MIRAH_OPTIONS_CATEGORY = "Advanced/org-netbeans-modules-mirah-support-api-MirahSettings"; // NOI18N
    public static final String MIRAH_DOC_PROPERTY  = "mirah.doc"; // NOI18N
    
    private static final String MIRAH_DOC  = "mirahDoc"; // NOI18N
    private static MirahSettings instance;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    

    private MirahSettings() {
    }

    public static synchronized MirahSettings getInstance() {
        if (instance == null) {
            instance = new MirahSettings();
        }
        return instance;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public String getMirahDoc() {
        synchronized (this) {
            return getPreferences().get(MIRAH_DOC, null); // NOI18N
        }
    }

    public void setMirahDoc(String mirahDoc) {
        assert mirahDoc != null;

        String oldValue;
        synchronized (this) {
            oldValue = getMirahDoc();
            getPreferences().put(MIRAH_DOC, mirahDoc);
        }
        propertyChangeSupport.firePropertyChange(MIRAH_DOC_PROPERTY, oldValue, mirahDoc);
    }

    @Override
    @NbBundle.Messages("AdvancedOption_DisplayName_Support=VRuby")
    public String getDisplayName() {
        return "Mirah Support";//AdvancedOption_DisplayName_Support();
    }

    @Override
    @NbBundle.Messages("AdvancedOption_Tooltip_Support=VRuby configuration")
    public String getTooltip() {
        return "VRuby support";//AdvancedOption_Tooltip_Support();
    }

    @Override
    public OptionsPanelController create() {
        return new SupportOptionsPanelController();
    }

    private Preferences getPreferences() {
        return NbPreferences.forModule(MirahSettings.class);
    }

}
