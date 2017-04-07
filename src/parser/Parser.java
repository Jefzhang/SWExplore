package parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
//import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import crawler.exception.ParseException;

import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import crawler.*;
import url.*;

import util.Util;

/**
 * Created by jfzhang on 03/03/2017.
 */
public class Parser extends Configurable{

    private final HtmlParser htmlParser;
    private final ParseContext parseContext;
    protected static final Logger logger = LoggerFactory.getLogger(Parser.class);


    public Parser(CrawlConfig config) throws InstantiationException, IllegalAccessException {
        super(config);
        htmlParser = new HtmlParser();
        parseContext = new ParseContext();
        parseContext.set(HtmlMapper.class, AllTagMapper.class.newInstance());
    }

    /**Use Htmlhandle for collecting data of a page
     * @param page
     * @param contextURL url
     */
    public void parse(Page page, String contextURL) throws ParseException
    {
            Metadata metadata = new Metadata();
            HtmlContentHandler contentHandler = new HtmlContentHandler();
            try (InputStream inputStream = new ByteArrayInputStream(page.getContentData())) {
                htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
            } catch (Exception e) {
               // throw new ParseException();
            }
            if (page.getContentCharset() == null) {
                page.setContentCharset(metadata.get("Content-Encoding"));
            }
            page.getWebURL().setAnchor(contentHandler.getWebTitle());
            HtmlParseData parseData = new HtmlParseData();
            parseData.setText(contentHandler.getMenuText().trim());
            parseData.setTitle(metadata.get(DublinCore.TITLE));
            Set<WebURL> outgoingUrls = new HashSet<>();

            String baseURL = contentHandler.getBaseUrl();
            if (baseURL != null) {
                contextURL = baseURL;
            }

            boolean character = isCharacter(contentHandler);
            parseData.setIsCharacter(character);

            if(character) {

                int urlCount = 0;

                //Select outgoing urls
                for (String href: contentHandler.getOutgoingUrls()) {
                    if ((href == null) || href.trim().isEmpty()) {
                        continue;
                    }


                    //String hrefLoweredCase = href.trim().toLowerCase();
                    String url = URLnormlization.getCanonicalURL(href, contextURL);
                    String hrefLoweredCase = url.trim().toLowerCase();
                    if (Util.isRelatedPage(hrefLoweredCase)) {
                        //String url = URLnormlization.getCanonicalURL(href, contextURL);
                        if (url != null) {
                            WebURL webURL = new WebURL();
                            webURL.setURL(url);
                            outgoingUrls.add(webURL);
                            urlCount++;
                            if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                                break;
                            }
                        }
                    }
                }
                parseData.setOutgoingUrls(outgoingUrls);
            }

            try {
                if (page.getContentCharset() == null) {
                    parseData.setHtml(new String(page.getContentData()));
                } else {
                    parseData.setHtml(new String(page.getContentData(), page.getContentCharset()));
                }

                page.setParseData(parseData);
            } catch (UnsupportedEncodingException e) {
                logger.error("error parsing the html: " + page.getWebURL().getURL(), e);
               // throw new ParseException();
            }
        }

    public boolean isCharacter(HtmlContentHandler handler){
        int count = 0;
        String menutext = handler.getMenuText();
        boolean hasBiography = menutext.matches("(.*)Biography(.*)");
        boolean hasPersonality = menutext.matches("(.*)Personality and traits(.*)");
        boolean hasCharacter = menutext.matches("(.*)Characteristic(.*)");
        if(hasBiography) count+=3;
        if(hasPersonality||hasCharacter) count+=1;
        if(count>=4) return true;
        if((count>=3)&&(Math.random()>=0.25)) return true;
        else return false;
    }
}