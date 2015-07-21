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

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.TagException;

/**
 * 
 */
public class Main {
    private final static String FilenameFormat_In = "Track %d.wav";
    private final static String FilenameFormat = "%02d._%s.wav";
    private final static String FilenameFormat_Old = "%d %s.wav";

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

					    /*
					     */
					    File source_file = new File(dir,String.format(FilenameFormat_In,position));
					    if (source_file.isFile()){

						Path source = source_file.toPath();

						File target_file = new File(dir,String.format(FilenameFormat,number,title));

						Path target = target_file.toPath();

						Files.move(source,target);

						out.printf("M '%s' '%s'%n",source,target);

						Tag(target_file,artist,album,release_id,position,number,title);
					    }
					    else {
						source_file = new File(dir,String.format(FilenameFormat_Old,number,title));
						if (source_file.isFile()){

						    Path source = source_file.toPath();

						    File target_file = new File(dir,String.format(FilenameFormat,number,title));

						    Path target = target_file.toPath();

						    Files.move(source,target);

						    out.printf("M '%s' '%s'%n",source,target);

						    Tag(target_file,artist,album,release_id,position,number,title);
						}
						else {
						    source_file = new File(dir,String.format(FilenameFormat,number,title));

						    if (source_file.isFile()){

							Tag(source_file,artist,album,release_id,position,number,title);

							out.printf("U '%s'%n",source_file.toPath());
						    }
						    else {

							err.printf("Error, file not found '%s'%n",source_file.toPath());

							System.exit(1);
						    }
						}
					    }
					}
				    }
				}
				else {
				    err.println("Error, search prooduced no results (count 'release-list' zero).");
				    err.println();
				    err.printf("Request: ",response.getUserData(API.DOM_HTTP_REQUEST));
				    err.printf("Response: ",response.getUserData(API.DOM_HTTP_STATUS));
				    err.println();
				    api_release.prettyPrint(response,err);
				    err.println();

				    System.exit(1);
				}
			    }
			    else {
				err.println("Error, missing 'release-list' under 'metadata'.");
				err.println();
				err.printf("Request: ",response.getUserData(API.DOM_HTTP_REQUEST));
				err.printf("Response: ",response.getUserData(API.DOM_HTTP_STATUS));
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
    private final static void Tag(File file, String artist, String album, String reid, int pos, int num, String title)
	throws CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, IOException, FieldDataInvalidException, CannotWriteException
    {
	AudioFile f = AudioFileIO.read(file);
	Tag tag = f.getTag();
	tag.setField(FieldKey.ARTIST,artist);
	tag.setField(FieldKey.ALBUM,album);
	tag.setField(FieldKey.MUSICBRAINZ_RELEASEID,reid);
	tag.setField(FieldKey.TRACK,Integer.toString(num));
	tag.setField(FieldKey.TITLE,title);
	f.commit();
    }
}
