#!/bin/bash

project=$(basename $(pwd))

unset mcp
unset cp
for pel in $(2>/dev/null ls lib/*.jar)
do
    if [ -n "${cp}" ]
    then
	mcp="${mcp} ${pel}"
	cp="${cp}:${pel}"
    else
	mcp=${pel}
	cp=${pel}
    fi
done



if [ ! -d bin ]
then
    mkdir bin
else
    files=$(find bin -type f )
    if [ -n "${files}" ]
    then
	2>/dev/null rm ${files}
    fi
fi

sources=$(find src -type f -name '*.java')

if [ -z "${cp}" ]
then
    compile="javac -g -d bin ${sources}"
else
    compile="javac -cp ${cp} -g -d bin ${sources}"
fi

echo ${compile}
if ${compile}
then

    if [ -z "${cp}" ]
    then
	cat<<EOF>src/Manifest.mf
Main-Class: cddb.Main
EOF
    else
	cat<<EOF>src/Manifest.mf
Main-Class: cddb.Main
Class-Path: ${mcp}
EOF
    fi
    cd bin

    if [ -n "$(find . -type f )" ]
    then

	echo jar cmf ../src/Manifest.mf ../${project}.jar *
	if  jar cmf ../src/Manifest.mf ../${project}.jar *
	then
	    cd ..

	    ls -l ${project}.jar
	    exit 0
	else
	    exit 1
	fi
    else
	exit 1
    fi
else
    exit 1
fi

