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
        AREA,
        LINK,
        IFRAME,
        FRAME,
        EMBED,
        IMG,
        BASE,
        META,
        BODY,
        SCRIPT
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

    private boolean isWithinBodyElement;
    private final StringBuilder bodyText;

    private final List<UrlTagPair> outgoingUrls;

    private UrlTagPair curUrl = null;
    private boolean anchorFlag = false;
    private final StringBuilder anchorText = new StringBuilder();

    public HtmlContentHandler() {
        isWithinBodyElement = false;
        bodyText = new StringBuilder();
        outgoingUrls = new ArrayList<>();
    }

    @Override

    public void startElement(String uri, String localName, String qName, Attributes attributes)
    throws SAXException {
        Element element = HtmlFactory.getElement(localName);

        if (element == Element.A) {           //read href
            if (isWithinBodyElement) {         //only read the links in body part
                String href = attributes.getValue("href");
                if (href != null) {
                    anchorFlag = true;
                    addToOutgoingUrls(href, localName);
                }
            }
        } else if (element == Element.BODY) {
            isWithinBodyElement = true;
        }
    }
       /* else if (element == Element.IMG) {
            String imgSrc = attributes.getValue("src");
            if (imgSrc != null) {
                addToOutgoingUrls(imgSrc, localName);

            }
        } else if ((element == Element.IFRAME) || (element == Element.FRAME) ||
                (element == Element.EMBED) || (element == Element.SCRIPT)) {
            String src = attributes.getValue("src");
            if (src != null) {
                addToOutgoingUrls(src, localName);

            }
        } else if (element == Element.BASE) {
            if (base != null) { // We only consider the first occurrence of the Base element.
                String href = attributes.getValue("href");
                if (href != null) {
                    base = href;
                }
            }
        } else if (element == Element.META) {
            String equiv = attributes.getValue("http-equiv");
            if (equiv == null) { // This condition covers several cases of XHTML meta
                equiv = attributes.getValue("name");
            }

            String content = attributes.getValue("content");
            if ((equiv != null) && (content != null)) {
                equiv = equiv.toLowerCase();
                metaTags.put(equiv, content);

                // http-equiv="refresh" content="0;URL=http://foo.bar/..."
                if ("refresh".equals(equiv) && (metaRefresh == null)) {
                    int pos = content.toLowerCase().indexOf("url=");
                    if (pos != -1) {
                        metaRefresh = content.substring(pos + 4);
                        addToOutgoingUrls(metaRefresh, localName);
                    }
                }

                // http-equiv="location" content="http://foo.bar/..."
                if ("location".equals(equiv) && (metaLocation == null)) {
                    metaLocation = content;
                    addToOutgoingUrls(metaLocation, localName);
                }
            }*/

    private void addToOutgoingUrls(String href, String tag) {
        curUrl = new UrlTagPair();
        curUrl.setHref(href);
        curUrl.setTag(tag);
        outgoingUrls.add(curUrl);
    }

    @Override
    public void endElement(String uri, String localName, String qName){
        Element element = HtmlFactory.getElement(localName);
        if (element == Element.A) {
            anchorFlag = false;
            if (curUrl != null) {
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
            }
            curUrl = null;
        } else if (element == Element.BODY) {
            isWithinBodyElement = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (isWithinBodyElement) {
            if (bodyText.length() > 0) {
                bodyText.append(' ');
            }
            bodyText.append(ch, start, length);
            if (anchorFlag) {
                anchorText.append(new String(ch, start, length));
            }
        }
    }

    public String getBodyText() {
        return bodyText.toString();
    }

    public List<UrlTagPair> getOutgoingUrls() {
        return outgoingUrls;
    }

    public String getBaseUrl() {
        return base;
    }

    public Map<String, String> getMetaTags() {
        return metaTags;
    }

}
