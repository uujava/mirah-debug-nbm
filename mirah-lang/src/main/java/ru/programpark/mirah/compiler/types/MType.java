package ru.programpark.mirah.compiler.types;

import mirah.objectweb.asm.Opcodes;
import mirah.objectweb.asm.Type;
import org.mirah.typer.ResolvedType;

/**
 * TODO implement java.lang.model??
 * Pair for MethodType
 * Strip off Future specific from mirah types
 * Minimal memory footprint - most info stored in  backing AsmType
 */
public class MType implements ResolvedType {
    final Type asmType;


    public MType(Type asmType) {
        this.asmType = asmType;
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
        return asmType.getClassName();
    }

    @Override
    public boolean isMeta() {
        return (asmType.getOpcode(Opcodes.ACC_STATIC) & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
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
        return false;
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
        if (!(o instanceof MType)) return false;

        MType jType = (MType) o;

        return asmType.equals(jType.asmType);

    }

    @Override
    public int hashCode() {
        return asmType.hashCode();
    }
}
