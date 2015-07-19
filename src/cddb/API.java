/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

/**
 * Single threaded Musicbrainz HTTP (client) interface.  A
 * multi-threaded user must ensure single threaded access to instances
 * of this class.
 */
public class API extends Object {

    public final static String HOST = "musicbrainz.org";
    public final static String PATH = "ws/2";

    private final static String USERAGENT = "syntelos-cddb/0.0 (http://www.syntelos.org/)";

    private final static DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
    static {
	DBF.setNamespaceAware(true);
    }


    public final Entity entity;
    /*
     * Single threaded access
     * 
     * [see] "Single threaded block"
     */
    private final DocumentBuilder builder;


    /**
     * 
     */
    public API(Entity entity){
	super();
	this.entity = entity;
	try {
	    this.builder = DBF.newDocumentBuilder();
	}
	catch (ParserConfigurationException exc){
	    throw new IllegalStateException("Error constructing XML parser",exc);
	}
    }


    /**
     * @param Plain text query string
     * 
     * @return Unconnected HTTP connection to service interface
     */
    public Document search(String query)
	throws IOException
    {
	StringBuilder url_string = new StringBuilder();
	{
	    url_string.append("http://");
	    url_string.append(HOST);
	    url_string.append('/');
	    url_string.append(PATH);
	    url_string.append('/');
	    url_string.append(entity.path);
	    url_string.append('?');
	    url_string.append("query");
	    url_string.append('=');
	    url_string.append(URLEncoder.encode(query));
	    url_string.append('&');
	    url_string.append("offset");
	    url_string.append('=');
	    url_string.append(0);
	    url_string.append('&');
	    url_string.append("limit");
	    url_string.append('=');
	    url_string.append(100);
	}

	URL url = new URL(url_string.toString());

	HttpURLConnection connection = (HttpURLConnection)url.openConnection();

	connection.setRequestProperty("User-Agent",USERAGENT);
	connection.setRequestProperty("Accept","application/xml");
	try {
	    if (200 == connection.getResponseCode()){
		try {
		    /*
		     * Single threaded block
		     */
		    builder.reset();

		    return builder.parse(connection.getInputStream(),url_string.toString());
		}
		catch (SAXException exc){
		    throw new IOException(String.format("Error parsing \"%s\" from \"%s\"%n",url_string,connection.getHeaderField(0)),exc);
		}
	    }
	    else {
		throw new java.net.ConnectException(String.format("Error response from \"%s\" was \"%s\"%n",url_string,connection.getHeaderField(0)));
	    }
	}
	finally {
	    connection.disconnect();
	}
    }
    /**
     * @param mbid The MBID required for MB lookups
     * 
     * @return Fully encoded URL string ready for {@link java.net.URL
     * URL} constructor
     */
    public String lookup(String mbid){
	StringBuilder url = new StringBuilder();
	{
	    url.append("http://");
	    url.append(HOST);
	    url.append('/');
	    url.append(PATH);
	    url.append('/');
	    url.append(entity.path);
	    url.append('/');
	    url.append(mbid);
	    url.append('?');
	    url.append("offset");
	    url.append('=');
	    url.append(0);
	    url.append('&');
	    url.append("limit");
	    url.append('=');
	    url.append(100);
	}
	return url.toString();
    }

}
