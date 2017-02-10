#/bin/bash
pwd
java -cp lib/*:war/WEB-INF/lib/guava-21.0.jar:/opt/gwt/*:src:war/WEB-INF/classes -Xmx2g com.google.gwt.dev.Compiler com.butent.bee.Bee
/usr/bin/notify-send -t 10000 "GWT compile $RANDOM" "full compile process ended"
