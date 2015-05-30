package ca.weblite.netbeans.mirah.lexer;

import mirah.lang.ast.Node;
import org.netbeans.modules.csl.api.ElementKind;

/**
 *
 * @author Markov, markovs@programpark.ru
 * @Created on 30.05.2015, 08:58
 */
interface BlockNode {

    Block addBlock(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind);
}
