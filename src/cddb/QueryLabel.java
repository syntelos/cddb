/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryLabel implements Query<QueryLabel> {
    ALIA("alia"),
    AREA("area"),
    BEGIN("begin"),
    CODE("code"),
    COMMENT("comment"),
    COUNTRY("country"),
    END("end"),
    ENDED("ended"),
    IPI("ipi"),
    LABEL("label"),
    LABELACCENT("labelaccent"),
    LAID("laid"),
    SORTNAME("sortname"),
    TAG("tag");


    public final String parameter;


    QueryLabel(String parameter){
        this.parameter = parameter;
    }
}
