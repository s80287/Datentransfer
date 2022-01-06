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