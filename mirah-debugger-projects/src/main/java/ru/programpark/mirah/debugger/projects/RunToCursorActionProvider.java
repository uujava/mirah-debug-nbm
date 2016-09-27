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

package ru.programpark.mirah.debugger.projects;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.debugger.ActionsManager;


import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;

import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.project.Project;
import org.netbeans.spi.debugger.ActionsProvider.Registration;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.netbeans.spi.project.ActionProvider;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;


/**
*
* @author   Jan Jancura
*/
@Registration(actions={"runToCursor"}, activateForMIMETypes={"text/x-vruby"})
public class RunToCursorActionProvider extends ActionsProviderSupport {
    private static final Logger logger = Logger.getLogger(RunToCursorActionProvider.class.getName());
    private EditorContextDispatcher editorContext;
    private LineBreakpoint          breakpoint;
    private static RequestProcessor RP = new RequestProcessor(RunToCursorActionProvider.class.getName());
    
    {
        editorContext = EditorContextDispatcher.getDefault();
        Listener listener = new Listener ();
        MainProjectManager.getDefault ().addPropertyChangeListener (listener);
        editorContext.addPropertyChangeListener("text/x-vruby",
                WeakListeners.propertyChange(listener, editorContext));
        DebuggerManager.getDebuggerManager ().addDebuggerListener (
            DebuggerManager.PROP_DEBUGGER_ENGINES,
            listener
        );

        setEnabled (
            ActionsManager.ACTION_RUN_TO_CURSOR,
            shouldBeEnabled ()
        );
    }
    
    @Override
    public Set getActions () {
        return Collections.singleton (ActionsManager.ACTION_RUN_TO_CURSOR);
    }
    
    @Override
    public void doAction (Object action) {
      
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "doAction =" + action);
        
        // 1) set breakpoint
        removeBreakpoint ();
        createBreakpoint (LineBreakpoint.create (
            editorContext.getCurrentURLAsString(),
            editorContext.getCurrentLineNumber ()
        ));
        
        // 2) start debugging of project
        invokeAction();
    }
    
    @Override
    public void postAction(Object action, final Runnable actionPerformedNotifier) {
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "postAction =" + action);
        final LineBreakpoint newBreakpoint = LineBreakpoint.create (
            editorContext.getCurrentURLAsString(),
            editorContext.getCurrentLineNumber ()
        );
        RP.post(new Runnable() {
            @Override
            public void run() {
                // 1) set breakpoint
                removeBreakpoint ();
                createBreakpoint (newBreakpoint);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            invokeAction();
                        }
                    });
                } catch (InterruptedException iex) {
                    // Procceed
                } catch (java.lang.reflect.InvocationTargetException itex) {
                    ErrorManager.getDefault().notify(itex);
                } finally {
                    actionPerformedNotifier.run();
                }
            }
        });
    }
    
    private void invokeAction() {
        debugProject(MainProjectManager.getDefault ().getMainProject ());
    }

    private static void debugProject(Project p) {
        p.getLookup ().lookup(ActionProvider.class).invokeAction (
                ActionProvider.COMMAND_DEBUG,
                p.getLookup ()
            );
    }

    private boolean shouldBeEnabled () {
        
        if ( true ) return true;
        
        if (editorContext.getCurrentLineNumber () < 0) {
            return false;
        }
        FileObject fo = editorContext.getCurrentFile();
        if (fo == null || !fo.hasExt("mirah")) {
            return false;
        }
        
        // check if current project supports this action
        Project p = MainProjectManager.getDefault ().getMainProject ();
        if (p == null) {
            return false;
        }
        ActionProvider actionProvider = (ActionProvider) p.getLookup ().
            lookup (ActionProvider.class);
        if (actionProvider == null) {
            return false;
        }
        String[] sa = actionProvider.getSupportedActions ();
        int i, k = sa.length;
        for (i = 0; i < k; i++) {
            if (ActionProvider.COMMAND_DEBUG.equals (sa [i])) {
                break;
            }
        }
        if (i == k) {
            return false;
        }

        // check if this action should be enabled
        return actionProvider.isActionEnabled (
                ActionProvider.COMMAND_DEBUG, 
                p.getLookup ()
            );
    }
    
    private void createBreakpoint (LineBreakpoint breakpoint) {
//        LOG.info("createBreakpoint breakpoint=" + breakpoint);
        
        breakpoint.setHidden (true);
        DebuggerManager.getDebuggerManager ().addBreakpoint (breakpoint);
        this.breakpoint = breakpoint;
    }
    
    private void removeBreakpoint () {
//        LOG.info("removeBreakpoint breakpoint=" + breakpoint);
        
        if (breakpoint != null) {
            DebuggerManager.getDebuggerManager ().removeBreakpoint (breakpoint);
            breakpoint = null;
        }
    }
    
    private class Listener extends DebuggerManagerAdapter {
        @Override
        public void propertyChange (PropertyChangeEvent e) {
            
//            LOG.info("propertyChange e=" + e);
            
            if (e.getPropertyName () == JPDADebugger.PROP_STATE) {
                int state = ((Integer) e.getNewValue ()).intValue ();
                if ( (state == JPDADebugger.STATE_DISCONNECTED) ||
                     (state == JPDADebugger.STATE_STOPPED)
                ) {
                    removeBreakpoint ();
                }
                return;
            }
            setEnabled (
                ActionsManager.ACTION_RUN_TO_CURSOR,
                shouldBeEnabled ()
            );
        }
        
        @Override
        public void engineAdded (DebuggerEngine engine) {
            JPDADebugger debugger = engine.lookupFirst(null, JPDADebugger.class);
//            LOG.info("engineAdded debugger=" + debugger);
            if (debugger == null) {
                return;
            }
            debugger.addPropertyChangeListener (
                JPDADebugger.PROP_STATE,
                this
            );
        }
        
        @Override
        public void engineRemoved (DebuggerEngine engine) {
            JPDADebugger debugger = engine.lookupFirst(null, JPDADebugger.class);
//            LOG.info("engineRemoved debugger=" + debugger);
            if (debugger == null) {
                return;
            }
            debugger.removePropertyChangeListener (
                JPDADebugger.PROP_STATE,
                this
            );
        }
    }
}
