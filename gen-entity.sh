#!/bin/bash

tgt=src/cddb/Entity.java

function camel {
    string=''
    for tok in $(echo $1 | sed 's/-/ /g')
    do
	b=$(echo $tok | sed 's/.//')
	a=$(echo $tok | sed "s%${b}%%" | tr '[a-z]' '[A-Z]')
	if [ -z "${string}" ]
	then
	    string="${a}${b}"
	else
	    string="${string}${a}${b}"
	fi
    done
    echo ${string}
}
function upcase {
    echo ${1} | sed 's/-/_/' | tr '[a-z]' '[A-Z]'
}

cat<<EOF>${tgt}
/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum Entity {
EOF

count=$(egrep -v '^#' mb-xws2-entities.txt | wc -l | awk '{print $1}')
cc=1
for entity in $(egrep -v '^#' mb-xws2-entities.txt )
do

    cname=$(camel ${entity})

    upname=$(upcase ${entity})

    classname="Query${cname}"

    if [ $cc -lt $count ]
    then
	cat<<EOF>>${tgt}
    ${upname}( "${entity}", ${classname}.class),
EOF
    else
	cat<<EOF>>${tgt}
    ${upname}( "${entity}", ${classname}.class);
EOF
    fi
    cc=$(( $cc + 1 ))
done

cat<<EOF>>${tgt}


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
EOF
