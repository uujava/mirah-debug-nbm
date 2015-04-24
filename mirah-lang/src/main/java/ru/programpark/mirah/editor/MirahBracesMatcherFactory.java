package ru.programpark.mirah.editor;


import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.BracesMatcherFactory;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;

class MirahBracesMatcherFactory implements BracesMatcherFactory {

    @Override
    public BracesMatcher createMatcher( MatcherContext context )
    {
        return new MirahBracesMatcher(context);
    }
}
