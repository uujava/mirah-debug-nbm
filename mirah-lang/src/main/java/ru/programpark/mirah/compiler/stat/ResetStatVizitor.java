package ru.programpark.mirah.compiler.stat;

/**
 * Reset stat
 */
public class ResetStatVizitor implements StatTimer.Visitor<StatTimer> {
    @Override
    public void visit(StatTimer statTimer) {
        statTimer.reset();
    }
}
