/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.editor.ast;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.util.LinkedList;
import mirah.lang.ast.Import;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Position;

/**
 *
 * @author savushkin
 */
public class AstSupport {

//    public static Node findByOffset( Node node, final int offset )
    public static Node findByOffset( MirahParser.NBMirahParserResult parserResult, final int offset )
    {       
        final Node[] res = new Node[1];
        res[0] = null;
        for( Object node : parserResult.getParsedNodes() ){
            if ( node instanceof Node ){
                ((Node)node).accept( new NodeScanner(){
                   @Override
                     public boolean enterDefault(Node node, Object arg) {
                        Position pos = node == null ? null : node.position();
                        if ( pos != null && pos.startChar() <= offset && pos.endChar() >= offset)
                        {
                             if ( res[0] != null && pos.endChar() >= res[0].position().endChar());
                             else res[0] = node;
                        }
                        return super.enterDefault(node, arg);
                     }
                 }, null);
            }
        }
        return res[0];
    }

    public static LinkedList<String> collectImports( Node node )
    {
        final LinkedList<String> includes = new LinkedList<>();
        
        node.accept( new NodeScanner(){
            public boolean enterImport( Import node, Object arg ) 
            {
                includes.add(node.fullName().identifier());
                return super.enterImport(node, arg);
            }
            
        }, null);

//        for( int i = 0 ; i < includes.size() ; i++ )
//        {
//            String s = includes.get(i);
//            System.out.println(s);
//        }
        return includes;
    }
  
    public static LinkedList<String> collectAsteriskImports( Node node )
    {
        final LinkedList<String> includes = new LinkedList<>();
        
        node.accept( new NodeScanner(){
            public boolean enterImport( Import node, Object arg ) 
            {
                if ( node.simpleName().identifier().equals("*"))
                    includes.add(node.fullName().identifier());
                return super.enterImport(node, arg);
            }
            
        }, null);

//        for( int i = 0 ; i < includes.size() ; i++ )
//        {
//            String s = includes.get(i);
//            System.out.println(s);
//        }
        return includes;
    }
  
}
