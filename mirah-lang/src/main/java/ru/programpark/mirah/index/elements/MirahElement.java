package ru.programpark.mirah.index.elements;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.Collections;
import java.util.Set;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.openide.filesystems.FileObject;

public abstract class MirahElement implements ElementHandle {

    /**
     * Name of the element where this Mirah element lies. This might be package
     * name in case of class element, class name in case of field element, etc.
     */
    protected String in;

    /** Name of this element */
    protected String name;

    /** Signature of the element*/
    protected String signature;


    public MirahElement() {
    }

    public MirahElement(String in) {
        this(in, null);
    }

    public MirahElement(String in, String name) {
        this.in = in;
        this.name = name;
    }

    @Override
    public abstract ElementKind getKind();

    @Override
    public boolean signatureEquals(ElementHandle handle) {
        if (getIn().equals(handle.getIn()) &&
            getName().equals(handle.getName()) &&
            getKind().equals(handle.getKind())) {

            return true;
        }
        return false;
    }

    @Override
    public String getIn() {
        return in;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns signature of this element.
     * <p>
     * The signature is formed as {@code in.name}.
     * </p>
     * @return signature of this element
     */
    public String getSignature() {
        if (signature == null) {
            StringBuilder sb = new StringBuilder();
            String clz = getIn();
            if (clz != null && clz.length() > 0) {
                sb.append(clz);
                sb.append("."); // NOI18N
            }
            sb.append(getName());
            signature = sb.toString();
        }

        return signature;
    }

    @Override
    public String getMimeType() {
        return MirahTokenId.MIME_TYPE;
    }

    @Override
    public FileObject getFileObject() {
        return null;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public OffsetRange getOffsetRange(ParserResult result) {
        return OffsetRange.NONE;
    }
}
