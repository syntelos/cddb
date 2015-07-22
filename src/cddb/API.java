/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

import java.io.IOException;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import org.xml.sax.SAXException;

/**
 * Single threaded Musicbrainz HTTP (client) interface.  A
 * multi-threaded user must ensure single threaded access to instances
 * of this class.
 */
public class API extends Object {

    public final static String DOM_HTTP_REQUEST = "cddb.dom.http.request";
    public final static String DOM_HTTP_STATUS = "cddb.dom.http.status";

    public final static String HOST = "musicbrainz.org";
    public final static String PATH = "ws/2";

    private final static String USERAGENT = "syntelos-cddb/0.0 (http://www.syntelos.org/)";

    private final static DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
    static {
	DBF.setNamespaceAware(true);
    }
    private final static org.w3c.dom.UserDataHandler UserDataHandler = null;


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
     * @return XML document response (or exception)
     */
    public Document search(String query)
	throws IOException
    {
	final StringBuilder url_builder = new StringBuilder();
	{
	    url_builder.append("http://");
	    url_builder.append(HOST);
	    url_builder.append('/');
	    url_builder.append(PATH);
	    url_builder.append('/');
	    url_builder.append(entity.path);
	    url_builder.append('?');
	    url_builder.append("query");
	    url_builder.append('=');
	    url_builder.append(URLEncoder.encode(query));
	    url_builder.append('&');
	    url_builder.append("offset");
	    url_builder.append('=');
	    url_builder.append(0);
	    url_builder.append('&');
	    url_builder.append("limit");
	    url_builder.append('=');
	    url_builder.append(100);
	}

	final String url_string = url_builder.toString();

	final URL url = new URL(url_string);

	final HttpURLConnection connection = (HttpURLConnection)url.openConnection();

	connection.setRequestProperty("User-Agent",USERAGENT);
	connection.setRequestProperty("Accept","application/xml");
	try {
	    if (200 == connection.getResponseCode()){
		try {
		    /*
		     * Single threaded block
		     */
		    builder.reset();

		    Document doc = builder.parse(connection.getInputStream(),url_string);

		    doc.setUserData(DOM_HTTP_REQUEST,url_string,UserDataHandler);
		    doc.setUserData(DOM_HTTP_STATUS,connection.getHeaderField(0),UserDataHandler);

		    return doc;
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
     * @return XML document response (or exception)
     */
    public Document lookup(String mbid)
	throws IOException
    {
	return lookup(mbid,null);
    }
    /**
     * @param mbid The MBID required for MB lookups
     * @param inc Optionally non null string containing a comma
     * separated list of one or more 'inc' parameter values.  For
     * example, "recordings" will cause a track list to be found in a
     * "release" lookup.
     * 
     * @return XML document response (or exception)
     */
    public Document lookup(String mbid, String inc)
	throws IOException
    {
	final StringBuilder url_builder = new StringBuilder();
	{
	    url_builder.append("http://");
	    url_builder.append(HOST);
	    url_builder.append('/');
	    url_builder.append(PATH);
	    url_builder.append('/');
	    url_builder.append(entity.path);
	    url_builder.append('/');
	    url_builder.append(mbid);
	    if (null != inc){
		url_builder.append('?');
		url_builder.append("inc");
		url_builder.append('=');
		url_builder.append(URLEncoder.encode(inc));
	    }
	}

	final String url_string = url_builder.toString();

	final URL url = new URL(url_string);

	final HttpURLConnection connection = (HttpURLConnection)url.openConnection();

	connection.setRequestProperty("User-Agent",USERAGENT);
	connection.setRequestProperty("Accept","application/xml");
	try {
	    if (200 == connection.getResponseCode()){
		try {
		    /*
		     * Single threaded block
		     */
		    builder.reset();

		    Document doc = builder.parse(connection.getInputStream(),url_string);

		    doc.setUserData(DOM_HTTP_REQUEST,url_string,UserDataHandler);
		    doc.setUserData(DOM_HTTP_STATUS,connection.getHeaderField(0),UserDataHandler);

		    return doc;
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
    public void prettyPrint(Node node){
	Document doc = (node instanceof Document)?((Document)node):(node.getOwnerDocument());
	DOMImplementationLS ls = (DOMImplementationLS)doc.getImplementation();

	LSOutput ls_out = ls.createLSOutput();
	{
	    ls_out.setEncoding("UTF-8");

	    ls_out.setByteStream(System.out); // [target]
	    ls_out.setSystemId("system:out");
	}
	LSSerializer ls_ser = ls.createLSSerializer();
	{
	    DOMConfiguration ls_ser_config = ls_ser.getDomConfig();

	    ls_ser_config.setParameter("format-pretty-print",true);
	}
	ls_ser.write(node,ls_out);
    }
    public void prettyPrint(Node node, OutputStream out){
	Document doc = (node instanceof Document)?((Document)node):(node.getOwnerDocument());
	DOMImplementationLS ls = (DOMImplementationLS)doc.getImplementation();

	LSOutput ls_out = ls.createLSOutput();
	{
	    ls_out.setEncoding("UTF-8");

	    ls_out.setByteStream(out); // [target]
	}
	LSSerializer ls_ser = ls.createLSSerializer();
	{
	    DOMConfiguration ls_ser_config = ls_ser.getDomConfig();

	    ls_ser_config.setParameter("format-pretty-print",true);
	}
	ls_ser.write(node,ls_out);
    }
}
