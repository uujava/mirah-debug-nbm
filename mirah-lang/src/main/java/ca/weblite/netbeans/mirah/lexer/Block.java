package ca.weblite.netbeans.mirah.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mirah.lang.ast.Node;
import org.netbeans.modules.csl.api.ElementKind;

/**
 *
 * @author Markov, markovs@programpark.ru
 * @Created on 30.05.2015, 09:02
 */
public class Block implements BlockNode {
    ElementKind kind;
    CharSequence function;
    int offset;
    int length;
    CharSequence extra;
    List<Block> children = new ArrayList<>();
    Node node;

    public Block(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        this.node = node;
        this.function = function;
        this.offset = offset;
        this.length = length;
        this.extra = extra;
        this.kind = kind;
    }

    @Override
    public Block addBlock(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        Block block = new Block(node, function, offset, length, extra, kind);
        children.add(block);
        return block;
    }

    public List<Block> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public CharSequence getExtra() {
        return extra;
    }

    public CharSequence getDescription() {
        return function;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public ElementKind getKind() {
        return kind;
    }

    public Node getNode() {
        return node;
    }
}
