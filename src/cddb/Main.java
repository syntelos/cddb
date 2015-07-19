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
import java.nio.file.Files;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class Main {
    private final static PrintStream out = System.out;
    private final static PrintStream err = System.err;

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
	if (0 < argv.length){

	    final File dir = new File(argv[0]).getAbsoluteFile();

	    if (dir.isDirectory()){

		final int dir_count = dir.listFiles().length;

		final String[] info = ArtistAlbum(dir);

		if (null != info){

		    final String artist = info[0];
		    final String album = info[1];
		    out.printf("# %s/%s%n",artist,album);

		    final API api_release = new API(Entity.RELEASE);

		    Document response = null;
		    try {
			/*
			 * Search for 'reid'
			 */
			String query = String.format("\"%s\" AND artist:\"%s\" AND country:US AND format:CD",album,artist);

			response = api_release.search(query);

			Element metadata = response.getDocumentElement();

			if (null != metadata){

			    Element release_list = (Element)metadata.getFirstChild();

			    if (null != release_list){

				Element el_release = (Element)release_list.getChildNodes().item(0);

				if (null != el_release){

				    String release_id = el_release.getAttribute("id");
				    /*
				     * lookup release for list of track data (via
				     * 'inc recordings')
				     */
				    response = api_release.lookup(release_id,"recordings");

				    NodeList tracks_list = response.getElementsByTagName("track");
				    {
					int cc = 0;
					int count = tracks_list.getLength();
					for (cc = 0; cc < count; cc++){
					    Element track = (Element)tracks_list.item(cc);

					    int position = Integer.parseInt(track.getElementsByTagName("position").item(0).getTextContent());
					    int number = Integer.parseInt(track.getElementsByTagName("number").item(0).getTextContent());
					    String title = track.getElementsByTagName("title").item(0).getTextContent();

					    //out.printf("%d %d %s%n",position,number,title);
					    File source_file = new File(dir,String.format("Track %d.wav",position));
					    if (source_file.isFile()){

						Path source = source_file.toPath();

						Path target = new File(dir,String.format("%d %s.wav",number,title)).toPath();

						Files.move(source,target);

						out.printf("M '%s' '%s'%n",source,target);
					    }
					    else {
						err.printf("Error, file not found '%s'%n",source_file.toPath());

						System.exit(1);
					    }
					}
				    }
				}
				else {
				    err.println("Error, search prooduced no results (count 'release-list' zero).");

				    System.exit(1);
				}
			    }
			    else {
				err.println("Error, missing 'release-list' under 'metadata'.");
				err.println();
				api_release.prettyPrint(response,err);
				err.println();
				System.exit(1);
			    }
			}
			System.exit(0);
		    }
		    catch (Exception any){
			any.printStackTrace();
			if (null != response){

			    err.printf("Request: ",response.getUserData(API.DOM_HTTP_REQUEST));
			    err.printf("Response: ",response.getUserData(API.DOM_HTTP_STATUS));
			    err.println();
			    api_release.prettyPrint(response,err);
			    err.println();
			}
			System.exit(1);
		    }
		}
		else {
		    err.printf("Unable to determine artist/album from '%s'.%n",dir.getPath());
		    System.exit(1);
		}
	    }
	    else {
		err.printf("Directory not found '%s'.%n",dir.getPath());
		System.exit(1);
	    }
	}
	else {
	    usage();
	    System.exit(1);
	}
    }
}
