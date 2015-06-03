package ru.programpark.mirah.index.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;

public final class IndexedMethod extends IndexedElement implements MethodElement {

    private final List<MethodParameter> parameters;
    private final String returnType;
    private int offset;

    public IndexedMethod(
            IndexResult result,
            String clz,
            String name,
            String returnType,
            List<MethodParameter> parameters,
            String attributes,
            int flags, int offset ) {

        super(result, clz, name, attributes, flags);
        this.returnType = returnType;
        this.parameters = parameters;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return getSignature();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    @Override
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(in);
        sb.append("#"); // NOI18N
        sb.append(name);

        if (!parameters.isEmpty()) {
            sb.append("("); // NOI18N
            for (MethodParameter param : parameters) {
                sb.append(param.getFqnType());
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")"); // NOI18N
        }

        return sb.toString();
    }

    @Override
    public List<MethodParameter> getParameters() {
        return parameters;
    }

    @Override
    public List<String> getParameterTypes() {
        List<String> paramTypes = new ArrayList<>();

        for (MethodParameter parameter : getParameters()) {
            paramTypes.add(parameter.getType());
        }
        return paramTypes;
    }

    @Override
    public ElementKind getKind() {
        if (((name == null) && signature.startsWith("initialize(")) || // NOI18N
                ((name != null) && name.equals("initialize"))) { // NOI18N

            return ElementKind.CONSTRUCTOR;
        } else {
            return ElementKind.METHOD;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.in);
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + Objects.hashCode(this.signature);
        hash = 47 * hash + Objects.hashCode(this.modifiers);
        hash = 47 * hash + Objects.hashCode(this.parameters);
        hash = 47 * hash + Objects.hashCode(this.returnType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IndexedMethod other = (IndexedMethod) obj;
        if (!Objects.equals(this.in, other.in)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.signature, other.signature)) {
            return false;
        }
        if (!Objects.equals(this.modifiers, other.modifiers)) {
            return false;
        }
        if (!Objects.equals(this.parameters, other.parameters)) {
            return false;
        }
        if (!Objects.equals(this.returnType, other.returnType)) {
            return false;
        }
        return true;
    }
    
    public int getOffset()
    {
        return offset;
    }

}
