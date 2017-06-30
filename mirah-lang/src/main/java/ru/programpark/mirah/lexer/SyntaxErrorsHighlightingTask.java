/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.lexer;

import ru.programpark.mirah.ImportFixList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Document;

import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shannah
 */
class SyntaxErrorsHighlightingTask extends ParserResultTask {

    public SyntaxErrorsHighlightingTask() {
       
    }

    @Override
    public void run(Parser.Result t, SchedulerEvent se) {
        MirahParserResult result = (MirahParserResult) t;
        
        if ( result == null || result.getDiagnostics().isEmpty() ){
            return;
        }
//        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "run diagnostics="+result.getMirahDiagnostics()+" errors="+result.getMirahDiagnostics().getErrors());
        
        List<ParserError> syntaxErrors = result.getDiagnostics();
        Snapshot snapshot = result.getSnapshot();
        if ( snapshot == null ){
            return;
        }
        Source source = snapshot.getSource();
        if ( source == null ){
            return;
        }
        
        Document document = source.getDocument(false);
        if ( document == null ){
            return;
        }
        List<ErrorDescription> errors = new ArrayList<>();
        for (ParserError syntaxError : syntaxErrors) {
            String message = syntaxError.description;
            int line = syntaxError.line;

            if (line <= 0) {
                continue;
            }

            ErrorDescription errorDescription = null;
            if ( message.toLowerCase().contains("cannot find class")){
                //List<Fix> imports = new ArrayList<Fix>();
                Pattern p = Pattern.compile("cannot find class ([a-zA-Z][a-zA-Z0-9\\.\\$]*)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(message);
                
                if ( m.find() ){
                    
                    String className = m.group(1);
                    if ( className.indexOf(".") != -1 ){
                        int pos = className.lastIndexOf(".");
                        if ( pos < className.length()-1 ){
                            pos++;
                        }
                        className = className.substring(pos);
                        
                    }
                    
                    ImportFixList importFixes = new ImportFixList(source, className);
                    errorDescription = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        message,
                        importFixes,
                        document,
                        line
                    );

                    RequestProcessor rp = new RequestProcessor(SyntaxErrorsHighlightingTask.class);
                    
                    rp.submit(importFixes);
                    

                }
            }
                
            if ( errorDescription == null ){
                errorDescription = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        message, 
                        document,
                        line);
            }
            
            errors.add(errorDescription);
        }
        
        CodeHintsTask codeHints = new CodeHintsTask();
        codeHints.run(t, se);
        errors.addAll(codeHints.getErrors());
        HintsController.setErrors(document, "vrb", errors);
        
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {

    }


}
