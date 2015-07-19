/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryArtist implements Query<QueryArtist> {
    AREA("area"),
    BEGINAREA("beginarea"),
    ENDAREA("endarea"),
    ARID("arid"),
    ARTIST("artist"),
    ARTISTACCENT("artistaccent"),
    ALIAS("alias"),
    BEGIN("begin"),
    COMMENT("comment"),
    COUNTRY("country"),
    END("end"),
    ENDED("ended"),
    GENDER("gender"),
    IPI("ipi"),
    SORTNAME("sortname"),
    TAG("tag");


    public final String parameter;


    QueryArtist(String parameter){
        this.parameter = parameter;
    }
}
