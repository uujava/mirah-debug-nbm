/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.netbeans.mirah.lexer.MirahParser.NBMirahParserResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.Call;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.ConstantAssign;
import mirah.lang.ast.ConstructorDefinition;
import mirah.lang.ast.FieldAssign;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.FunctionalCall;
import mirah.lang.ast.Import;
import mirah.lang.ast.InterfaceDeclaration;
import mirah.lang.ast.MacroDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Package;
import mirah.lang.ast.Position;
import mirah.lang.ast.StaticMethodDefinition;
import mirah.lang.ast.Unquote;
import org.mirah.typer.ResolvedType;
import org.mirah.jvm.mirrors.BytecodeMirror;
import org.mirah.jvm.mirrors.DebugError;
import org.mirah.jvm.mirrors.JvmErrorType;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.ElementKind;

/**
 *
 * @author savushkin savushkin@programpark.ru
 */
public class BlockCollector implements BlockNode {

    // При Array.size <= 7 поиск перебором по массиву не медленнее индексированного поиска в Map
    private static final Collection<String> macroNames = new ArrayList<>();

    static {
        macroNames.add("attr_reader");
        macroNames.add("attr_writer");
        macroNames.add("attr_accessor");
        macroNames.add("fx_component");
        macroNames.add("entity_attr");
    }

    List<Block> blockList = new ArrayList<>();
    List<Block> importList = new ArrayList<>();
    List<Block> dslList = new ArrayList<>();
    List<Block> blockComments = new ArrayList<>();
    List<Block> lineComments = new ArrayList<>();
    List<Block> macroList = new ArrayList<>();
    
    boolean importsBlock = false;
    boolean commentsBlock = false;
    boolean macroBlock = false;
    
    
    Block currClass = null;
    
    @Override
    public Block addBlock(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        importsBlock = false;
        commentsBlock = false;
        macroBlock = false;
//        offset = 0;
//        length = 0;
        if (length == 0 && node != null && node.position() != null) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        Block block = new Block(node, function, offset, length, extra, kind);
        blockList.add(block);
        return block;
    }

    @Override
    public Block addDSL(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        importsBlock = false;
        commentsBlock = false;
        macroBlock = false;
//        offset = 0;
//        length = 0;
        if (length == 0 && node != null && node.position() != null) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        Block block = new Block(node, function, offset, length, extra, kind);
        dslList.add(block);
        return block;
    }

    @Override
    public Block addImport(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        commentsBlock = false;
        macroBlock = false;
        
//        offset = 0;
//        length = 0;
        if (length == 0 && node != null && node.position() != null ) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        if (importsBlock && !importList.isEmpty()) {
            Block block = importList.get(importList.size() - 1);
            block.length = offset + length - block.offset;
            return block;
        } else {
            importsBlock = true;
            Block block = new Block(node, function, offset, length, extra, kind);
            importList.add(block);
            return block;
        }
    }
    
    public Block addMacro(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        commentsBlock = false;
        importsBlock = false;
        if (length == 0 && node != null && node.position() != null) {
            offset = node.position().startChar();
            length = node.position().endChar() - node.position().startChar();
        }
        if (macroBlock && !macroList.isEmpty()) {
            Block block = macroList.get(macroList.size() - 1);
            block.length = offset + length - block.offset;
            return block;
        } else {
            macroBlock = true;
            Block block = new Block(node, function, offset, length, extra, kind);
            macroList.add(block);
            return block;
        }
    }

    public Block addBlockComment(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        // комментарии не прерывают другие блоки, в т.ч. блоки импорта и блоки макросов атрибутики
        commentsBlock = false;
        Block block = new Block(node, function, offset, length, extra, kind);
        blockComments.add(block);
        return block;
    }

    public Block addLineComment(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
        // комментарии не прерывают блоков импорта
        if (commentsBlock && !lineComments.isEmpty()) {
            Block block = lineComments.get(lineComments.size() - 1);
            block.length = offset + length - block.offset;
            return block;
        } else {
            commentsBlock = true;
            Block block = new Block(node, function, offset, length, extra, kind);
            lineComments.add(block);
            return block;
        }
    }

