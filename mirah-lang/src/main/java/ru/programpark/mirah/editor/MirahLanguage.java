/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package ru.programpark.mirah.editor;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.MirahStructureAnalyzer;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.Set;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.lexer.Language;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.InstantRenamer;
import org.netbeans.modules.csl.api.KeystrokeHandler;
import org.netbeans.modules.csl.api.OccurrencesFinder;
import org.netbeans.modules.csl.api.SemanticAnalyzer;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.csl.spi.LanguageRegistration;
import org.netbeans.modules.parsing.spi.indexing.PathRecognizerRegistration;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexerFactory;
import org.openide.filesystems.MIMEResolver;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import ru.programpark.mirah.index.MirahIndexer;
import ru.programpark.mirah.editor.api.completion.CompletionHandler;

/**
 * Language/lexing configuration for Mirah
 *
 *
 * @Note: Since NetBeans 6.9, csl uses LanguageRegistration, and process source
 * files to auto-generate layer, What I have to do is either use manually
 * register all relative instance in layer.xml or write it in Java.
 *
 * @see org.netbeans.modules.csl.core.LanguageRegistrationProcessor
 * @see https://netbeans.org/bugzilla/show_bug.cgi?id=169991 What
 * LanguageRegistrationProcessor created is under
 * build/classes/META-INF/generated-layer.xml
 *
 * @author Caoyuan Deng
 */
@MIMEResolver.ExtensionRegistration(
    mimeType = "text/x-mirah",
    displayName = "#LBL_MirahEditorTab",
    extension = "mirah",
    position = 281
)
@LanguageRegistration(
    mimeType = "text/x-mirah",
    useMultiview = true
)
@PathRecognizerRegistration(
        mimeTypes = "text/x-mirah",
        sourcePathIds = ClassPath.SOURCE,
//        libraryPathIds = ClassPath.BOOT,
        libraryPathIds = {},
        binaryLibraryPathIds = {}
)
public class MirahLanguage extends DefaultLanguageConfig {

    @MultiViewElement.Registration(displayName = "#LBL_MirahEditorTab",
    iconBase = "ca/weblite/netbeans/mirah/1391571312_application-x-ruby.png",
    persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
    preferredID = "mirah.source",
    mimeType = "text/x-mirah",
    position = 1)

    public static MultiViewEditorElement createMultiViewEditorElement(Lookup context) {
        return new MultiViewEditorElement(context);
    }

    public MirahLanguage() {
    }

    @Override
    public Language getLexerLanguage() {
        return MirahTokenId.getLanguage();
    }

    @Override
    public String getLineCommentPrefix() {
        return "//"; // NOI18N
    }

    @Override
    public String getDisplayName() {
        return "Mirah"; // NOI18N
    }

    @Override
    public String getPreferredExtension() {
        return "mirah"; // NOI18N
    }

    /**
     * @see org.netbeans.modules.scala.platform.MirahPlatformClassPathProvider
     * and ModuleInstall
     */
    @Override
    public Set<String> getLibraryPathIds() {
        return java.util.Collections.singleton(ClassPath.BOOT);
    }

    @Override
    public Set<String> getSourcePathIds() {
        return java.util.Collections.singleton(ClassPath.SOURCE);
    }

    @Override
    public Parser getParser() {
//        LOG.info(this,"--- getParser ---");
        return new MirahParser();
    }

    @Override
    public SemanticAnalyzer getSemanticAnalyzer() {
//        LOG.info(this,"--- getSemanticAnalyzer ---");
        return null; //new MirahSemanticAnalyzer();
    }

    @Override
    public boolean hasOccurrencesFinder() {
        return true;
    }

    @Override
    public OccurrencesFinder getOccurrencesFinder() {
//        LOG.info(this,"--- getOccurrencesFinder ---");
        return null; //new MirahOccurrencesFinder();
    }

    @Override
    public boolean hasStructureScanner() {
        return true;
    }

    @Override
    public StructureScanner getStructureScanner() {
//        LOG.info(this,"--- getStructureScanner ---");
        //LOG.putStack(null);
        return new MirahStructureAnalyzer();
    }

    @CheckForNull
    public EmbeddingIndexerFactory getIndexerFactory() {
        
//        LOG.info(this, "--- getIndexerFactory ---" );
//        LOG.putStack(null);
        return new MirahIndexer.Factory();
    }

    @Override
    public DeclarationFinder getDeclarationFinder() {
//        LOG.info(this,"--- getDeclarationFinder ---");
//        LOG.putStack(null);
//        return new MirahDeclarationFinder();
        return null;
    }

    @Override
    public InstantRenamer getInstantRenamer() {
//        LOG.info(this,"--- getInstantRenamer ---");
        return null; //new MirahInstantRenamer();
    }

    @Override
    public CodeCompletionHandler getCompletionHandler() {
//        LOG.info(this,"--- getCompletionHandler ---");
//        return null; //new MirahCodeCompletionHandler();
        return new CompletionHandler();
    }

    @Override
    public KeystrokeHandler getKeystrokeHandler() {
//        LOG.info(this,"--- getKeystrokeHandler ---");
        return null; //new MirahKeystrokeHandler();
    }

    @Override
    public boolean hasFormatter() {
        return true;
    }

    @Override
    public Formatter getFormatter() {
//        LOG.info(this,"--- getFormatter ---");
        return null; //new MirahFormatter();
    }

    @Override
    public boolean hasHintsProvider() {
        return true;
    }

    // hintsProvider is registered in layer.xml under "csl-hints" folder
    @Override
    public HintsProvider getHintsProvider() {
//        LOG.info(this,"--- getHintsProvider ---");
//        LOG.putStack(null);
        return null; //new MirahHintsProvider();
    }
//    @Override def getIndexerFactory = new MirahIndexer.Factory
}
