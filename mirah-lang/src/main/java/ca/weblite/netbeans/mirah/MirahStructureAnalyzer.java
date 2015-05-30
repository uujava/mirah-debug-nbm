package ca.weblite.netbeans.mirah;

import ca.weblite.netbeans.mirah.lexer.Block;
import ca.weblite.netbeans.mirah.lexer.MirahParser.NBMirahParserResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        ArrayList<StructureItem> out = new ArrayList<>();
        for (Block block : res.getBlocks()) {
            out.add(new MirahStructureItem(res.getSnapshot(), block));
        }
        return out;
    }

    @Override
    public Map<String, List<OffsetRange>> folds(ParserResult pr) {
        Map<String, List<OffsetRange>> out = new HashMap<>();
        NBMirahParserResult res = (NBMirahParserResult) pr;
        ArrayList<OffsetRange> ranges = new ArrayList<>();
        for (Block block : res.getBlocks()) {
            LOG.info(this, "block = " + block);
            // todo узел PACKAGE почему-то сделан родительским узлом всех блоков. из-за этого не работает фолдинг. 
            // Либо исправить размер, чтобы он охватывал все подчиненные узлы, либо убрать из списка блоков. разобраться
            if (block.getKind() == ElementKind.PACKAGE) {
                for (Block child : block.getChildren()) {
                    ranges.add(new OffsetRange(child.getOffset(), child.getOffset() + child.getLength()));
                }
            } else {
                ranges.add(new OffsetRange(block.getOffset(), block.getOffset() + block.getLength()));
            }
        }
        out.put("codeblocks", ranges);
        return out;
    }

    @Override
    public Configuration getConfiguration() {
        return new Configuration(true, true);
    }
}
