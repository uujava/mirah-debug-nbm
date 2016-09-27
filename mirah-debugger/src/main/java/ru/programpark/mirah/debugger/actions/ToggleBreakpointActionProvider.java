/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package ru.programpark.mirah.debugger.actions;

import org.netbeans.modules.parsing.spi.Parser;
import ru.programpark.mirah.lexer.MirahParserResult;
import ru.programpark.mirah.lexer.SourceQuery;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.spi.debugger.ActionsProvider;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.jpda.EditorContext;
import org.openide.ErrorManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.NbDocument;
import org.openide.util.NbBundle;
import ru.programpark.mirah.debugger.EditorContextBridge;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.netbeans.modules.maven.NbMavenProjectImpl;
//import org.netbeans.modules.maven.api.NbMavenProject;
//import org.netbeans.modules.maven.spi.PackagingProvider;

/**
 * @author Jan Jancura
 * @see debugger.jpda.ui/src/org/netbeans/modules/debugger/jpda/ui/actions/ToggleBreakpointActionProvider.java
 */
@ActionsProvider.Registrations({
        @ActionsProvider.Registration(path = "", actions = {"toggleBreakpoint"}, activateForMIMETypes = {"text/x-vruby"}),
        @ActionsProvider.Registration(path = "netbeans-JPDASession", actions = {"toggleBreakpoint"}, activateForMIMETypes = {"text/x-vruby"})
})
public class ToggleBreakpointActionProvider extends ActionsProviderSupport
        implements PropertyChangeListener {

    private JPDADebugger debugger;
    private static final Logger logger = Logger.getLogger(ToggleBreakpointActionProvider.class.getName());

    public ToggleBreakpointActionProvider() {
        EditorContextBridge.getContext().addPropertyChangeListener(this);
    }

    public ToggleBreakpointActionProvider(ContextProvider lookupProvider) {
        debugger = lookupProvider.lookupFirst(null, JPDADebugger.class);
        debugger.addPropertyChangeListener(JPDADebugger.PROP_STATE, this);
        EditorContextBridge.getContext().addPropertyChangeListener(this);
    }

    private void destroy() {
        debugger.removePropertyChangeListener(JPDADebugger.PROP_STATE, this);
        EditorContextBridge.getContext().removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String url = EditorContextBridge.getContext().getCurrentURL();
        FileObject fo;
        try {
            fo = URLMapper.findFileObject(new URL(url));
        } catch (MalformedURLException muex) {
            fo = null;
        }
        setEnabled(
                ActionsManager.ACTION_TOGGLE_BREAKPOINT,
                (EditorContextBridge.getContext().getCurrentLineNumber() >= 0)
                        && (fo != null && "text/x-vruby".equals(fo.getMIMEType())) // NOI18N
        );
        if (debugger != null
                && debugger.getState() == JPDADebugger.STATE_DISCONNECTED) {
            destroy();
        }
    }

    @Override
    public Set getActions() {
        return Collections.singleton(ActionsManager.ACTION_TOGGLE_BREAKPOINT);
    }

    /*
    public static void putStack(String text) {

        if (LOG == null) return;

        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            LOG.info("" + text + "-> " + ste);
        }
    }
    */
    private static DataObject getDataObject(String url) {
        FileObject file;
        try {
            file = URLMapper.findFileObject(new URL(url));
        } catch (MalformedURLException e) {
            return null;
        }

        if (file == null) {
            return null;
        }
        try {
            return DataObject.find(file);
        } catch (DataObjectNotFoundException ex) {
            return null;
        }
    }

    public StyledDocument getStyledDocument(String url) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return null;
        }
        Source source = Source.create(fo);
        if (source == null) {
            return null;
        }
        if (!"text/x-vruby".equals(fo.getMIMEType())) {
            /**
             * Should return null instead of "" here,
             *
             * @see
             * org.netbeans.modules.debugger.jpda.EditorContextBridge#CompoundContextProvider#getClassName
             * @notice this has been fixed in main's rev: 30100e497ae4
             */
            return null;
        }
        EditorCookie ec = (EditorCookie) dataObject.getLookup().lookup(EditorCookie.class);
        if (ec == null) {
            return null;
        }
        StyledDocument doc;
        try {
            doc = ec.openDocument();
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
            return null;
        }
        return doc;
    }

    public Source getSource(String url) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return null;
        }
        return Source.create(fo);
    }

    //todo сделать через AstPath
    public String[] getClassInfo(String url, final int lineNumber) {
        try {
            //LOG.info("getClassInfo url = " + url);
            final StyledDocument doc = getStyledDocument(url);
            //LOG.info("getClassInfo doc = " + doc);
            Source source = getSource(url);
            if (doc == null) {
                return null;
            }
            final String[] result = new String[]{"", "", ""};
            final int offset = NbDocument.findLineOffset(doc, lineNumber - 1);
            ParserManager.parse(Collections.singleton(source), new UserTask() {
                @Override
                public void run(ResultIterator ri) throws Exception {
                    Parser.Result parserResult = ri.getParserResult();
                    SourceQuery queryDocument = new SourceQuery((MirahParserResult) parserResult);
                    result[0] = queryDocument.findPackage();
                    result[1] = queryDocument.findClassName(offset);
                    result[2] = queryDocument.findMethodName(offset);
                }
            });
            return result;
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return null;
        } catch (IndexOutOfBoundsException ioobex) {
            //XXX: log the exception?
            return null;
        }
    }

    @Override
    public void doAction(Object action) {

        DebuggerManager d = DebuggerManager.getDebuggerManager();

        EditorContext context = EditorContextBridge.getContext();

        // 1) get source name & line number
        int lineNumber = context.getCurrentLineNumber();
        String url = context.getCurrentURL();

        if ("".equals(url.trim())) {
            return;
        }
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "toggle breakpoint in URL = " + url + " lineNumber = " + lineNumber);

        if (lineNumber < 0) return;

        // 2) find and remove existing line breakpoint
        LineBreakpoint lb = findBreakpoint(url, lineNumber);
        if (lb != null) {
            d.removeBreakpoint(lb);
            return;
        }

        String[] classInfo = getClassInfo(url, lineNumber);
        if (classInfo == null) return;

        String packageName = classInfo[0];