    public void prepareBlocks( final NBMirahParserResult parsed ) {
        if ( parsed.getRoot() == null ) return;
        
        
        final LinkedList<Block> blockStack = new LinkedList<>();
        final BlockCollector self = this;
        try {
        parsed.getRoot().accept(new NodeScanner() {

            @Override
            public boolean enterImport(Import node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                blockStack.push(parent.addImport(node, node.fullName().identifier(), 0, 0, "", ElementKind.OTHER));
//                blockStack.push(parent.addImport(node, node.fullName().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.OTHER));
                return super.enterImport(node, arg);
            }

            @Override
            public Object exitImport(Import node, Object arg) {
                blockStack.pop();
                return super.exitImport(node, arg);
            }

            @Override
            public boolean enterBlock(mirah.lang.ast.Block node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                blockStack.push(parent.addDSL(node, "DSL",  0, 0, "", ElementKind.METHOD));
                return super.enterBlock(node, arg);
            }

            @Override
            public Object exitBlock(mirah.lang.ast.Block node, Object arg) {
                blockStack.pop();
                return super.exitBlock(node, arg);
            }

            @Override
            public boolean enterPackage(Package node, Object arg) {
                if (!blockStack.isEmpty()) {
                    blockStack.pop();
                }
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.PACKAGE));
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.PACKAGE));
                return super.enterPackage(node, arg);
            }

            @Override
            public boolean enterClassDefinition(ClassDefinition node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                currClass = parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.CLASS);
                blockStack.push(currClass);
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CLASS));
                return super.enterClassDefinition(node, arg);
            }

            @Override
            public Object exitClassDefinition(ClassDefinition node, Object arg) {
                blockStack.pop();
                currClass = null;
                return super.exitClassDefinition(node, arg);
            }

            @Override
            public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
//                if (node.name() instanceof Unquote) {
//                    return false;
//                }
                // это фейковый метод из макроса
                if ( node.originalNode() != null ) {
                    // узел не создается, но дочерние поля просматриваются
                    int t = 0;
                }
                else {
//                    System.out.println("enterMethodDefinition node=" + node);
                    BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                    blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.METHOD));
                }
                return super.enterMethodDefinition(node, arg);
            }

            @Override
            public Object exitMethodDefinition(MethodDefinition node, Object arg) {
//                if (!(node.name() instanceof Unquote)) {
                if (node.originalNode() == null) {
                    blockStack.pop();
                }
                return super.exitMethodDefinition(node, arg);
            }

            @Override
            public boolean enterConstructorDefinition(ConstructorDefinition node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.CONSTRUCTOR));
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CONSTRUCTOR));
                return super.enterConstructorDefinition(node, arg);
            }

            @Override
            public Object exitConstructorDefinition(ConstructorDefinition node, Object arg) {
                blockStack.pop();
                return super.exitConstructorDefinition(node, arg);
            }

            @Override
            public boolean enterInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
                blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.INTERFACE));
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.INTERFACE));
                return super.enterInterfaceDeclaration(node, arg);
            }

            @Override
            public Object exitInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
                blockStack.pop();
                return super.exitInterfaceDeclaration(node, arg);
            }

            @Override
            public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.METHOD));
                blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.METHOD));
                return super.enterStaticMethodDefinition(node, arg);
            }

            @Override
            public Object exitStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
                blockStack.pop();
                return super.exitStaticMethodDefinition(node, arg);
            }

            @Override
            public boolean enterFieldAssign(FieldAssign node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.FIELD));
                Object type = parsed.getResolvedTypes().get(node);
                String name = "";
                //todo разобраться с неизвестными типами :error
                if ( type instanceof BytecodeMirror ) name = ((BytecodeMirror) type).name(); //((BytecodeMirror)type).getAsmType().getClassName();
                else
                if ( type instanceof DebugError ) name = ((DebugError) type).name(); //((DebugError)type).toString();
                else
                if ( type instanceof JvmErrorType ) name = ((JvmErrorType)type).getAsmType().getClassName();
                else
                if ( type instanceof ResolvedType ) name = ((ResolvedType)type).name();
                if ( node.originalNode() != null ) {
                    Position pos = node.originalNode().position();
                    Block newBlock = currClass.addBlock(node, "@"+node.name().identifier(), pos.startChar(), pos.endChar() - pos.startChar(), name, ElementKind.FIELD);
                    blockStack.push(newBlock);
                }
                else {
                    blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, name, ElementKind.FIELD));
                }
                return super.enterFieldAssign(node, arg);
            }

            @Override
            public Object exitFieldAssign(FieldAssign node, Object arg) {
                blockStack.pop();
                return super.exitFieldAssign(node, arg);
            }

            @Override
            public boolean enterConstantAssign(ConstantAssign node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CONSTANT));
                blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.CONSTANT));
                return super.enterConstantAssign(node, arg);
            }

            @Override
            public Object exitConstantAssign(ConstantAssign node, Object arg) {
                blockStack.pop();
                return super.exitConstantAssign(node, arg);
            }

            @Override
            public boolean enterMacroDefinition(MacroDefinition node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.METHOD));
                blockStack.push(parent.addBlock(node, node.name().identifier(), 0, 0, "", ElementKind.METHOD));
                return super.enterMacroDefinition(node, arg);
            }

            @Override
            public Object exitMacroDefinition(MacroDefinition node, Object arg) {
                blockStack.pop();
                return super.exitMacroDefinition(node, arg);
            }

            @Override
            public boolean enterCall(Call node, Object arg) {
                final String identifier = node.name().identifier();
                if (macroNames.contains(identifier)) {
                    blockStack.push(self.addMacro(node, identifier, 0, 0, "", ElementKind.CALL));
                } else {
                    self.macroBlock = false;
                }
                return super.enterCall(node, arg);
            }

            @Override
            public Object exitCall(Call node, Object arg) {
                final String identifier = node.name().identifier();
                if (macroNames.contains(identifier)) {
                    blockStack.pop();
                }
                return super.exitCall(node, arg);
            }

            @Override
            public boolean enterFunctionalCall(FunctionalCall node, Object arg) {
                final String identifier = node.name().identifier();
                if (macroNames.contains(identifier)) {
                    blockStack.push(self.addMacro(node, identifier, 0, 00, "", ElementKind.CALL));
                } else {
                    self.macroBlock = false;
                }
                return super.enterFunctionalCall(node, arg);
            }

            @Override
            public Object exitFunctionalCall(FunctionalCall node, Object arg) {
                final String identifier = node.name().identifier();
                if (macroNames.contains(identifier)) {
                    blockStack.pop();
                }
                return super.exitFunctionalCall(node, arg);
            }

            @Override
            public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
                final BlockNode parent = blockStack.isEmpty() ? self : blockStack.peek();
