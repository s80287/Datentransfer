#!/bin/bash
if [ "$1" = "server" ]
then
	java -cp bin:s80287/bin filetransferUDP.Server $@
elif [ "$1" = "client" ]
then
	java -cp bin:s80287/bin filetransferUDP.Client $@
else
	echo "First argument is either server or client!"
fi

# copy file
java -cp bin:s12345/bin FileCopy $1 $2 $3 $4 > /dev/null