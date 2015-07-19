/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Path;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * 
 */
public class Main {
    private static void usage(){
	System.err.println("Synopsis");
	System.err.println();
	System.err.println("    cddb.Main <dir>");
	System.err.println();
	System.err.println("Description");
	System.err.println();
	System.err.println("    Check and repair track file names for Artist/Album directory.");
	System.err.println();
    }
    private static String[] ArtistAlbum(File dir){

	Path path = dir.toPath();
	int len = path.getNameCount();
	int x_album = (len-1);
	int x_artist = (x_album-1);
	if (0 <= x_artist && x_artist < x_album){
	    File f_artist = path.getName(x_artist).toFile();
	    File f_album = path.getName(x_album).toFile();
	    return new String[]{
		f_artist.getName(),
		f_album.getName()
	    };
	}
	else {
	    throw new IllegalStateException(dir.getPath());
	}
    }
    /**
     * 
     */
    public static void main(String[] argv){

	final PrintStream out = System.out;

	if (0 < argv.length){

	    final File dir = new File(argv[0]).getAbsoluteFile();

	    if (dir.isDirectory()){

		final int dir_count = dir.listFiles().length;

		final String[] info = ArtistAlbum(dir);

		if (null != info){

		    final String artist = info[0];
		    final String album = info[1];
		    out.printf("Artist: %s%n",artist);
		    out.printf("Album: %s%n",album);

		    try {
			API release = new API(Entity.RELEASE);

			String query = String.format("\"%s\" AND artist:\"%s\"",album,artist);

			Document response = release.search(query);

			String uri = response.getDocumentURI();

			System.out.println(uri);
			System.out.println();

			DOMImplementationLS ls = (DOMImplementationLS)response.getImplementation();

			LSOutput ls_out = ls.createLSOutput();
			ls_out.setByteStream(System.out);
			ls_out.setSystemId("system:out");
			ls_out.setEncoding("UTF-8");

			LSSerializer ls_ser = ls.createLSSerializer();
			{
			    DOMConfiguration ls_ser_config = ls_ser.getDomConfig();

			    ls_ser_config.setParameter("format-pretty-print",true);
			}
			ls_ser.write(response,ls_out);

			System.out.println();

			System.exit(0);
		    }
		    catch (Exception any){
			any.printStackTrace();
			System.exit(1);
		    }
		}
		else {
		    System.err.printf("Unable to determine artist/album from '%s'.%n",dir.getPath());
		    System.exit(1);
		}
	    }
	    else {
		System.err.printf("Directory not found '%s'.%n",dir.getPath());
		System.exit(1);
	    }
	}
	else {
	    usage();
	    System.exit(1);
	}
    }
}
