/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id$
 *
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package org.jaudiotagger.audio.mp3;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.logging.*;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagNotFoundException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.lyrics3.AbstractLyrics3;
import org.jaudiotagger.tag.reference.ID3V2Version;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

/**
 * This class represents a physical MP3 File
 */
public class MP3File extends AudioFile
{
    private static final int MINIMUM_FILESIZE = 150;

    protected static AbstractTagDisplayFormatter tagFormatter;

    /**
     * the ID3v2 tag that this file contains.
     */
    private AbstractID3v2Tag id3v2tag = null;

    /**
     * Representation of the idv2 tag as a idv24 tag
     */
    private ID3v24Tag id3v2Asv24tag = null;

    /**
     * The Lyrics3 tag that this file contains.
     */
    private AbstractLyrics3 lyrics3tag = null;


    /**
     * The ID3v1 tag that this file contains.
     */
    private ID3v1Tag id3v1tag = null;

    /**
     * Creates a new empty MP3File datatype that is not associated with a
     * specific file.
     */
    public MP3File()
    {
    }

    /**
     * Creates a new MP3File datatype and parse the tag from the given filename.
     *
     * @param filename MP3 file
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     * @throws org.jaudiotagger.audio.exceptions.ReadOnlyFileException
     * @throws org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
     */
    public MP3File(String filename) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException
    {
        this(new File(filename));
    }


    /* Load ID3V1tag if exists */
    public static final int LOAD_IDV1TAG = 2;

    /* Load ID3V2tag if exists */
    public static final int LOAD_IDV2TAG = 4;

    /**
     * This option is currently ignored
     */
    public static final int LOAD_LYRICS3 = 8;

    public static final int LOAD_ALL = LOAD_IDV1TAG | LOAD_IDV2TAG | LOAD_LYRICS3;

    /**
     * Creates a new MP3File dataType and parse the tag from the given file
     * Object, files must be writable to use this constructor.
     *
     * @param file        MP3 file
     * @param loadOptions decide what tags to load
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     * @throws org.jaudiotagger.audio.exceptions.ReadOnlyFileException
     * @throws org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
     */
    public MP3File(File file, int loadOptions) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException
    {
        this(file, loadOptions, false);
    }

    /**
     * Read v1 tag
     *
     * @param file
     * @param newFile
     * @param loadOptions
     * @throws IOException
     */
    private void readV1Tag(File file, RandomAccessFile newFile, int loadOptions) throws IOException
    {
        if ((loadOptions & LOAD_IDV1TAG) != 0)
        {
            logger.finer("Attempting to read id3v1tags");
            try
            {
                id3v1tag = new ID3v11Tag(newFile, file.getName());
            }
            catch (TagNotFoundException ex)
            {
                logger.config("No ids3v11 tag found");
            }

            try
            {
                if (id3v1tag == null)
                {
                    id3v1tag = new ID3v1Tag(newFile, file.getName());
                }
            }
            catch (TagNotFoundException ex)
            {
                logger.config("No id3v1 tag found");
            }
        }
    }

