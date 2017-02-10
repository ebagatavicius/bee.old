@echo %time%
java -cp lib/*;war/WEB-INF/lib/guava-21.0.jar;e:/Marius/gwt/*;src;war/WEB-INF/classes -Xmx2g com.google.gwt.dev.Compiler -draftCompile com.butent.bee.Dev