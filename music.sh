#!/bin/bash

name=$(basename $0 .sh)

log=${name}.log

rm -f ${log}

for dir in $(cat ${name}.dlist | sed 's% %___%g')
do
    dir="$(echo ${dir} | sed 's%___% %g')"
    cat<<EOF>>${log}
# 2>&1 java -jar cddb.jar "${dir}" 
EOF
    2>&1 java -jar cddb.jar "${dir}" $* | tee ${log}
    if read -p "Continue? [Yn] " -s test && [ 'n' = "${test}" ]
    then
	echo
	break
    else
	echo
    fi
done