    /**
     * Read V2tag if exists
     *
     * TODO:shouldn't we be handing TagExceptions:when will they be thrown
     *
     * @param file
     * @param loadOptions
     * @throws IOException
     * @throws TagException
     */
    private void readV2Tag(File file, int loadOptions, int startByte) throws IOException, TagException
    {
        //We know where the actual Audio starts so load all the file from start to that point into
        //a buffer then we can read the IDv2 information without needing any more File I/O
        if (startByte >= AbstractID3v2Tag.TAG_HEADER_LENGTH)
        {
            logger.finer("Attempting to read id3v2tags");
            FileInputStream fis = null;
            FileChannel fc = null;
            ByteBuffer bb;
            try
            {
                fis = new FileInputStream(file);
                fc = fis.getChannel();
                bb = fc.map(FileChannel.MapMode.READ_ONLY,0,startByte);
            }
            //#JAUDIOTAGGER-419:If reading networked file map can fail so just copy bytes instead
            catch(IOException ioe)
            {
                bb =  ByteBuffer.allocate(startByte);
                fc.read(bb,0);
            }
            finally
            {
                if (fc != null)
                {
                    fc.close();
                }

                if (fis != null)
                {
                    fis.close();
                }
            }

            try
            {
                bb.rewind();

                if ((loadOptions & LOAD_IDV2TAG) != 0)
                {
                    logger.config("Attempting to read id3v2tags");
                    try
                    {
                        this.setID3v2Tag(new ID3v24Tag(bb, file.getName()));
                    }
                    catch (TagNotFoundException ex)
                    {
                        logger.config("No id3v24 tag found");
                    }

                    try
                    {
                        if (id3v2tag == null)
                        {
                            this.setID3v2Tag(new ID3v23Tag(bb, file.getName()));
                        }
                    }
                    catch (TagNotFoundException ex)
                    {
                        logger.config("No id3v23 tag found");
                    }

                    try
                    {
                        if (id3v2tag == null)
                        {
                            this.setID3v2Tag(new ID3v22Tag(bb, file.getName()));
                        }
                    }
                    catch (TagNotFoundException ex)
                    {
                        logger.config("No id3v22 tag found");
                    }
                }
            }
            finally
            {
                //Workaround for 4724038 on Windows
                bb.clear();
                if (bb != null && bb.isDirect())
                {
                    ((sun.nio.ch.DirectBuffer) bb).cleaner().clean();
                }
            }
        }
        else
        {
            logger.config("Not enough room for valid id3v2 tag:" + startByte);
        }
    }

    /**
     * Read lyrics3 Tag
     *
     * TODO:not working
     *
     * @param file
     * @param newFile
     * @param loadOptions
     * @throws IOException
     */
    private void readLyrics3Tag(File file, RandomAccessFile newFile, int loadOptions) throws IOException
    {
        /*if ((loadOptions & LOAD_LYRICS3) != 0)
        {
            try
            {
                lyrics3tag = new Lyrics3v2(newFile);
            }
            catch (TagNotFoundException ex)
            {
            }
            try
            {
                if (lyrics3tag == null)
                {
                    lyrics3tag = new Lyrics3v1(newFile);
                }
            }
            catch (TagNotFoundException ex)
            {
            }
        }
        */
    }


