package ru.programpark.mirah.lexer;

import mirah.lang.ast.Import;
import mirah.lang.ast.NodeScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kozyr on 20.06.2016.
 * TODO handle method and macro level imports
 */
public class ImportScanner extends NodeScanner {
    List<String> found = new ArrayList<>();

    public ImportScanner() {
    }

    @Override
    public boolean enterImport(Import node, Object arg) {
        found.add(node.fullName().identifier());
        return false;
    }

}
