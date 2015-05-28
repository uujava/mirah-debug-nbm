package ru.programpark.mirah.editor.api.completion;

import java.util.Arrays;

/**
 *
 * @author Petr Hejl
 */
public final class MethodSignature {

    private final String name;

    private final String[] parameters;

    public MethodSignature(String name, String[] parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String[] getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MethodSignature other = (MethodSignature) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.parameters != other.parameters && (this.parameters == null
                || !Arrays.equals(this.parameters, other.parameters))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 41 * hash + (this.parameters != null ? Arrays.hashCode(this.parameters) : 0);
        return hash;
    }

}