    /**
     *
     * @param startByte
     * @param endByte
     * @return
     * @throws Exception
     *
     * @return true if all the bytes between in the file between startByte and endByte are null, false
     * otherwise
     */
    private boolean isFilePortionNull(int startByte, int endByte) throws IOException
    {
        logger.config("Checking file portion:" + Hex.asHex(startByte) + ":" + Hex.asHex(endByte));
        FileInputStream fis=null;
        FileChannel     fc=null;
        try
        {
            fis = new FileInputStream(file);
            fc = fis.getChannel();
            fc.position(startByte);
            ByteBuffer bb = ByteBuffer.allocateDirect(endByte - startByte);
            fc.read(bb);
            while(bb.hasRemaining())
            {
                if(bb.get()!=0)
                {
                    return false;
                }
            }
        }
        finally
        {
            if (fc != null)
            {
                fc.close();
            }

            if (fis != null)
            {
                fis.close();
            }
        }
        return true;
    }
    /**
     * Regets the audio header starting from start of file, and write appropriate logging to indicate
     * potential problem to user.
     *
     * @param startByte
     * @param firstHeaderAfterTag
     * @return
     * @throws IOException
     * @throws InvalidAudioFrameException
     */
    private MP3AudioHeader checkAudioStart(long startByte, MP3AudioHeader firstHeaderAfterTag) throws IOException, InvalidAudioFrameException
    {
        MP3AudioHeader headerOne;
        MP3AudioHeader headerTwo;

        logger.warning(ErrorMessage.MP3_ID3TAG_LENGTH_INCORRECT.getMsg(file.getPath(), Hex.asHex(startByte), Hex.asHex(firstHeaderAfterTag.getMp3StartByte())));

        //because we cant agree on start location we reread the audioheader from the start of the file, at least
        //this way we cant overwrite the audio although we might overwrite part of the tag if we write this file
        //back later
        headerOne = new MP3AudioHeader(file, 0);
        logger.config("Checking from start:" + headerOne);

        //Although the id3 tag size appears to be incorrect at least we have found the same location for the start
        //of audio whether we start searching from start of file or at the end of the alleged of file so no real
        //problem
        if (firstHeaderAfterTag.getMp3StartByte() == headerOne.getMp3StartByte())
        {
            logger.config(ErrorMessage.MP3_START_OF_AUDIO_CONFIRMED.getMsg(file.getPath(),
                    Hex.asHex(headerOne.getMp3StartByte())));
            return firstHeaderAfterTag;
        }
        else
        {

            //We get a different value if read from start, can't guarantee 100% correct lets do some more checks
            logger.config((ErrorMessage.MP3_RECALCULATED_POSSIBLE_START_OF_MP3_AUDIO.getMsg(file.getPath(),
                            Hex.asHex(headerOne.getMp3StartByte()))));

            //Same frame count so probably both audio headers with newAudioHeader being the first one
            if (firstHeaderAfterTag.getNumberOfFrames() == headerOne.getNumberOfFrames())
            {
                logger.warning((ErrorMessage.MP3_RECALCULATED_START_OF_MP3_AUDIO.getMsg(file.getPath(),
                                Hex.asHex(headerOne.getMp3StartByte()))));
                return headerOne;
            }

            //If the size reported by the tag header is a little short and there is only nulls between the recorded value
            //and the start of the first audio found then we stick with the original header as more likely that currentHeader
            //DataInputStream not really a header
            if(isFilePortionNull((int) startByte,(int) firstHeaderAfterTag.getMp3StartByte()))
            {
                return firstHeaderAfterTag;
            }

            //Skip to the next header (header 2, counting from start of file)
            headerTwo = new MP3AudioHeader(file, headerOne.getMp3StartByte()
                    + headerOne.mp3FrameHeader.getFrameLength());

            //It matches the header we found when doing the original search from after the ID3Tag therefore it
            //seems that newAudioHeader was a false match and the original header was correct
            if (headerTwo.getMp3StartByte() == firstHeaderAfterTag.getMp3StartByte())
            {
                logger.warning((ErrorMessage.MP3_START_OF_AUDIO_CONFIRMED.getMsg(file.getPath(),
                                Hex.asHex(firstHeaderAfterTag.getMp3StartByte()))));
                return firstHeaderAfterTag;
            }

            //It matches the frameCount the header we just found so lends weight to the fact that the audio does indeed start at new header
            //however it maybe that neither are really headers and just contain the same data being misrepresented as headers.
            if (headerTwo.getNumberOfFrames() == headerOne.getNumberOfFrames())
            {
                logger.warning((ErrorMessage.MP3_RECALCULATED_START_OF_MP3_AUDIO.getMsg(file.getPath(),
                                Hex.asHex(headerOne.getMp3StartByte()))));
                return headerOne;
            }
            ///Doesnt match the frameCount lets go back to the original header
            else
            {
                logger.warning((ErrorMessage.MP3_RECALCULATED_START_OF_MP3_AUDIO.getMsg(file.getPath(),
                                Hex.asHex(firstHeaderAfterTag.getMp3StartByte()))));
                return firstHeaderAfterTag;
            }
        }
    }

    /**
     * Creates a new MP3File dataType and parse the tag from the given file
     * Object, files can be opened read only if required.
     *
     * @param file        MP3 file
     * @param loadOptions decide what tags to load
     * @param readOnly    causes the files to be opened readonly
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     * @throws org.jaudiotagger.audio.exceptions.ReadOnlyFileException
     * @throws org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
     */
    public MP3File(File file, int loadOptions, boolean readOnly) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException
    {
        RandomAccessFile newFile = null;
        try
        {
            this.file = file;

            //Check File accessibility
            newFile = checkFilePermissions(file, readOnly);

            //Read ID3v2 tag size (if tag exists) to allow audioHeader parsing to skip over tag
            long tagSizeReportedByHeader = AbstractID3v2Tag.getV2TagSizeIfExists(file);
            logger.config("TagHeaderSize:" + Hex.asHex(tagSizeReportedByHeader));
            audioHeader = new MP3AudioHeader(file, tagSizeReportedByHeader);

            //If the audio header is not straight after the end of the tag then search from start of file
            if (tagSizeReportedByHeader != ((MP3AudioHeader) audioHeader).getMp3StartByte())
            {
                logger.config("First header found after tag:" + audioHeader);
                audioHeader = checkAudioStart(tagSizeReportedByHeader, (MP3AudioHeader) audioHeader);
            }

            //Read v1 tags (if any)
            readV1Tag(file, newFile, loadOptions);

            //Read v2 tags (if any)
            readV2Tag(file, loadOptions, (int)((MP3AudioHeader) audioHeader).getMp3StartByte());

            //If we have a v2 tag use that, if we do not but have v1 tag use that
            //otherwise use nothing
            //TODO:if have both should we merge
            //rather than just returning specific ID3v22 tag, would it be better to return v24 version ?
            if (this.getID3v2Tag() != null)
            {
                tag = this.getID3v2Tag();
            }
            else if (id3v1tag != null)
            {
                tag = id3v1tag;
            }
        }
        finally
        {
            if (newFile != null)
            {
                newFile.close();
            }
        }
    }

