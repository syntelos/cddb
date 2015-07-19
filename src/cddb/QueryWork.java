/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryWork implements Query<QueryWork> {
    ALIAS("alias"),
    ARID("arid"),
    ARTIST("artist"),
    COMMENT("comment"),
    ISWC("iswc"),
    LANG("lang"),
    TAG("tag"),
    WID("wid"),
    WORK("work"),
    WORKACCENT("workaccent");


    public final String parameter;


    QueryWork(String parameter){
        this.parameter = parameter;
    }
}
