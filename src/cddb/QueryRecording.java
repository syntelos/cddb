/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryRecording implements Query<QueryRecording> {
    ARID("arid"),
    ARTIST("artist"),
    ARTISTNAME("artistname"),
    CREDITNAME("creditname"),
    COMMENT("comment"),
    COUNTRY("country"),
    DATE("date"),
    DUR("dur"),
    FORMAT("format"),
    ISRC("isrc"),
    NUMBER("number"),
    POSITION("position"),
    PRIMARYTYPE("primarytype"),
    PUID("puid"),
    QDUR("qdur"),
    RECORDING("recording"),
    RECORDINGACCENT("recordingaccent"),
    REID("reid"),
    RELEASE("release"),
    RGID("rgid"),
    RID("rid"),
    SECONDARYTYPE("secondarytype"),
    STATUS("status"),
    TID("tid"),
    TNUM("tnum"),
    TRACKS("tracks"),
    TRACKSRELEASE("tracksrelease"),
    TAG("tag"),
    VIDEO("video");


    public final String parameter;


    QueryRecording(String parameter){
        this.parameter = parameter;
    }
}
