@echo %time%
java -cp lib/*;war/WEB-INF/lib/guava-21.0.jar;c:/gwt/*;src;war/WEB-INF/classes -Xmx1g com.google.gwt.dev.Compiler com.butent.bee.Bee