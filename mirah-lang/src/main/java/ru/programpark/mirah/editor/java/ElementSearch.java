package ru.programpark.mirah.editor.java;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.netbeans.api.annotations.common.CheckForNull;

/**
 *
 * @author Petr Hejl
 */
public final class ElementSearch {

    private ElementSearch() {
        super();
    }

    @CheckForNull
    public static TypeElement getClass(Elements elements, String name) {
        TypeElement typeElement = elements.getTypeElement(name);
        if (typeElement == null) {
            typeElement = getInnerClass(elements, name);
        }
        return typeElement;
    }

    private static TypeElement getInnerClass(Elements elements, String name) {
        int index = name.indexOf("$"); // NOI18N
        TypeElement typeElement = null;
        if (index > 0 && name.length() > index + 1) {
            TypeElement enclosingElement = elements.getTypeElement(name.substring(0, index));

            int nextIndex = index;
            while (enclosingElement != null && nextIndex >= 0) {
                String subName = name.substring(nextIndex + 1);
                int subIndex = subName.indexOf("$"); // NOI18N
                if (subIndex >= 0) {
                    subName = subName.substring(0, subIndex);
                    nextIndex = nextIndex + 1 + subIndex;
                } else {
                    nextIndex = -1;
                }

                boolean found = false;
                for (TypeElement elem : ElementFilter.typesIn(enclosingElement.getEnclosedElements())) {
                    Name elemName = elem.getSimpleName();

                    if (elemName.toString().equals(subName)) {
                        enclosingElement = elem;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    enclosingElement = null;
                }
            }
            typeElement = enclosingElement;
        }
        return typeElement;
    }
}
