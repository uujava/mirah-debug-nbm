/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.vector.script.actions;

import java.awt.event.ActionEvent;
import java.util.concurrent.Future;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.util.ImageUtilities;

public final class StopAction extends AbstractAction {

    private Future<?> task;
    private Thread thread;

    public StopAction() {
        setEnabled(false); // initially, until ready
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("ru/programpark/vector/script/stop.png", false)); // NOI18N
        putValue(Action.SHORT_DESCRIPTION, "Stop script!");
        thread = null;
    }

    public void setTask(Future<?> task) {
        synchronized (this) {
            this.task = task;
        }
    }
    
    public void setThread(Thread thread) {
        synchronized (this) {
            this.thread = thread;
        }
    }

    public void actionPerformed(ActionEvent e) {
        setEnabled(false); // discourage repeated clicking

//        Future<?> actionTask;
//        synchronized (this) {
//            actionTask = task;
//        }
//
//        if (actionTask != null) {
//            actionTask.cancel(true);
//        }
        Thread actionThread;
        synchronized (this) {
            actionThread = thread;
        }
        
        try {
            if (actionThread != null) {
                actionThread.interrupt();
            }
        } finally { // final state
//            UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
        }
        
    }

}
