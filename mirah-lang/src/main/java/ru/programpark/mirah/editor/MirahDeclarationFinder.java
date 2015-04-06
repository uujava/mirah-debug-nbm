/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.programpark.mirah.editor;

import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;

/**
 *
 * @author savushkin
 */
public class MirahDeclarationFinder implements DeclarationFinder {

//    static Logger LOG = Logger.getLogger(MirahDeclarationFinder.class.getCanonicalName());
    
    @Override
    public DeclarationLocation findDeclaration(ParserResult pr, int i) {
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OffsetRange getReferenceSpan(Document dcmnt, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
