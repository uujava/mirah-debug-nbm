/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.debugger;

import java.util.logging.Logger;

/**
 *
 * @author savushkin
 */
public class LOG {

    static Logger logger = System.getProperty("mirah.logger") != null ? Logger.getLogger(LOG.class.getCanonicalName()) : null;

    public static void info(Object o, String text) {
        if (logger == null) return;

        String name = o.getClass().getSimpleName();
        if (o instanceof Class ) name = ((Class) o).getSimpleName();
        if (o instanceof String) name = (String) o;
        logger.info("" + name + ":" + text);
    }

    public static void putStack(String text) {
        if (logger == null) return;
        int i = 0;
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
           if ( ++i > 2 ) logger.info("> " + ste);
        }
    }
    
}
