package ru.programpark.mirah.editor.utils;

/**
 * Handy methods when working with the Closure expressions.
 *
 * @author Hamlet D'Arcy
 * @author Sergei Egorov
 */

public class ClosureUtils {

    /**
     * Converts a ClosureExpression into the String source.
     *
     * @param readerSource a source
     * @param expression a closure. Can't be null
     * @return the source the closure was created from
     * @throws java.lang.IllegalArgumentException when expression is null
     * @throws java.lang.Exception when closure can't be read from source
     */
/*    
    public static String convertClosureToSource(ReaderSource readerSource, ClosureExpression expression) throws Exception {
        if (expression == null) throw new IllegalArgumentException("Null: expression");

        StringBuilder result = new StringBuilder();
        for (int x = expression.getLineNumber(); x <= expression.getLastLineNumber(); x++) {
            String line = readerSource.getLine(x, null);
            if (line == null) {
                throw new Exception(
                        "Error calculating source code for expression. Trying to read line " + x + " from " + readerSource.getClass()
                );
            }
            if (x == expression.getLastLineNumber()) {
                line = line.substring(0, expression.getLastColumnNumber() - 1);
            }
            if (x == expression.getLineNumber()) {
                line = line.substring(expression.getColumnNumber() - 1);
            }
            //restoring line breaks is important b/c of lack of semicolons
            result.append(line).append('\n');
        }


        String source = result.toString().trim();
        if (!source.startsWith("{")) {
            throw new Exception("Error converting ClosureExpression into source code. Closures must start with {. Found: " + source);
        }

        return source;
    }
*/    
}