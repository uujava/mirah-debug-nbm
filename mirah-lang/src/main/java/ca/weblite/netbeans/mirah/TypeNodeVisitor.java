package ca.weblite.netbeans.mirah;

import mirah.lang.ast.Array;
import mirah.lang.ast.Boolean;
import mirah.lang.ast.Call;
import mirah.lang.ast.Fixnum;
import mirah.lang.ast.FunctionalCall;
import mirah.lang.ast.Hash;
import mirah.lang.ast.HashEntry;
import mirah.lang.ast.Identifier;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeList;
import mirah.lang.ast.SimpleString;
import mirah.lang.ast.Symbol;

/**
 *
 * @author Markov, markovs@programpark.ru
 * @Created on 30.05.2015, 17:44
 */
public class TypeNodeVisitor extends AbstractNodeVisitor {

    @Override
    public Object visitNodeList(NodeList nl, Object o) {
        // Обрабатывает большей частью массивы и хэши
        for (int i = 0; i < nl.size(); i++) {
            Node node = nl.get(i);
            Object result = node.accept(this, o);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public Object visitCall(Call call, Object o) {
        Node target = call.target();
        if (target != null) {
            Object result = target.accept(this, o);
            if (result != null) {
                return result;
            }
        }
        return call.parameters().accept(this, o);
    }

    @Override
    public Object visitSymbol(Symbol symbol, Object o) {
        return "String";
    }

    @Override
    public Object visitSimpleString(SimpleString ss, Object o) {
        return "String";
    }

    @Override
    public Object visitFloat(mirah.lang.ast.Float f, Object o) {
        return "double";
    }

    @Override
    public Object visitFixnum(Fixnum fixnum, Object o) {
        return "long";
    }

    @Override
    public Object visitBoolean(Boolean bln, Object o) {
        return "boolean";
    }

    @Override
    public Object visitHash(Hash hash, Object o) {
        for (int i = 0; i < hash.size(); i++) { 
            Object result = hash.get(i).accept(this, 0);
            if (result != null) {
                return result;
            }
        }
        return "{}";
    }

    @Override
    public Object visitHashEntry(HashEntry he, Object o) {
        Object key = he.key().accept(this, o);
        Object value = he.value().accept(this, o);
        if (key == null && value == null) {
            return null;
        } else if (key == null) {
            key = "Object";
        } else if (value == null) {
            value = "Object";
        }
        return new StringBuilder("{").append(key).append(" => ").append(value).append("}").toString();
    }

    @Override
    public Object visitArray(Array array, Object o) {
        Object type = array.values().accept(this, o);
        if (type == null) {
            return "[]";
        } else {
            return type + "[]";
        }
    }

    @Override
    public Object visitFunctionalCall(FunctionalCall fc, Object o) {
        Identifier type = fc.name();
        if (type == null) {
            return fc.parameters().accept(this, o);
        } else {
            return type.identifier();
        }
    }
}
