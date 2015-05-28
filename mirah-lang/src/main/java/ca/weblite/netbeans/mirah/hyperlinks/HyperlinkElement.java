/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.hyperlinks;

/**
 *
 * @author savushkin
 */
public class HyperlinkElement {
    
    String url;
    int offset;
    
    public HyperlinkElement( String url, int offset )
    {
        this.url = url;
        this.offset = offset;
    }
    public String getUrl()
    {
        return url;
    }
    public int getOffset()
    {
        return offset;
    }
}