//        LOG.info("packageName = " + packageName + " context=" + context);
        String className = classInfo[1];
//        LOG.info("className = " + className + " context=" + context);

        if (className == null) return;

        lb = LineBreakpoint.create(url, lineNumber);

        String preferredClassName = packageName + className;
        if (packageName != null) {
            preferredClassName = packageName + "." + className;
        }
        int lastIndex = url.lastIndexOf('/');
        String sourcePath = lastIndex == -1 ? url : url.substring(lastIndex + 1);
        lb.setSourceName(sourcePath);
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "sourceName = " + sourcePath);
        if (packageName != null) {
            sourcePath = packageName.replace('.', '/') + "/" + sourcePath;
        }
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "sourcePath = " + sourcePath);
        lb.setSourcePath(sourcePath);
        lb.setPreferredClassName(preferredClassName);
        lb.setPrintText(NbBundle.getBundle(ToggleBreakpointActionProvider.class).getString("CTL_Line_Breakpoint_Print_Text"));
        d.addBreakpoint(lb);
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "add breakpoint in url=" + url + " line=" + lineNumber + " preferredClassName=" + preferredClassName);
    }

    @Override
    public void postAction(final Object action, final Runnable actionPerformedNotifier) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doAction(action);
                actionPerformedNotifier.run();
            }
        });
    }

    static LineBreakpoint findBreakpoint(String url, int lineNumber) {
        Breakpoint[] breakpoints = DebuggerManager.getDebuggerManager().getBreakpoints();
        for (int i = 0; i < breakpoints.length; i++) {
            if (!(breakpoints[i] instanceof LineBreakpoint)) {
                continue;
            }
            LineBreakpoint lb = (LineBreakpoint) breakpoints[i];
            if (!lb.getURL().equals(url)) {
                continue;
            }
            if (lb.getLineNumber() == lineNumber) {
                return lb;
            }
        }
        return null;
    }
}
