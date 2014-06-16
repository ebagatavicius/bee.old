#!/bin/bash
# UNIX DEPLPOY
## orka server configuration
clear
WAR_FOLDER=/media/bnovo/Bee/war
WAR_FILE=/tmp/Bee.war
ZIP_FILE=/tmp/Bee
APP_FOLDER=/opt/glassfish4/glassfish/domains/domain1/applications/Bee
echo start b-novo deploy `date` $DEPCOUNTLEFT

if [ "$1" == "full" ]; then
/media/bnovo/Bee/samples/cli/gwtc.sh
fi

if [ "$1" == "dev" ]; then
 /media/bnovo/Bee/samples/cli/gwtc_dev.sh
fi

if [ "$1" == "reconfig" ]; then
 echo reconfig start
 cp -u -v -r $WAR_FOLDER/css/*  $APP_FOLDER/css
 cp -u -v -r $WAR_FOLDER/images/*  $APP_FOLDER/images
 cp -u -v -r $WAR_FOLDER/js/*  $APP_FOLDER/js
 cp -u -v -r $WAR_FOLDER/WEB-INF/config/*  $APP_FOLDER/WEB-INF/config
 cp -u -v  $WAR_FOLDER/WEB-INF/web.xml  $APP_FOLDER/WEB-INF/web.xml
 echo reconfig completed
 return 0
fi
#
## Creating zip file due read-only /media/bnovo/ file system
echo start create war `date`
cd $WAR_FOLDER
zip -r -0 -q $ZIP_FILE .

## Rename zip to war
mv $ZIP_FILE.zip $WAR_FILE
echo start deploy `date`
/opt/glassfish4/glassfish/bin/asadmin deploy --force=true --name Bee --contextroot Bee $WAR_FILE


if [ -z "${DEPCOUNTLEFT}" ]; then
export DEPCOUNTLEFT="####"
echo restart server due deploy count `date`
/opt/glassfish4/glassfish/bin/asadmin stop-domain domain1
/opt/glassfish4/glassfish/bin/asadmin start-domain domain1
fi
export DEPCOUNTLEFT=${DEPCOUNTLEFT%?}
echo end deploy `date` $DEPCOUNTLEFT
free