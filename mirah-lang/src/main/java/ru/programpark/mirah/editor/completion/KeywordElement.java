package ru.programpark.mirah.editor.completion;


import org.netbeans.modules.csl.api.ElementKind;


/**
 * Element describing a Groovy keyword
 *
 * @author Tor Norbye
 * @author Gopala Krishnan S
 */
public class KeywordElement extends MirahElement {

    private final String name;

    /** Creates a new instance of DefaultComKeyword */
    public KeywordElement(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.KEYWORD;
    }
}
