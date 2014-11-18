REM Cleaning gwt unicache
REM Before start run script
REM Menu Run >External tools > External tools configurations
REM Select Program of left field and press New
REM Enter Name: clean gwt unicache
REM In Main tab: 
REM Enter Location: ${workspace_loc:/Bee/samples/cli/clean_uniCache.bat}
REM Enter working directory: ${workspace_loc:/Bee/gwt-unitCache}
REM Enter arguments: ${workspace_loc:/Bee/gwt-unitCache}
REM Press Apply button
REM You can run this script in External tools toolbar.


del %1 /q