//                blockStack.push(parent.addBlock(node, "@"+node.name().identifier(), 0, 0, "", ElementKind.FIELD));
//                blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.FIELD));
                return super.enterFieldDeclaration(node, arg);
            }

            @Override
            public Object exitFieldDeclaration(FieldDeclaration node, Object arg) {
//                blockStack.pop();
                return super.exitFieldDeclaration(node, arg);
            }

            @Override
            public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
                return false;
            }
            
        }, null);
        } catch( Exception ee ) {
            System.out.println("prepareBlocks EXCEPTION = "+ee);
            ee.printStackTrace();
        }
        final Document doc = parsed.getSnapshot().getSource().getDocument(false);
        if (doc == null) {
//            LOG.info(this,"doc = null file="+res.getSnapshot().getSource().getFileObject().getPath());
            return;
        }
        final DocumentQuery dq = new DocumentQuery(doc);
        if (dq == null) {
//            LOG.info(this,"dq = null file="+res.getSnapshot().getSource().getFileObject().getPath());
            return;
        }
        final TokenSequence<MirahTokenId> seq = dq.getTokens(0, false);
//        final MirahTokenId tComment = MirahTokenId.get(Tokens.tComment);
//        final MirahTokenId tNL = MirahTokenId.get(Tokens.tNL);
        do {
            final MirahTokenId tokenId = seq.token().id();
            if (!tokenId.is(Tokens.tComment) && !tokenId.is(Tokens.tJavaDoc)) {
                if (!tokenId.is(Tokens.tNL)) {
                    commentsBlock = false;
                }
                continue;
            }
            // К сожалению, это не работает
            // boolean inJavadoc = seq.token().id().ordinal() == Tokens.tJavaDoc.ordinal();
            String comment = seq.token().text().toString().trim();
            if (comment.startsWith("/*") && comment.endsWith("*/")) {
                addBlockComment(null, "BlockComment", seq.offset(), seq.token().length(), "", ElementKind.DB);
            } else if (comment.startsWith("#")) {
                addLineComment(null, "LineComment", seq.offset(), seq.token().length(), "", ElementKind.TAG);
            }
        } while (seq.moveNext());
    }
    public List<Block> getBlocks() {
        return blockList;
    }
    public List<Block> getImports() {
        return importList;
    }

    public List<Block> getDSLs() {
        return dslList;
    }

    public List<Block> getMacroses() {
        return macroList;
    }

    public List<Block> getLineComments() {
        return lineComments;
    }

    public List<Block> getBlockComments() {
        return blockComments;
    }

}
