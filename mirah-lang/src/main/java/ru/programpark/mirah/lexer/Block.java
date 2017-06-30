package ru.programpark.mirah.lexer;

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
    List<Block> blocks = new ArrayList<>();
    List<Block> imports = new ArrayList<>();
    boolean importsBlock = false;

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
        importsBlock = false;
        if (length == 0 && node != null && node.position() != null) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        Block block = new Block(node, function, offset, length, extra, kind);
        children.add(block);
        return block;
    }

    @Override
    public Block addDSL(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        importsBlock = false;
        if (length == 0 && node != null && node.position() != null) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        Block block = new Block(node, function, offset, length, extra, kind);
        blocks.add(block);
        return block;
    }

    @Override
    public Block addImport(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        if (length == 0 && node != null && node.position() != null) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        if (importsBlock && !imports.isEmpty()) {
            Block block = imports.get(imports.size() - 1);
            block.length = offset + length - block.offset;
            return block;
        } else {
            importsBlock = true;
            Block block = new Block(node, function, offset, length, extra, kind);
            imports.add(block);
            return block;
        }
    }

    public List<Block> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public List<Block> getImports() {
        return Collections.unmodifiableList(imports);
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
