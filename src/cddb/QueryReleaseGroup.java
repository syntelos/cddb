/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryReleaseGroup implements Query<QueryReleaseGroup> {
    ARID("arid"),
    ARTIST("artist"),
    ARTISTNAME("artistname"),
    ASIN("asin"),
    BARCODE("barcode"),
    CATNO("catno"),
    COMMENT("comment"),
    COUNTRY("country"),
    CREDITNAME("creditname"),
    DATE("date"),
    DISCIDS("discids"),
    DISCIDSMEDIUM("discidsmedium"),
    FORMAT("format"),
    LAID("laid"),
    LABEL("label"),
    LANG("lang"),
    MEDIUMS("mediums"),
    PRIMARYTYPE("primarytype"),
    PUID("puid"),
    QUALITY("quality"),
    REID("reid"),
    RELEASE("release"),
    RELEASEACCENT("releaseaccent"),
    RGID("rgid"),
    SCRIPT("script"),
    SECONDARYTYPE("secondarytype"),
    STATUS("status"),
    TAG("tag"),
    TRACKS("tracks"),
    TRACKSMEDIUM("tracksmedium");


    public final String parameter;


    QueryReleaseGroup(String parameter){
        this.parameter = parameter;
    }
}
