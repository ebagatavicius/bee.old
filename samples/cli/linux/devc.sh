#/bin/bash

java -cp lib/*:war/WEB-INF/lib/guava-21.0.jar:/opt/gwt/*:src:war/WEB-INF/classes -Xms1024m -Xmx2048m com.google.gwt.dev.Compiler -draftCompile com.butent.bee.Dev

/usr/bin/notify-send -t 10000 "GWT compile $RANDOM" "compile process ended"
