/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.programpark.mirah.lexer;

import org.mirah.typer.ResolvedType;
import ru.programpark.mirah.ClassIndex;
import ru.programpark.mirah.ClassIndex.ClassPathQuery;
import ru.programpark.mirah.ClassIndex.Future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.LocalAssignment;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Named;
import mirah.lang.ast.Node;
import mirah.lang.ast.Package;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.RequiredArgument;
import mirah.lang.ast.Script;
import mirah.lang.ast.StaticMethodDefinition;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class SourceQuery implements List<Node>{

    private final MirahParserResult parser_result;
    List<Node> results = null;
    
    public SourceQuery(MirahParserResult pr){
        this.parser_result = pr;
    }
    
    private SourceQuery(MirahParserResult pr, List<Node> results){
        this.parser_result = pr;
        this.results = new ArrayList<Node>();
        this.results.addAll(results);
    }

    public SourceQuery(MirahParserResult pr, Node root){
        this.parser_result = pr;
        this.results = new ArrayList<Node>();
        this.results.add(root);
    }

    public String findPackage() {
        List<Node> list = results;
        if ( list == null )
        {
            list = parser_result.getParsedNodes();
        }

        for (int i = 0; i < list.size(); i++)
        if (list.get(i) instanceof Script) {
            Script script = (Script) list.get(i);
            for (int j = 0; j < script.body().size(); j++) {
                if (script.body().get(j) instanceof mirah.lang.ast.Package) {
                    return ((mirah.lang.ast.Package) script.body().get(j)).name().identifier();
                }
            }
        }
        return null;
    }
    
    public SourceQuery findClasses(int offset){

        if (parser_result.getParsedNodes() == null ) {
            //if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "findClasses DEBUGGER is NULL!!!");
            return null;
        }
        ClassScanner scanner = new ClassScanner();
        scanner.offset = offset;
        for( Object node : parser_result.getParsedNodes() ){
            ((Node)node).accept(scanner, null);
        }
        //if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "findClasses scanner.found: "+scanner.found);
        if ( scanner.found == null ) return null;
        return new SourceQuery(parser_result, scanner.found);
    }
    
    public SourceQuery findMethods(int offset){
        if (parser_result == null || parser_result.getParsedNodes() == null ) {
            //if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "findMethods DEBUGGER is NULL!!!");
            return null;
        }
        MethodOffsetScanner scanner = new MethodOffsetScanner();
        scanner.offset = offset;
        for( Object node : parser_result.getParsedNodes() ){
            ((Node)node).accept(scanner, null);
        }
        //if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "findMethods scanner.found: " + scanner.found);
        if (scanner.found == null) return null;
        return new SourceQuery(parser_result, scanner.found);
    }
    
    public SourceQuery findMethod(int offset){
        MethodDefinition cdef = null;
        int currRange = -1;
        for ( Node n : findMethods(offset)){
            if ( cdef == null
            || (n.position() != null && n.position().endChar()- n.position().startChar() < currRange)){
                cdef = (MethodDefinition)n;
                currRange = n.position().endChar()-n.position().startChar();
            }
        }
        List<Node> found = new ArrayList<Node>();
        if ( cdef != null ){
            found.add(cdef);
        }
        return new SourceQuery(parser_result, found);
    }
    
    public String findClassName( int offset )
    {
        SourceQuery queryClass = findClass(offset);
        // выбираю последнее значение из списка - возможно это вложенное замыкание
        if (queryClass != null && queryClass.size() > 0) {
            int i = queryClass.size() - 1;
            if (queryClass.get(i) instanceof Named) {
                return ((Named) queryClass.get(i)).name().identifier();
            } else {
                return queryClass.get(i).toString();
            }
        }
        return null;
    }
    public String findMethodName(int offset) {
    {
        SourceQuery queryMethod = findMethod(offset);
        if (queryMethod == null || queryMethod.isEmpty()) return null;
        
        for (int i = 0; i < queryMethod.toArray().length; i++) {
            if (queryMethod.toArray()[i] instanceof MethodDefinition) {
                MethodDefinition mdef = (MethodDefinition) queryMethod.toArray()[i];
                for (int j = 0; j < mdef.arguments().required_size(); j++) {
                    RequiredArgument rq = mdef.arguments().required(j);
                }
                return mdef.name().identifier();
            }
        }
        return null;
    }

    }
        
    public SourceQuery findClass(int offset){
        ClassDefinition cdef = null;
        int currRange = -1;
        SourceQuery sq = findClasses(offset);
        if ( sq != null )
        {
            for ( Node n : sq ){
                if ( cdef == null
                || (n.position() != null && n.position().endChar()- n.position().startChar() < currRange)){
                    cdef = (ClassDefinition)n;
                    currRange = n.position().endChar()-n.position().startChar();
                }
            }
        }
        List<Node> found = new ArrayList<Node>();
        if ( cdef != null ){
            found.add(cdef);
        }
        return new SourceQuery(parser_result, found);
    }
        
    public SourceQuery findParent(Object filter){
        List<Node> found = new ArrayList<Node>();
        for ( Node n : this.results ){
            Node cls = null;
            Node curr = n;
            while ( curr != null && (filter == null || !filter.equals(curr))){
                curr = curr.parent();
            }
            if ( curr != n && (filter == null || filter.equals(curr)) ){
                found.add(curr);
            }
        }
        return new SourceQuery(parser_result, found);
    }
    
    public SourceQuery findParentClass(){
        return findParent(new Object(){

            @Override
            public boolean equals(Object obj) {
                return obj instanceof ClassDefinition;
            }
            
        });
    }
    
    public SourceQuery findParentClosure(){
        return findParent(new Object(){

            @Override
            public boolean equals(Object obj) {
                return obj instanceof ClosureDefinition;
            }
            
        });
    }
    
    
    
    public String getType(){
        if ( results != null && !results.isEmpty() ){
            ResolvedType resolvedType = parser_result.getResolvedType(results.get(0));
            if(resolvedType != null) return resolvedType.name();
        }
        return null;
    }
    
    
    
    public SourceQuery findParentClosureOrClass(){
        return findParent(new Object(){

            @Override
            public boolean equals(Object obj) {
                return obj instanceof ClosureDefinition
                        || obj instanceof ClassDefinition;
            }
            
        });
    }
    
    public SourceQuery findParentPackage(){
        return findParent(new Object(){
            @Override
            public boolean equals(Object obj) {
                return obj instanceof mirah.lang.ast.Package;
            }
        });
    }
        
        
    
    public SourceQuery findClass(final String name){
        
        final List<Node> found = new ArrayList<Node>();
        if ( results == null ){
            if ( parser_result == null ){
                return new SourceQuery(parser_result, found);
            }
            for( Object node : parser_result.getParsedNodes() ){



                if ( node instanceof Node ){
                    ((Node)node).accept(new NodeScanner(){

                        @Override
                        public boolean enterClassDefinition(ClassDefinition node, Object arg) {
                            if ( name == null || name.equals(node.name().identifier())){
                                found.add(node);
                            }
                            return super.enterClassDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                        }





                    }, null);
                }
            }
        } else {
            for ( Node res : results ){
                res.accept(new NodeScanner(){

                    @Override
                    public boolean enterClassDefinition(ClassDefinition node, Object arg) {
                        if ( name == null || name.equals(node.name().identifier())){
                            found.add(node);
                        }
                        return super.enterClassDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                    }





                }, null);
            }
        }
        
        return new SourceQuery(parser_result, found);
    }
    
    
    private static class ClosureScanner extends BaseScanner {

        private String className;
        
        
        @Override
        public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
            if ( className == null ){
                found.add(node);
            }
            else if ( node.superclass() != null ){
                if ( "*".equals(className) ){
                    found.add(node);
                } else if ( className.equals(node.superclass().typeref().name())){
                    found.add(node);
                }
            } else if ( node.interfaces() != null && node.interfaces_size()>0 ){
                if ( "*".equals(className) ){
                    found.add(node);
                } else {
                    for ( int i=0; i<node.interfaces_size(); i++){
                        if ( className.equals(node.interfaces(i).typeref().name())){
                            found.add(node);
                            break;
                        }
                    }
                }
            } 
            return super.enterClosureDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
        }




    };
    
    private static class MethodScanner extends BaseScanner {

        private String methodName;
        

        @Override
        public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
            if ( methodName == null ){
                found.add(node);
            }
            
            else if ( node.name() != null ){
                if ( "*".equals(methodName) ){
                    found.add(node);
                } else if ( methodName.equals(node.name().identifier())){
                    found.add(node);
                }
            } 
            
            return super.enterMethodDefinition(node, arg);
        }
        
    };
    
    
    private static class FieldDefScanner extends BaseScanner {

        private String fieldName;
        

        @Override
        public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
            if ( fieldName == null ){
                found.add(node);
            }
            
            else if ( node.name() != null ){
                if ( "*".equals(fieldName) ){
                    found.add(node);
                } else if ( fieldName.equals(node.name().identifier())
                        || fieldName.equals("@"+node.name().identifier())){
                    found.add(node);
                }
            } 
            
            return super.enterFieldDeclaration(node, arg); //To change body of generated methods, choose Tools | Templates.
        }

        
    };
    
    private static class LocalVarScanner extends BaseScanner {

        private String varName;

        @Override
        public boolean enterLocalAssignment(LocalAssignment node, Object arg) {
            if ( varName == null ){
                found.add(node);
            }
            
            else if ( node.name() != null ){
                if ( "*".equals(varName) ){
                    found.add(node);
                } else if ( varName.equals(node.name().identifier())){
                    found.add(node);
                }
            } 
            return super.enterLocalAssignment(node, arg); //To change body of generated methods, choose Tools | Templates.
        }

        
        
     
        

        
        
       

        
    };
    
    private static class BaseScanner extends NodeScanner {
        int offset = -1;
        ArrayList<Node> found = new ArrayList<Node>();
    }
    
    private static class ClassScanner extends BaseScanner {
        

        @Override
        public boolean enterClassDefinition(ClassDefinition node, Object arg) {
            
            if ( node.position() != null 
                    && node.position().startChar() <= offset
                    && node.position().endChar() > offset ){
                found.add(node);
            }
            return super.enterClassDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
        }
        // `Добавлена обработка замыканий
        @Override
        public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
            
            if ( node.position() != null 
                    && node.position().startChar() <= offset
                    && node.position().endChar() > offset ){
                found.add(node);
            }
            return super.enterClosureDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    private static class MethodOffsetScanner extends BaseScanner {

        @Override
        public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
            if ( node.position() != null 
                    && node.position().startChar() <= offset
                    && node.position().endChar() > offset ){
                found.add(node);
            }
            return super.enterMethodDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
            if ( node.position() != null 
                    && node.position().startChar() <= offset
                    && node.position().endChar() > offset ){
                found.add(node);
            }
            return super.enterStaticMethodDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
        }
        
        
        
    }
    
    
    public SourceQuery findClosures(final String className){
        ClosureScanner scanner = new ClosureScanner();
        scanner.className = className;
        return find(scanner);
    }
    
    
    public SourceQuery findMethods(final String methodName){
        MethodScanner scanner = new MethodScanner();
        scanner.methodName = methodName;
        return find(scanner);
    }
    
    
    public SourceQuery findLocalVars(final String varname){
        LocalVarScanner scanner = new LocalVarScanner();
        scanner.varName = varname;
        return find(scanner);
    }
    
    
    
    private SourceQuery find(BaseScanner scanner){
        if ( results == null ){
            if ( parser_result == null ){
                return new SourceQuery(parser_result, scanner.found);
            }
            for( Object node : parser_result.getParsedNodes() ){
                if ( node instanceof Node ){
                    ((Node)node).accept(scanner, null);
                }
            }
        } else {
            for ( Node res : results ){
                res.accept(scanner, null);
            }
        }
        
        return new SourceQuery(parser_result, scanner.found);
    }
    
    
    public SourceQuery findFieldDefinitions(final String fieldName){
        FieldDefScanner scanner = new FieldDefScanner();
        scanner.fieldName = fieldName;
        if ( results == null ){
            if ( parser_result == null ){
                return new SourceQuery(parser_result, scanner.found);
            }
            for( Object node : parser_result.getParsedNodes() ){
                if ( node instanceof Node ){
                    ((Node)node).accept(scanner, null);
                }
            }
        } else {
            for ( Node res : results ){
                res.accept(scanner, null);
            }
        }
        
        return new SourceQuery(parser_result, scanner.found);
    }
    
   
    public String getFQN(String className, int offset){
        
        try {

            ImportScanner importScanner = new ImportScanner();
            for( Object node : parser_result.getParsedNodes() ){
                ((Node)node).accept(importScanner, null);
            }

            List<String> imports = importScanner.found;
            
            for ( String imprt : imports ){
                if ( imprt.endsWith("."+className) ){
                    return imprt;
                }
            }
            
            SourceQuery pkg = findClass(offset).findParentPackage();
            String packageName = null;
            if ( !pkg.isEmpty() ){
                Package p = (Package)pkg.get(0);
                packageName = p.name().identifier();
            }
            
            ClassIndex index = new ClassIndex();
            ClassPath[] classpaths = ClassQuery.getClassPaths(parser_result.getFileObject());
            Set<String> foundClasses = new HashSet<String>();
            for ( ClassPath cp : classpaths ){
                final boolean[] complete = new boolean[]{false};
                Future results = new Future(){

                    @Override
                    protected void resultsAdded() {
                        complete[0] = true;
                    }
                    
                };
                index.findClass(new ClassPathQuery(100, className, "", cp), results);
                foundClasses.addAll(results.getMatches());
            }
            
            // First let's look for classes in the same package
            if ( packageName != null ){
                for ( String cls : foundClasses ){
                    if ( cls.indexOf(".") != -1 ){
                        String pname = cls.substring(0, cls.lastIndexOf("."));
                        if ( pname.equals(packageName)){
                            return cls;
                        }
                    }
                        
                }
            }
            
            // Next let's look for classes in packages that have been imported
            for ( String cls : foundClasses ){
                if ( cls.indexOf(".") != -1 ){
                    String pname = cls.substring(0, cls.lastIndexOf("."));
                    if ( imports.contains(pname+".*")){
                        return cls;
                    }
                }

            }
            
            // Next let's look for classes in subpackages of this package
            if ( packageName != null ){
                for ( String cls : foundClasses ){
                    if ( cls.indexOf(".") != -1 ){
                        String pname = cls.substring(0, cls.lastIndexOf("."));
                        if ( pname.startsWith(packageName)){
                            return cls;
                        }
                    }

                }
            }
            
            // Next look for classes that are closest to the current package
            if ( packageName != null ){
                final String fPackageName = packageName;
                List<String> sortedPkgs = new ArrayList<String>();
                sortedPkgs.addAll(foundClasses);
                Collections.sort(sortedPkgs, new Comparator<String>(){

                    @Override
                    public int compare(String o1, String o2) {
                        int d1 = distance(o1, fPackageName);
                        int d2 = distance(o2, fPackageName);
                        if ( d1 < d2 ){
                            return -1;
                        } else if ( d1 > d2 ){
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                    
                });
                
                if ( !sortedPkgs.isEmpty()){
                    return sortedPkgs.get(0)+"."+className;
                }
            }
            
            if ( !foundClasses.isEmpty()){
                for ( String cls : foundClasses ){
                    return cls;
                }
            }
            
            
            
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    int distance(String package1, String package2){
        Set<String> parts1 = new HashSet<String>();
        Set<String> parts2 = new HashSet<String>();
        parts1.addAll(Arrays.asList(package1.split(".")));
        parts2.addAll(Arrays.asList(package2.split(".")));
        
        int parts1Size = parts1.size();
        
        parts1.removeAll(parts2);
        return Math.round((float)(parts1Size-parts1.size())/(float)parts1Size *100f);
    }
    
    
    
    public String getMethodId(MethodDefinition m){
        StringBuilder sb = new StringBuilder();
        sb.append(m.name().identifier());
        for ( int i=0; i<m.arguments().required_size(); i++){
            if ( m.arguments().required(i) == null){
                continue;
            }
            if ( m.arguments().required(i).type() == null ){
                continue;
            }
            if ( m.arguments().required(i).type().typeref() == null ){
                continue;
            }
            
            String argType = getFQN(m.arguments().required(i).type().typeref().name(), m.position().startChar());
            sb.append("_").append(argType);
        }
        return sb.toString();
    }

    @Override
    public int size() {
        return results.size();
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return results.contains(o);
    }

    @Override
    public Iterator<Node> iterator() {
        return results.iterator();
    }

    @Override
    public Object[] toArray() {
        return results.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return results.toArray(a);
    }

    @Override
    public boolean add(Node e) {
        return results.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return results.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return results.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Node> c) {
        return results.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Node> c) {
        return results.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return results.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return results.retainAll(c);
    }

    @Override
    public void clear() {
         results.clear();
    }

    @Override
    public Node get(int index) {
        return results.get(index);
    }
    
    public MethodDefinition getMethod(int index){
        return (MethodDefinition)get(index);
    }
    
    public ClosureDefinition getClosure(int index){
        return (ClosureDefinition)get(index);
    }

    @Override
    public Node set(int index, Node element) {
        return results.set(index, element);
    }

    @Override
    public void add(int index, Node element) {
        results.add(index, element);
    }

    @Override
    public Node remove(int index) {
        return results.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return results.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return results.lastIndexOf(o);
    }

    @Override
    public ListIterator<Node> listIterator() {
        return results.listIterator();
    }

    @Override
    public ListIterator<Node> listIterator(int index) {
        return results.listIterator(index);
    }

    @Override
    public List<Node> subList(int fromIndex, int toIndex) {
        return results.subList(fromIndex, toIndex);
    }
    
    
    
    
    
    
    
    
    
    
    
    
}
