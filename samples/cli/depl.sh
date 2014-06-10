#!/bin/bash
# UNIX DEPLPOY
## orka server configuration
clear
WAR_FOLDER=/media/bnovo/Bee/war
WAR_FILE=/tmp/Bee.war
ZIP_FILE=/tmp/Bee
echo start gwt compile `date` $DEPCOUNTLEFT
#. /media/bnovo/Bee/samples/cli/gwtc.sh
#. /media/bnovo/Bee/samples/cli/gwtc_dev.sh
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