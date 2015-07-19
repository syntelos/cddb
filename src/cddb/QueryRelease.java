/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum QueryRelease implements Query<QueryRelease> {
    ARID("arid"),
    ARTIST("artist"),
    ARTISTNAME("artistname"),
    COMMENT("comment"),
    CREDITNAME("creditname"),
    PRIMARYTYPE("primarytype"),
    RGID("rgid"),
    RELEASEGROUP("releasegroup"),
    RELEASEGROUPACCENT("releasegroupaccent"),
    RELEASES("releases"),
    RELEASE("release"),
    REID("reid"),
    SECONDARYTYPE("secondarytype"),
    STATUS("status"),
    TAG("tag");


    public final String parameter;


    QueryRelease(String parameter){
        this.parameter = parameter;
    }
}
