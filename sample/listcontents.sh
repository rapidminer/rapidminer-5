#/bin/sh


function extract_data
{
	echo "*** Listing entries in folder $1"
	{
		for FOLDER in $1/*
		do
			if [ -d ${FOLDER} ]
			then
				echo FOLDER `basename ${FOLDER}`
			fi
		done
	
		for IOO in $1/*.ioo $1/*.rmp
		do
			if [ -f ${IOO} ]
			then
				echo ENTRY `basename ${IOO}`
			fi
		done
	} > $1/CONTENTS	
	
	for FOLDER in $1/*
	do
		if [ -d ${FOLDER} ]
		then
		   extract_data ${FOLDER}
		fi
	done
}

extract_data .
