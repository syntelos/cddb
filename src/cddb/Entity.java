/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum Entity {
    AREA( "area", QueryArea.class),
    ARTIST( "artist", QueryArtist.class),
    LABEL( "label", QueryLabel.class),
    RECORDING( "recording", QueryRecording.class),
    RELEASE( "release", QueryRelease.class),
    RELEASE_GROUP( "release-group", QueryReleaseGroup.class),
    WORK( "work", QueryWork.class);


    public final String path;

    public final Class query;


    Entity(String path, Class query){
	this.path = path;
        this.query = query;
    }


    public Class<Query> getQuery(){
        return (Class<Query>)query;
    }
}
