#!/bin/bash
cd /media/bnovo/Bee
echo start gwt dev compilator `date` $COMCOUNTLEFT
java -cp ./lib/*:./war/WEB-INF/lib/guava-17.0.jar:/opt/gwt-2.6.1/*:./src:./war/WEB-INF/classes -Xmx512m com.google.gwt.dev.Compiler -draftCompile com.butent.bee.Dev

if [ -z "${COMCOUNTLEFT}" ]; then
export COMCOUNTLEFT="#####"
echo Please clean cache
fi
export DEPCOUNTLEFT=${COMCOUNTLEFT%?}
echo end deploy `date` $COMCOUNTLEFT

free
cd