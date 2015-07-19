/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryArea implements Query<QueryArea> {
    AID("aid"),
    ALIAS("alias"),
    AREA("area"),
    BEGIN("begin"),
    COMMENT("comment"),
    END("end"),
    ENDED("ended"),
    SORTNAME("sortname"),
    ISO("iso"),
    ISO1("iso1"),
    ISO2("iso2"),
    ISO3("iso3");


    public final String parameter;


    QueryArea(String parameter){
        this.parameter = parameter;
    }
}
