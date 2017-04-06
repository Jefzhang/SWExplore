package parser;

import org.xml.sax.helpers.DefaultHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
/**
 * Created by jfzhang on 03/03/2017.
 * do:
 *   1,Collect all the text of body;
 *   2,Collect all the links of body part;
 */
public class HtmlContentHandler extends DefaultHandler{
    private static final int MAX_ANCHOR_LENGTH = 100;

    private enum Element {
        A,
        META,
        BODY,
        H2,
        H1,
        SCRIPT,
        HEAD
    }

    private static class HtmlFactory {
        private static final Map<String, Element> name2Element;

        static {
            name2Element = new HashMap<>();
            for (Element element : Element.values()) {
                name2Element.put(element.toString().toLowerCase(), element);
            }
        }

        public static Element getElement(String name) {
            return name2Element.get(name);
        }
    }

    private String base;
    private String metaRefresh;
    private String metaLocation;
    private final Map<String, String> metaTags = new HashMap<>();
    private String webTitle;
    private volatile boolean isWithinBodyElement;
    private volatile boolean isWithinScript;
    private volatile boolean isWithinHead;
    private boolean isWithinh2;
    private boolean isWithinh1;
    private boolean hasBiography;
    private boolean hasPersonality;

    private final StringBuilder menuText;
    private final StringBuilder headText;

    private final List<String> outgoingUrls;

   // private UrlTagPair curUrl = null;
    private boolean anchorFlag = false;
    private final StringBuilder anchorText = new StringBuilder();

    public HtmlContentHandler() {
        isWithinBodyElement = false;
        isWithinScript = false;
        isWithinHead = false;
        isWithinh1 = false;
        isWithinh2 = false;
        hasBiography = false;
        hasPersonality = false;
        menuText = new StringBuilder();
        headText = new StringBuilder();
        outgoingUrls = new ArrayList<>();
    }

    @Override

    public void startElement(String uri, String localName, String qName, Attributes attributes)
    throws SAXException {
        //System.out.println(qName);
        Element element = HtmlFactory.getElement(qName);
        //System.out.println("start an element");

        /*if (element == Element.A) {           //read href
            if (isWithinBodyElement) {         //only read the links in body part
                String href = attributes.getValue("href");
                if (href != null) {
                    anchorFlag = true;
                    addToOutgoingUrls(href, localName);
                }
            }
        } else if (element == Element.BODY) {
            isWithinBodyElement = true;
        }*/
        if(element == Element.A) {
            //System.out.println("enter an element a");
            if (isWithinBodyElement && !isWithinScript) {
                String href = attributes.getValue("href");
                if (href != null) {
                    //anchorFlag = true;
                    addToOutgoingUrls(href);
                }
            }
        }else if(element == Element.BODY)
                isWithinBodyElement = true;
        else if(element == Element.H2)
                isWithinh2 = true;
        else if(element == Element.SCRIPT) {
            isWithinScript = true;
        }
        else if(element== Element.H1) {
            isWithinh1 = true;
            anchorFlag = true;
        }
    }

    private void addToOutgoingUrls(String href) {
       // curUrl = new UrlTagPair();
       // curUrl.setHref(href);
       // curUrl.setTag(tag);
        outgoingUrls.add(href);
    }

    @Override
    public void endElement(String uri, String localName, String qName){
        Element element = HtmlFactory.getElement(qName);
            if(element == Element.A){
                //anchorFlag = false;
                /*if (curUrl != null) {
                    String anchor =
                            anchorText.toString().replaceAll("\n", " ").replaceAll("\t", " ").trim();
                    if (!anchor.isEmpty()) {
                        if (anchor.length() > MAX_ANCHOR_LENGTH) {
                            anchor = anchor.substring(0, MAX_ANCHOR_LENGTH) + "...";
                        }
                        curUrl.setTag(localName);
                        curUrl.setAnchor(anchor);
                    }
                    anchorText.delete(0, anchorText.length());
                }*/
                }
            else if (element==Element.BODY)
                isWithinBodyElement = false;
            else if (element==Element.H2)
                isWithinh2 = false;
            else if (element==Element.SCRIPT)
                isWithinScript = false;
            else if (element==Element.H1) {
                isWithinh1 = false;
                anchorFlag = false;
                String name = anchorText.toString().replaceAll("\n", " ").replaceAll("\t", " ").trim();
                if(!name.isEmpty()){
                    this.webTitle = name;
                }
                else this.webTitle = null;
                anchorText.delete(0, anchorText.length());
            }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (isWithinh2) {
            if (menuText.length() > 0) {
                menuText.append(' ');
            }
            menuText.append(ch, start, length);

            /*if(isWithh2){
                if (new String(ch).contains("Biography")) hasBiography = true;
                if (new String(ch).contains("Personality")) hasPersonality = true;
            }*/
        }
        if(isWithinBodyElement){
            if (anchorFlag) {
                anchorText.append(new String(ch, start, length));
            }
        }
        /*if(isWithinScript){
            if(headText.length() > 0){
                headText.append(' ');
            }
            //System.out.println(new String(ch));
            headText.append(new String(ch,start,length));
        }*/
       // System.out.print(ch.toString());
    }

    @Override
    public void skippedEntity(String name){
        System.out.println(name);
    }

    public String getMenuText() {
        return menuText.toString().replaceAll("\n", " ").replaceAll("\t", " ").trim();
    }

    public List<String> getOutgoingUrls() {
        return outgoingUrls;
    }

    public String getBaseUrl() {
        return base;
    }

    public String getWebTitle(){
        return this.webTitle;
    }

    public String getScriptText(){
        return headText.toString().replaceAll("\n"," ").replaceAll("\t"," ").trim();
    }

    public boolean DoeshaveBiography(){
        return this.hasBiography;
    }

    public boolean DoeshavePersonality(){
        return this.hasPersonality;
    }

    public Map<String, String> getMetaTags() {
        return metaTags;
    }

}
