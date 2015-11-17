package ru.programpark.mirah.editor.navigator;

import ca.weblite.netbeans.mirah.lexer.Block;
import ca.weblite.netbeans.mirah.lexer.BlockCollector;
import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahParser.NBMirahParserResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.Document;

import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.ParserResult;

/**
 * Given a parse tree, scan its structure and produce a flat list of structure items suitable for display in a navigator
 * / outline / structure view
 *
 * @author shannah
 */
public class MirahStructureAnalyzer implements StructureScanner {

    @Override
    public List<? extends StructureItem> scan(ParserResult pr) {
        NBMirahParserResult res = (NBMirahParserResult) pr;
        
        //список блоков в MirahParser формируется после парсинга файла mirah, до подстановки макросов и инферинга,
        // поэтому не обрабатываются поля, заданные макросами fx_attr и др
        //todo - наверное, надо добавить обработку блоков в нужном месте MirahParser
        // пока сделано в классе BlockCollector
//        BlockCollector coll = new BlockCollector();
//        coll.prepareBlocks(res);

        BlockCollector coll = res.getBlockCollection();
        ArrayList<StructureItem> out = new ArrayList<>();
//        for (Block block : res.getBlocks()) {
        for (Block block : coll.getBlocks()) {
            out.add(new MirahStructureItem(res.getSnapshot().getSource(), block));
        }
        return out;
    }

    /**
     * Compute a list of foldable regions, named "codeblocks", "comments", "imports", "initial-comment", ... The
     * returned Map must be keyed by {@link FoldType#code}. For backward compatibility, the following tokens are
     * temporarily supported although no FoldType is registered explicitly.
     * <ul>
     * <li>codeblocks
     * <li>comments
     * <li>initial-comment
     * <li>imports
     * <li>tags
     * <li>inner-classes
     * <li>othercodeblocks
     * </ul>
     * This additional support will cease to exist after NB-8.0. Language owners are required to register their
     * {@link FoldTypeProvider} and define their own folding.
     */
    @Override
    public Map<String, List<OffsetRange>> folds(ParserResult pr) {
        Map<String, List<OffsetRange>> out = new HashMap<>();
        NBMirahParserResult res = (NBMirahParserResult) pr;

//        BlockCollector coll = new BlockCollector();
//        coll.prepareBlocks(res);
        BlockCollector coll = res.getBlockCollection();
        
        // узел PACKAGE почему-то сделан родительским узлом всех блоков
//        for (Block block : res.getBlocks()) {
        for (Block block : coll.getBlocks()) {
            folds(block, null, out, pr);
        }
//        for (Block block : res.getImports()) {
        for (Block block : coll.getImports()) {
            folds(block, null, out, pr);
        }
        for (Block block : coll.getLineComments()) {
//        for (Block block : res.getLineComments()) {
            folds(block, null, out, pr);
        }
        for (Block block : coll.getBlockComments()) {
//        for (Block block : res.getBlockComments()) {
            folds(block, null, out, pr);
        }
//        for (Block block : res.getMacroses()) {
        for (Block block : coll.getMacroses()) {
            folds(block, null, out, pr);
        }
        return out;
    }

    /**
     * Первую строку того, что заворачиваем, оставляем видимой. todo: методы и классы с аннотациями - первой строкой
     * метода считается аннотация, которая может быть в отдельной строке.
     *
     * @param block
     * @param parent приходится передавать также родителя, т.к. у потомка нет ссылки на родительский блок.
     * @param out
     * @return
     */
    private void folds(Block block, Block parent, Map<String, List<OffsetRange>> out, ParserResult pr) {
        List<OffsetRange> ranges = getRanges(block, parent, out);
        if (ranges != null) {
            final Document doc = pr.getSnapshot().getSource().getDocument(false);
            DocumentQuery dq = new DocumentQuery(doc);
            int eol = dq.getEOL(block.getOffset());
            if (eol <= block.getOffset() + block.getLength()) {
                ranges.add(new OffsetRange(eol, block.getOffset() + block.getLength()));
            }
        }
        for (Block child : block.getChildren()) {
            folds(child, block, out, pr);
        }
        for (Block child : block.getImports()) {
            folds(child, block, out, pr);
        }
        // Фолдинг для блоков do end и { }
        if (block.getKind() == ElementKind.METHOD) {
            for (Block child : block.getBlocks()) {
                folds(child, block, out, pr);
            }
        }
    }

    /**
     *
     * @param block
     * @param parent приходится передавать также родителя, т.к. у потомка нет ссылки на родительский блок.
     * @param out
     * @return
     */
    private List<OffsetRange> getRanges(Block block, Block parent, Map<String, List<OffsetRange>> out) {
        // Р”РёР°РїР°Р·РѕРЅС‹ СЃРѕС…СЂР°РЅСЏРµРј С‚РѕР»СЊРєРѕ РґР»СЏ РѕРїСЂРµРґРµР»РµРЅРЅС‹С… С‚РёРїРѕРІ Р±Р»РѕРєРѕРІ
        switch (block.getKind()) {
            case CLASS:
            case INTERFACE:
                if (parent.getKind() == ElementKind.PACKAGE) {
                    // Классы верхнего уровня не фолдим, чтобы не перегружать интерфейс
                    return null;
                } else {
                    return getRanges("inner-classes", out);
                }
            case CONSTRUCTOR:
            case METHOD:
                return getRanges("codeblocks", out);
            case OTHER:
                return getRanges("imports", out);
            case TAG:
                // Однострочные комментарии в стиле Ruby, начинающиеся с #
                return getRanges("comments", out);
            case DB:
                // Комментарии в стиле Java, начинающиеся с /* и /**
                return getRanges("initial-comment", out);
            case CALL:
                // Различные макросы, типа attr_accessor
                return getRanges("othercodeblocks", out);
            default:
                return null;
        }
    }

    private List<OffsetRange> getRanges(String what, Map<String, List<OffsetRange>> out) {
        List<OffsetRange> ranges = out.get(what);
        if (ranges == null) {
            ranges = new ArrayList<>();
            out.put(what, ranges);
        }
        return ranges;
    }

    @Override
    public Configuration getConfiguration() {
        return new Configuration(true, true);
    }
}
