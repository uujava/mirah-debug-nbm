package ru.programpark.mirah.editor.completion;

/**
 *
 * @author schmidtm
 */
public enum KeywordCategory {

    KEYWORD("keyword"),

    PRIMITIVE("primitive"),

    MODIFIER("modifier"),

    ANY("any"),

    NONE("none");

    private final String category; // keyword, primitive, modifier

    private KeywordCategory(String category) {
        this.category = category;
    }



}
