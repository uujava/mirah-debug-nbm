package ru.programpark.mirah.lexer;

import mirah.lang.ast.Node;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;
import org.mirah.typer.ResolvedType;
import org.mirah.typer.TypeFuture;
import org.mirah.typer.TypeListener;
import org.mirah.util.Context;

import java.lang.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by kozyr on 20.06.2016.
 */
public class TypeInferenceListener implements DebuggerInterface {

    public static class PositionType {

        public int startPos, endPos;
        public ResolvedType type;
        public Node node;

        @Override
        public String toString() {
            if (type == null) {
                return "[" + startPos + "," + endPos + "]";
            } else {
                return "[" + type.name() + " " + startPos + "," + endPos + "]";
            }
        }
    }

    final private TreeSet<PositionType> leftEdges;
    final private TreeSet<PositionType> rightEdges;
    final private HashMap<Node, ResolvedType> resolvedTypes = new HashMap<>();

    public TypeInferenceListener() {
        leftEdges = new TreeSet<>(new Comparator<PositionType>() {

            @Override
            public int compare(PositionType o1, PositionType o2) {
                if (o1.startPos < o2.startPos) {
                    return -1;
                } else if (o2.startPos < o1.startPos) {
                    return 1;
                } else if (o1.endPos < o2.endPos) {
                    return -1;
                } else if (o2.endPos < o1.endPos) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        rightEdges = new TreeSet<>(new Comparator<PositionType>() {

            @Override
            public int compare(PositionType o1, PositionType o2) {
                if (o1.endPos < o2.endPos) {
                    return -1;
                } else if (o2.endPos < o1.endPos) {
                    return 1;
                } else if (o1.startPos < o2.startPos) {
                    return -1;
                } else if (o2.startPos < o1.startPos) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    public PositionType findNearestPositionOccurringAfter(int pos) {
        PositionType t = new PositionType();
        t.startPos = pos;
        return leftEdges.ceiling(t);
    }

    public PositionType findNearestPositionOccuringBefore(int pos) {
        PositionType t = new PositionType();
        t.endPos = pos;
        return rightEdges.lower(t);
    }

    public ResolvedType getType(Node node) {
        return resolvedTypes.get(node);
    }

    public HashMap<Node, ResolvedType> getResolvedTypes() {
        return resolvedTypes;
    }

    public SortedSet<PositionType> findPositionsWithRightEdgeInRange(
            int start,
            int end) {
        PositionType p1 = new PositionType();
        p1.endPos = start;
        p1.startPos = 0;

        PositionType p2 = new PositionType();
        p2.endPos = end;
        p2.startPos = end;

        SortedSet<PositionType> o1 = rightEdges.subSet(p1, p2);
        return o1;
    }

    public int countNodes() {
        return rightEdges.size();
    }

    public Node firstNode() {
        return leftEdges.first().node;
    }

    @Override
    public void parsedNode(Node node) {
    }

    @Override
    public void enterNode(Context cntxt, Node node, boolean bln) {
    }

    @Override
    public void exitNode(Context cntxt, final Node node, TypeFuture tf) {
        tf.onUpdate(new TypeListener() {

            @Override
            public void updated(TypeFuture tf, ResolvedType rt) {
                if (!tf.isResolved()) {
                    return;
                }

                if (node.position() == null) {
                    return;
                }
                PositionType t = new PositionType();
                t.startPos = node.position().startChar();
                t.endPos = node.position().endChar();
                t.type = rt;
                t.node = node;
                if (leftEdges.contains(t)) {
                    leftEdges.remove(t);
                }
                if (rightEdges.contains(t)) {
                    rightEdges.remove(t);
                }
                leftEdges.add(t);
                rightEdges.add(t);
                resolvedTypes.put(node, rt);
            }
        });
    }

    @Override
    public void inferenceError(Context cntxt, Node node, TypeFuture tf) {
    }
}