    /**
     * Used by tags when writing to calculate the location of the music file
     *
     * @param file
     * @return the location within the file that the audio starts
     * @throws java.io.IOException
     * @throws org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
     */
    public long getMP3StartByte(File file) throws InvalidAudioFrameException, IOException
    {
        try
        {
            //Read ID3v2 tag size (if tag exists) to allow audio header parsing to skip over tag
            long startByte = AbstractID3v2Tag.getV2TagSizeIfExists(file);

            MP3AudioHeader audioHeader = new MP3AudioHeader(file, startByte);
            if (startByte != audioHeader.getMp3StartByte())
            {
                logger.config("First header found after tag:" + audioHeader);
                audioHeader = checkAudioStart(startByte, audioHeader);
            }
            return audioHeader.getMp3StartByte();
        }
        catch (InvalidAudioFrameException iafe)
        {
            throw iafe;
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
    }

    /**
     * Extracts the raw ID3v2 tag data into a file.
     *
     * This provides access to the raw data before manipulation, the data is written from the start of the file
     * to the start of the Audio Data. This is primarily useful for manipulating corrupted tags that are not
     * (fully) loaded using the standard methods.
     *
     * @param outputFile to write the data to
     * @return
     * @throws TagNotFoundException
     * @throws IOException
     */
    public File extractID3v2TagDataIntoFile(File outputFile) throws TagNotFoundException, IOException
    {
        int startByte = (int) ((MP3AudioHeader) audioHeader).getMp3StartByte();
        if (startByte >= 0)
        {

            //Read byte into buffer
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            ByteBuffer bb = ByteBuffer.allocate(startByte);
            fc.read(bb);

            //Write bytes to outputFile
            FileOutputStream out = new FileOutputStream(outputFile);
            out.write(bb.array());
            out.close();
            fc.close();
            fis.close();
            return outputFile;
        }
        throw new TagNotFoundException("There is no ID3v2Tag data in this file");
    }

    /**
     * Return audio header
     * @return
     */
    public MP3AudioHeader getMP3AudioHeader()
    {
        return (MP3AudioHeader) getAudioHeader();
    }

    /**
     * Returns true if this datatype contains an <code>Id3v1</code> tag
     *
     * @return true if this datatype contains an <code>Id3v1</code> tag
     */
    public boolean hasID3v1Tag()
    {
        return (id3v1tag != null);
    }

    /**
     * Returns true if this datatype contains an <code>Id3v2</code> tag
     *
     * @return true if this datatype contains an <code>Id3v2</code> tag
     */
    public boolean hasID3v2Tag()
    {
        return (id3v2tag != null);
    }

    /**
     * Returns true if this datatype contains a <code>Lyrics3</code> tag
     * TODO disabled until Lyrics3 fixed
     * @return true if this datatype contains a <code>Lyrics3</code> tag
     */
    /*
    public boolean hasLyrics3Tag()
    {
        return (lyrics3tag != null);
    }
    */

    /**
     * Creates a new MP3File datatype and parse the tag from the given file
     * Object.
     *
     * @param file MP3 file
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     * @throws org.jaudiotagger.audio.exceptions.ReadOnlyFileException
     * @throws org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
     */
    public MP3File(File file) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException
    {
        this(file, LOAD_ALL);
    }

    /**
     * Sets the ID3v1(_1)tag to the tag provided as an argument.
     *
     * @param id3v1tag
     */
    public void setID3v1Tag(ID3v1Tag id3v1tag)
    {
        logger.config("setting tagv1:v1 tag");
        this.id3v1tag = id3v1tag;
    }

    public void setID3v1Tag(Tag id3v1tag)
    {
        logger.config("setting tagv1:v1 tag");
        this.id3v1tag = (ID3v1Tag) id3v1tag;
    }

    /**
     * Sets the <code>ID3v1</code> tag for this dataType. A new
     * <code>ID3v1_1</code> dataType is created from the argument and then used
     * here.
     *
     * @param mp3tag Any MP3Tag dataType can be used and will be converted into a
     *               new ID3v1_1 dataType.
     */
    public void setID3v1Tag(AbstractTag mp3tag)
    {
        logger.config("setting tagv1:abstract");
        id3v1tag = new ID3v11Tag(mp3tag);
    }

    /**
     * Returns the <code>ID3v1</code> tag for this dataType.
     *
     * @return the <code>ID3v1</code> tag for this dataType
     */
    public ID3v1Tag getID3v1Tag()
    {
        return id3v1tag;
    }

    /**
     * Sets the <code>ID3v2</code> tag for this dataType. A new
     * <code>ID3v2_4</code> dataType is created from the argument and then used
     * here.
     *
     * @param mp3tag Any MP3Tag dataType can be used and will be converted into a
     *               new ID3v2_4 dataType.
     */
    public void setID3v2Tag(AbstractTag mp3tag)
    {
        id3v2tag = new ID3v24Tag(mp3tag);

    }

    /**
     * Sets the v2 tag to the v2 tag provided as an argument.
     * Also store a v24 version of tag as v24 is the interface to be used
     * when talking with client applications.
     *
     * @param id3v2tag
     */
    public void setID3v2Tag(AbstractID3v2Tag id3v2tag)
    {
        this.id3v2tag = id3v2tag;
        if (id3v2tag instanceof ID3v24Tag)
        {
            this.id3v2Asv24tag = (ID3v24Tag) this.id3v2tag;
        }
        else
        {
            this.id3v2Asv24tag = new ID3v24Tag(id3v2tag);
        }
    }

    /**
     * Set v2 tag ,don't need to set v24 tag because saving
     *

     * @param id3v2tag
     */
    //TODO temp its rather messy
    public void setID3v2TagOnly(AbstractID3v2Tag id3v2tag)
    {
        this.id3v2tag = id3v2tag;
        this.id3v2Asv24tag = null;
    }

    /**
     * Returns the <code>ID3v2</code> tag for this datatype.
     *
     * @return the <code>ID3v2</code> tag for this datatype
     */
    public AbstractID3v2Tag getID3v2Tag()
    {
        return id3v2tag;
    }

    /**
     * @return a representation of tag as v24
     */
    public ID3v24Tag getID3v2TagAsv24()
    {
        return id3v2Asv24tag;
    }

    /**
     * Sets the <code>Lyrics3</code> tag for this dataType. A new
     * <code>Lyrics3v2</code> dataType is created from the argument and then
     *
     * used here.
     *
     * @param mp3tag Any MP3Tag dataType can be used and will be converted into a
     *               new Lyrics3v2 dataType.
     */
    /*
    public void setLyrics3Tag(AbstractTag mp3tag)
    {
        lyrics3tag = new Lyrics3v2(mp3tag);
    }
    */

    /**
     *
     *
     * @param lyrics3tag
     */
    /*
    public void setLyrics3Tag(AbstractLyrics3 lyrics3tag)
    {
        this.lyrics3tag = lyrics3tag;
    }
    */

    /**
     * Returns the <code>ID3v1</code> tag for this datatype.
     *
     * @return the <code>ID3v1</code> tag for this datatype
     */
    /*
    public AbstractLyrics3 getLyrics3Tag()
    {
        return lyrics3tag;
    }
    */

    /**
     * Remove tag from file
     *
     * @param mp3tag
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void delete(AbstractTag mp3tag) throws FileNotFoundException, IOException
    {
        RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
        mp3tag.delete(raf);
        raf.close();
        if(mp3tag instanceof ID3v1Tag)
        {
            id3v1tag=null;
        }

        if(mp3tag instanceof AbstractID3v2Tag)
        {
            id3v2tag=null;
        }
    }

    /**
     * Saves the tags in this dataType to the file referred to by this dataType.
     *
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public void save() throws IOException, TagException
    {
        save(this.file);
    }

    /**
     * Overridden for compatibility with merged code
     *
     * @throws CannotWriteException
     */
    public void commit() throws CannotWriteException
    {
        try
        {
            save();
        }
        catch (IOException ioe)
        {
            throw new CannotWriteException(ioe);
        }
        catch (TagException te)
        {
            throw new CannotWriteException(te);
        }
    }

    /**
     * Check can write to file
     *
     * @param file
     * @throws IOException
     */
    public void precheck(File file) throws IOException
    {
        if (!file.exists())
        {
            logger.severe(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_NOT_FOUND.getMsg(file.getName()));
            throw new IOException(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_NOT_FOUND.getMsg(file.getName()));
        }

        if (!file.canWrite())
        {
            logger.severe(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(file.getName()));
            throw new IOException(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(file.getName()));
        }

        if (file.length() <= MINIMUM_FILESIZE)
        {
            logger.severe(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(file.getName()));
            throw new IOException(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(file.getName()));
        }
    }

    /**
     * Saves the tags in this dataType to the file argument. It will be saved as
     * TagConstants.MP3_FILE_SAVE_WRITE
     *
     * @param fileToSave file to save the this dataTypes tags to
     * @throws FileNotFoundException if unable to find file
     * @throws IOException           on any I/O error
     */
    public void save(File fileToSave) throws IOException
    {
        //Ensure we are dealing with absolute filepaths not relative ones
        File file = fileToSave.getAbsoluteFile();

        logger.config("Saving  : " + file.getPath());

        //Checks before starting write
        precheck(file);

        RandomAccessFile rfile = null;
        try
        {
            //ID3v2 Tag
            if (TagOptionSingleton.getInstance().isId3v2Save())
            {
                if (id3v2tag == null)
                {
                    rfile = new RandomAccessFile(file, "rw");
                    (new ID3v24Tag()).delete(rfile);
                    (new ID3v23Tag()).delete(rfile);
                    (new ID3v22Tag()).delete(rfile);
                    logger.config("Deleting ID3v2 tag:"+file.getName());
                    rfile.close();
                }
                else
                {
                    logger.config("Writing ID3v2 tag:"+file.getName());
                    final MP3AudioHeader mp3AudioHeader = (MP3AudioHeader) this.getAudioHeader();
                    final long mp3StartByte = mp3AudioHeader.getMp3StartByte();
                    final long newMp3StartByte = id3v2tag.write(file, mp3StartByte);
                    if (mp3StartByte != newMp3StartByte) {
                        logger.config("New mp3 start byte: " + newMp3StartByte);
                        mp3AudioHeader.setMp3StartByte(newMp3StartByte);
                    }

                }
            }
            rfile = new RandomAccessFile(file, "rw");

            //Lyrics 3 Tag
            if (TagOptionSingleton.getInstance().isLyrics3Save())
            {
                if (lyrics3tag != null)
                {
                    lyrics3tag.write(rfile);
                }
            }
            //ID3v1 tag
            if (TagOptionSingleton.getInstance().isId3v1Save())
            {
                logger.config("Processing ID3v1");
                if (id3v1tag == null)
                {
                    logger.config("Deleting ID3v1");
                    (new ID3v1Tag()).delete(rfile);
                }
                else
                {
                    logger.config("Saving ID3v1");
                    id3v1tag.write(rfile);
                }
            }
        }
        catch (FileNotFoundException ex)
        {
            logger.log(Level.SEVERE, ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_NOT_FOUND.getMsg(file.getName()), ex);
            throw ex;
        }
        catch (IOException iex)
        {
            logger.log(Level.SEVERE, ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE.getMsg(file.getName(), iex.getMessage()), iex);
            throw iex;
        }
        catch (RuntimeException re)
        {
            logger.log(Level.SEVERE, ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE.getMsg(file.getName(), re.getMessage()), re);
            throw re;
        }
        finally
        {
            if (rfile != null)
            {
                rfile.close();
            }
        }
    }

    /**
     * Displays MP3File Structure
     */
    public String displayStructureAsXML()
    {
        createXMLStructureFormatter();
        tagFormatter.openHeadingElement("file", this.getFile().getAbsolutePath());
        if (this.getID3v1Tag() != null)
        {
            this.getID3v1Tag().createStructure();
        }
        if (this.getID3v2Tag() != null)
        {
            this.getID3v2Tag().createStructure();
        }
        tagFormatter.closeHeadingElement("file");
        return tagFormatter.toString();
    }

    /**
     * Displays MP3File Structure
     */
    public String displayStructureAsPlainText()
    {
        createPlainTextStructureFormatter();
        tagFormatter.openHeadingElement("file", this.getFile().getAbsolutePath());
        if (this.getID3v1Tag() != null)
        {
            this.getID3v1Tag().createStructure();
        }
        if (this.getID3v2Tag() != null)
        {
            this.getID3v2Tag().createStructure();
        }
        tagFormatter.closeHeadingElement("file");
        return tagFormatter.toString();
    }

    private static void createXMLStructureFormatter()
    {
        tagFormatter = new XMLTagDisplayFormatter();
    }

    private static void createPlainTextStructureFormatter()
    {
        tagFormatter = new PlainTextTagDisplayFormatter();
    }

    public static AbstractTagDisplayFormatter getStructureFormatter()
    {
        return tagFormatter;
    }

    /**
     * Set the Tag
     *
     * If the parameter tag is a v1tag then the v1 tag is set if v2tag then the v2tag.
     *
     * @param tag
     */
    public void setTag(Tag tag)
    {
        this.tag = tag;
        if (tag instanceof ID3v1Tag)
        {
            setID3v1Tag((ID3v1Tag) tag);
        }
        else
        {
            setID3v2Tag((AbstractID3v2Tag) tag);
        }
    }


    /** Create Default Tag
     *
     * @return
     */
    @Override
    public Tag createDefaultTag()
    {
        if(TagOptionSingleton.getInstance().getID3V2Version()==ID3V2Version.ID3_V24)
        {    
            return new ID3v24Tag();
        }
        else if(TagOptionSingleton.getInstance().getID3V2Version()==ID3V2Version.ID3_V23)
        {
            return new ID3v23Tag();
        }
        else if(TagOptionSingleton.getInstance().getID3V2Version()==ID3V2Version.ID3_V22)
        {
            return new ID3v22Tag();
        }
        //Default in case not set somehow
        return new ID3v24Tag();
    }

    /**
     * Convert tag from current version to another as specified by id3V2Version
     *
     * @return
     */
    public Tag convertTag(Tag tag, ID3V2Version id3V2Version)
    {
        if(tag instanceof ID3v24Tag)
        {
            switch(id3V2Version)
            {
                case ID3_V22:
                    return new ID3v22Tag((ID3v24Tag)tag);
                case ID3_V23:
                    return new ID3v23Tag((ID3v24Tag)tag);
                case ID3_V24:
                    return tag;
            }
        }
        else if(tag instanceof ID3v23Tag)
        {
            switch(id3V2Version)
            {
                case ID3_V22:
                    return new ID3v22Tag((ID3v23Tag)tag);
                case ID3_V23:
                    return tag;
                case ID3_V24:
                    return new ID3v24Tag((ID3v23Tag)tag);
            }
        }
        else if(tag instanceof ID3v22Tag)
        {
            switch(id3V2Version)
            {
                case ID3_V22:
                    return tag;
                case ID3_V23:
                    return new ID3v23Tag((ID3v22Tag)tag);
                case ID3_V24:
                    return new ID3v24Tag((ID3v22Tag)tag);
            }
        }
        return tag;
    }

    /**
     * Overidden to only consider ID3v2 Tag
     *
     * @return
     */
    @Override
    public Tag getTagOrCreateDefault()
    {
        Tag tag = getID3v2Tag();
        if(tag==null)
        {
            return createDefaultTag();
        }
        return tag;
    }


    /**
     * Get the ID3v2 tag and convert to preferred version or if the file doesn't have one at all
     * create a default tag of preferred version and set it. The file may already contain a ID3v1 tag but because
     * this is not terribly useful the v1tag is not considered for this problem.
     *
     * @return
     */
    @Override
    public Tag getTagAndConvertOrCreateAndSetDefault()
    {
        Tag tag = getTagOrCreateDefault();
        tag=convertTag(tag, TagOptionSingleton.getInstance().getID3V2Version());
        setTag(tag);
        return tag;
    }
}
