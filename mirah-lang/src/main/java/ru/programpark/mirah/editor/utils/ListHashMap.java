package ru.programpark.mirah.editor.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This represents a 
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
public class ListHashMap<K,V> implements Map<K,V> {
    private final Object[] listKeys;
    private final Object[] listValues;
    private int size = 0;
    private Map<K,V> innerMap;
    private final int maxListFill;

    public ListHashMap() {
        this(3);
    }

    public ListHashMap(int listSize){
        this.listKeys = new Object[listSize];
        this.listValues = new Object[listSize];
        maxListFill = listSize;
    }

    public void clear() {
        innerMap = null;
        for (int i=0; i<maxListFill; i++) {
            listValues[i] = null;
            listKeys[i] = null;
        }
        size = 0;
    }

    public boolean containsKey(Object key) {
        if (size<maxListFill) {
            for (int i=0; i<size; i++) {
                if (listKeys[i].equals(key)) return true;
            }
            return false;
        } else {
            return innerMap.containsKey(key);
        }
    }

    public boolean containsValue(Object value) {
        if (size<maxListFill) {
            for (int i=0; i<size; i++) {
                if (listValues[i].equals(value)) return true;
            }
            return false;
        } else {
            return innerMap.containsValue(value);
        }
    }

    private Map<K,V> makeMap() {
        Map<K,V> m = new HashMap();
        for (int i=0; i<size; i++) {
            m.put((K) listKeys[i], (V) listValues[i]);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        Map m = innerMap!=null?innerMap:makeMap();
        return m.entrySet();
    }

    public V get(Object key) {
        if(size==0) return null;
        if (innerMap==null) {
            for (int i=0; i<size; i++) {
                if (listKeys[i].equals(key)) return (V) listValues[i];
            }
            return null;
        } else {
            return innerMap.get(key);
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Set<K> keySet() {
        Map m = innerMap!=null?innerMap:makeMap();
        return m.keySet();
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (innerMap==null) {
            for (int i=0; i<size; i++) {
                if (listKeys[i].equals(key)) {
                    V old = (V) listValues[i];
                    listValues[i] = value;
                    return old;
                }
            }
            if (size<maxListFill) {
                listKeys[size] = key;
                listValues[size] = value;
                size++;
                return null;
            } else {
                innerMap = makeMap();
            }
        }
        V val = (V) innerMap.put(key, value);
        size = innerMap.size();
        return val;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public V remove(Object key) {
        if (innerMap==null) {
            for (int i=0; i<size; i++) {
                if (listKeys[i].equals(key)) {
                    V old = (V) listValues[i];
                    size--;
                    listValues[i] = listValues[size];
                    listKeys[i] = listKeys[size];
                    return old;
                }
            }
            return null;
        } else {
            V old = innerMap.remove(key);
            size = innerMap.size();
            if (size<=maxListFill) {
                mapToList();
            }
            return old;
        }
    }

    private void mapToList() {
        int i = 0;
        for (Entry<? extends K,? extends V> entry : innerMap.entrySet()) {
            listKeys[i] = entry.getKey();
            listValues[i] = entry.getValue();
            i++;
        }
        size = innerMap.size();
        innerMap = null;
    }

    public int size() {
        return size;
    }

    public Collection<V> values() {
        if (innerMap==null) {
            ArrayList<V> list = new ArrayList(size);
            for (int i=0; i<size; i++) {
                list.add((V) listValues[i]);
            }
            return list;
        } else {
            return innerMap.values();
        }
    }

}
