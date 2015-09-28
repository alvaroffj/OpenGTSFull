#!/bin/bash
export GTS_HOME=/home/dev/OpenGTSFull
$GTS_HOME/bin/runserver.sh -s GTX -kill
git pull
ant GTX
$GTS_HOME/bin/runserver.sh -s GTX
