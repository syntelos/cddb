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
    private final static String FilenameFormat_In = "Track %d.%s";
    private final static String FilenameFormat = "%02d._%s.%s";
    private final static String FilenameFormat_Old = "%d %s.%s";

    private final static PrintStream out = System.out;
    private final static PrintStream err = System.err;
    private final static InputStream in = System.in;

    private static void usage(){
	System.err.println("Synopsis");
	System.err.println();
	System.err.println("    cddb.Main <dir> [--print | --tag]");
	System.err.println();
	System.err.println("Description");
	System.err.println();
	System.err.println("    Check and repair track file names for Artist/Album directory.");
	System.err.println();
	System.err.println("    With 'print' option, don't modify files.");
	System.err.println();
	System.err.println("    With 'tag' option, don't use [CDDB/Musicbrainz] web services.");
	System.err.println("    (Employs artist, album and track directory and file name data).");
	System.err.println();
	System.err.println("    With 'print' and 'tag' options, list ID3 tags for files found");
	System.err.println("    in directory.");
	System.err.println();
	System.exit(1);
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

	    File dir = null;

	    String fext = "mp3";

	    boolean print = false, tag = false;

	    /*
	     */
	    for (int argc = argv.length, argx = 0; argx < argc; argx++){
		String arg = argv[argx];
		if ('-' == arg.charAt(0)){
		    if (arg.equals("--print")){
			print = (!print);
		    }
		    else if (arg.equals("--tag")){
			tag = (!tag);
		    }
		    else if (arg.equals("--fext")){
			argx += 1;
			if (argx < argc){
			    arg = argv[argx];
			    fext = arg;
			}
			else {
			    usage();
			}
		    }
		    else {
			usage();
		    }
		}
		else {
		    dir = new File(arg).getAbsoluteFile();		    
		}
	    }

	    /*
	     */
	    if (null == dir){
		usage();
	    }
	    else if (print && tag){
		try {
		    for (File file : dir.listFiles()){

			if (PrintTag(file)){
			    continue;
			}
			else {
			    System.exit(1);
			}
		    }
		    System.exit(0);
		}
		catch (Exception exc){
		    exc.printStackTrace();
		    System.exit(1);
		}
	    }
	    else if (tag && dir.isDirectory()){

		fext = Fext(dir,fext);

		final int dir_count = Count(dir,fext);

		final String[] aa = ArtistAlbum(dir);

		if (null != aa){

		    final String artist = aa[0];
		    final String album = aa[1];

		    try {
			for (File file : dir.listFiles(new FextFilter(fext))){

			    UpdateTag(file,artist,album);
			}

			System.exit(0);
		    }
		    catch (Exception exc){
			exc.printStackTrace();
			System.exit(1);
		    }
		}
		else {
		    err.printf("Unable to determine artist/album from '%s'.%n",dir.getPath());
		    System.exit(1);
		}
	    }
	    else if (dir.isDirectory()){

		fext = Fext(dir,fext);

		final int dir_count = Count(dir,fext);

		final String[] aa = ArtistAlbum(dir);

		if (null != aa){

		    final String artist = aa[0];
		    final String album = aa[1];

		    final API api_release = new API(Entity.RELEASE);

		    Document response = null;
		    try {
			/*
			 * Search for 'reid'
			 */
			String query = String.format("\"%s\" AND artist:\"%s\" AND format:CD",album,artist);

			response = api_release.search(query);

			Element metadata = response.getDocumentElement();

			if (print){
			    out.printf("Request: %s%n",response.getUserData(API.DOM_HTTP_REQUEST));
			    out.printf("Response: %s%n",response.getUserData(API.DOM_HTTP_STATUS));
			    out.println();
			    api_release.prettyPrint(response,out);
			    out.println();
			    System.exit(0);
			}
			else if (null != metadata){

			    err.printf("# %s/%s%n",artist,album);

			    NodeList release_list = metadata.getFirstChild().getChildNodes();

			    int release_list_count = release_list.getLength();

			    if (0 < release_list_count){

				Update(dir,fext,artist,album,api_release,response,release_list_count,release_list);
			    }
			    else {
				err.printf("Error, 'release-list' count %d.%n",release_list.getLength());
				err.println();
				err.printf("Request: %s%n",response.getUserData(API.DOM_HTTP_REQUEST));
				err.printf("Response: %s%n",response.getUserData(API.DOM_HTTP_STATUS));
				err.println();
				api_release.prettyPrint(response,err);
				err.println();
				System.exit(1);
			    }
			}
			System.exit(0);
		    }
		    catch (Exception any){

			if (null != response){

			    err.println();
			    api_release.prettyPrint(response,err);
			    err.println();
			    err.printf("Request: %s%n",response.getUserData(API.DOM_HTTP_REQUEST));
			    err.printf("Response: %s%n",response.getUserData(API.DOM_HTTP_STATUS));
			    err.println();
			    any.printStackTrace();
			    System.exit(1);
			}
			else {
			    any.printStackTrace();
			    System.exit(1);
			}
		    }
		}
		else {
		    err.printf("Unable to determine artist/album from '%s'.%n",dir.getPath());
		    System.exit(1);
		}
	    }
	    else {
		usage();
	    }
	}
	else {
	    usage();
	}
    }
    private final static boolean PrintTag(File file)
	throws CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, IOException, FieldDataInvalidException, CannotWriteException
    {
	AudioFile f = AudioFileIO.read(file);
	Tag tag = f.getTag();

	String artist = null, album = null, track = null, title = null;

	for (String string : tag.getAll(FieldKey.ARTIST)){
	    if (null != string){

		artist = string;
		break;
	    }
	}
	for (String string : tag.getAll(FieldKey.ALBUM)){

	    if (null != string){
		album = string;
		break;
	    }
	}
	for (String string : tag.getAll(FieldKey.TRACK)){

	    if (null != string){
		track = string;
		break;
	    }
	}
	for (String string : tag.getAll(FieldKey.TITLE)){

	    if (null != string){
		title = string;
		break;
	    }
	}

	if (null != track && null != artist && null != album && null != title){

	    out.printf("%s : %s : %s : %s : %s%n",file.toPath(),track,artist,album,title);

	    return true;
	}
	else {
	    out.println("Tag data not found.");

	    return false;
	}
    }
    private final static void UpdateTag(File file, String artist, String album, int pos, int num, String title)
	throws CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, IOException, FieldDataInvalidException, CannotWriteException
    {
	AudioFile f = AudioFileIO.read(file);
	Tag tag = f.getTagOrCreateAndSetDefault();

	tag.setField(FieldKey.ARTIST,artist);
	tag.setField(FieldKey.ALBUM,album);
	tag.setField(FieldKey.TRACK,Integer.toString(num));
	tag.setField(FieldKey.TITLE,title);
	f.commit();
    }
    private final static String[] SplitTag(String name){
	String[] re = new String[2];
	{
	    int idx0 = name.indexOf('.');
	    int idx1 = name.lastIndexOf('.');
	    if (0 < idx0 && idx0 < idx1){
		re[0] = name.substring(0,idx0).replace('.',' ').trim();
		re[1] = name.substring(idx0+1,idx1).replace('_',' ').trim();
		return re;
	    }
	    else {
		throw new IllegalArgumentException(name);
	    }
	}
    }
    private final static void UpdateTag(File file, String artist, String album)
	throws CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, IOException, FieldDataInvalidException, CannotWriteException
    {
	String track = null, title = null;
	{
	    String name = file.getName();
	    String[] pieces = SplitTag(name);
	    if (null != pieces){

		track = pieces[0];

		title = pieces[1];
	    }
	    else {
		throw new IllegalStateException(name);
	    }
	}

	AudioFile f = AudioFileIO.read(file);
	Tag tag = f.getTagOrCreateAndSetDefault();

	tag.setField(FieldKey.ARTIST,artist);
	tag.setField(FieldKey.ALBUM,album);
	tag.setField(FieldKey.TRACK,track);
	tag.setField(FieldKey.TITLE,title);
	f.commit();

	err.printf("U %s : %s : %s : %s : %s%n",file.toPath(),artist,album,track,title);
    }
    private final static boolean Accept(String artist, String album, API api, Element release)
	throws IOException
    {
	try {
	    String country = release.getElementsByTagName("country").item(0).getTextContent();

	    out.println();
	    api.prettyPrint(release,out);
	    out.printf("\n\tAccept (%s: %s [%s])? [Yn]%n",artist,album,country);
	}
	catch (Exception exc){
	    out.println();
	    api.prettyPrint(release,out);
	    out.printf("\n\tAccept (%s: %s)? [Yn]%n",artist,album);
	}
	out.flush();
	switch(in.read()){
	case '\r':
	case '\n':
	case 'y':
	case 'Y':
	    while (0 < in.available()){
		in.read();
	    }
	    out.println();
	    return true;
	default:
	    while (0 < in.available()){
		in.read();
	    }
	    out.println();
	    return false;
	}
    }
    private final static void Update(File dir, String fext, 
				     String artist, String album,
				     API api_release, Document response, 
				     int release_list_count, NodeList release_list)
	throws CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, IOException, FieldDataInvalidException, CannotWriteException
    {
	int release_list_last = (release_list_count-1);
	_release_loop:
	for (int release_list_ix = 0; release_list_ix < release_list_count; release_list_ix++){

	    Element el_release = (Element)release_list.item(release_list_ix);
	    /*
	     * don't auto-select when there's multiple possibilities
	     */
	    if (1 == release_list_count || Accept(artist,album,api_release,el_release)){

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
		    _tracks_loop:
		    for (cc = 0; cc < count; cc++){
			Element track = (Element)tracks_list.item(cc);

			int position = Integer.parseInt(track.getElementsByTagName("position").item(0).getTextContent());
			int number = Integer.parseInt(track.getElementsByTagName("number").item(0).getTextContent());
			String title = track.getElementsByTagName("title").item(0).getTextContent();

			/*
			 */
			File source_file = new File(dir,String.format(FilenameFormat_In,position,fext));
			if (source_file.isFile()){

			    Path source = source_file.toPath();

			    File target_file = new File(dir,String.format(FilenameFormat,number,title.replace('/','-'),fext));

			    Path target = target_file.toPath();

			    Files.move(source,target);

			    err.printf("M '%s' '%s'%n",source,target);

			    UpdateTag(target_file,artist,album,position,number,title);
			}
			else {
			    source_file = new File(dir,String.format(FilenameFormat_Old,number,title.replace('/','-'),fext));
			    if (source_file.isFile()){

				Path source = source_file.toPath();

				File target_file = new File(dir,String.format(FilenameFormat,number,title.replace('/','-'),fext));

				Path target = target_file.toPath();

				Files.move(source,target);

				err.printf("M '%s' '%s'%n",source,target);

				UpdateTag(target_file,artist,album,position,number,title);
			    }
			    else {
				source_file = new File(dir,String.format(FilenameFormat,number,title.replace('/','-'),fext));

				if (source_file.isFile()){

				    UpdateTag(source_file,artist,album,position,number,title);

				    err.printf("U '%s'%n",source_file.toPath());
				}
				else {

				    err.printf("Error, file not found '%s'%n",source_file.toPath());

				    System.exit(1);
				}
			    }
			}
		    }
		}
		break _release_loop;
	    }
	}
    }
    private final static String Fext(File dir, String fext){
	boolean fext_valid = false;
	String fext_replace = null;
	/*
	 * Validate or replace the argument
	 */
	for (File file : dir.listFiles()){

	    String file_ext = Fext(file.getName());

	    if (fext.equals(file_ext)){

		fext_valid = true;
	    }
	    else if (null == fext_replace){

		fext_replace = file_ext;
	    }
	}

	if (fext_valid)
	    return fext;
	else
	    return fext_replace;
    }
    private final static String Fext(String name){
	int lidx = name.lastIndexOf('.');
	if (0 < lidx)
	    return name.substring(lidx+1);
	else
	    throw new IllegalArgumentException(name);
    }
    private final static int Count(File dir, String fext){
	int count = 0;
	for (File file : dir.listFiles()){
	    String check = Fext(file.getName());
	    if (check.equals(fext)){
		count += 1;
	    }
	}
	return count;
    }

    static class FextFilter
	implements java.io.FilenameFilter 
    {
	final String fext, filter;

	FextFilter(String fext){
	    super();
	    this.fext = fext;
	    this.filter = ("."+fext);
	}

	public boolean accept(File dir, String name){

	    return name.endsWith(this.filter);
	}
    }
}
