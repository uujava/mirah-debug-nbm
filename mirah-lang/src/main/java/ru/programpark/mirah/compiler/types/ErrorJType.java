package ru.programpark.mirah.compiler.types;

import org.mirah.typer.ResolvedType;

/**
 * TODO implement java.lang.model??
 * Strip off Future specific from mirah types
 * Minimal memory footprint - most info stored in  backing AsmType
 */
public class ErrorJType implements ResolvedType {

    private String name;
    private String message;


    public ErrorJType(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public ResolvedType widen(ResolvedType resolvedType) {
        throw new UnsupportedOperationException("widen not yet supported: " + this + " to: " + resolvedType);
    }

    @Override
    public boolean assignableFrom(ResolvedType resolvedType) {
        throw new UnsupportedOperationException("assignableFrom not yet supported: " + this + " to: " + resolvedType);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isMeta() {
        return false;
    }

    @Override
    public boolean isBlock() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public boolean matchesAnything() {
        return false;
    }

    @Override
    public boolean isFullyResolved() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorJType)) return false;

        ErrorJType jType = (ErrorJType) o;

        return name.equals(jType.name) && message.equals(((ErrorJType) o).message);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
