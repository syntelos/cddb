#!/bin/bash

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

for line in $(egrep -v '^#' mb-xws2-query.txt )
do
    entity=$(echo ${line} | sed 's/=.*//')

    parameters=$(echo ${line} | sed 's/.*=//; s/,/ /g;')

    if [ -n "$(egrep ${entity} mb-xws2-entities.txt)" ]
    then

	cname=$(camel ${entity})

	classname="Query${cname}"

	tgt_f="src/cddb/${classname}.java"

	cat<<EOF>"${tgt_f}"
/*
 * CDDB via Musicbrainz
 * Copyright 2015 John Pritchard, Syntelos
 */
package cddb;

/**
 * 
 */
public enum ${classname} implements Query<${classname}> {
EOF
	count=$(echo $parameters | tr ' ' '\n' | wc -l | awk '{print $1}')
	cc=1
	for name in ${parameters}
	do
	    upname=$(upcase ${name})
	    if [ ${cc} -lt ${count} ]
	    then
		cat<<EOF>>"${tgt_f}"
    ${upname}("${name}"),
EOF
	    else
		cat<<EOF>>"${tgt_f}"
    ${upname}("${name}");
EOF
	    fi
	    cc=$(( $cc + 1 ))
	done
	cat<<EOF>>"${tgt_f}"


    public final String parameter;


    ${classname}(String parameter){
        this.parameter = parameter;
    }
}
EOF
        git add ${tgt_f}
    fi
done
