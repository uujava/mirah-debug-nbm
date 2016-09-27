package ru.programpark.mirah.compiler.stat;

import ru.programpark.mirah.compiler.stat.StatTimer;

/**
 * Reset stat
 */
public class ResetStatVizitor implements StatTimer.Visitor<StatTimer> {
    @Override
    public void visit(StatTimer statTimer) {
        statTimer.reset();
    }
}
