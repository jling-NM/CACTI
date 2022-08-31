@ECHO OFF

set version=1.0.0
set bundleversion=1.0.0
set appname=CACTI

:: https://www.oracle.com/java/technologies/downloads/
:: https://openjdk.org/
set jdk=C:\Users\josef\projects\jdk-18.0.2.1
set jdk_mods=C:\Users\josef\projects\jdk-18.0.2.1\jmods

:: https://gluonhq.com/products/javafx/
set openjfx_mods=C:\Users\josef\projects\javafx-jmods-18.0.2
set openjfx_lib=C:\Users\josef\projects\javafx-sdk-18.0.2\lib

set project_base_dir=C:\Users\josef\projects\CACTI
cd %project_base_dir%

:: 
:: clean and compile
::
rmdir /S /Q %project_base_dir%\target
MKDIR %project_base_dir%\target\classes

:: copy resources directory
Xcopy /E /I %project_base_dir%\src\main\resources %project_base_dir%\target\resources

MKDIR %project_base_dir%\target\classes\edu\unm\casaa\main

:: extract classes directly into class path so they are just built into jar
cd %project_base_dir%\target\classes
%jdk%\bin\jar vxf %project_base_dir%\lib\sqlite-jdbc-3.25.2.jar
cd %project_base_dir%

echo ""
echo "+++++++++++++++++++++++++++"
echo "    Compile"
echo "+++++++++++++++++++++++++++"
%jdk%\bin\javac -verbose -Xlint:unchecked -d %project_base_dir%\target\classes -classpath %project_base_dir%\target\classes --module-path %jdk_mods% --add-modules java.xml,java.desktop,java.sql,java.prefs --module-path %openjfx_mods% --add-modules javafx.controls,javafx.fxml,javafx.media %project_base_dir%\src\main\java\edu\unm\casaa\misc\*.java %project_base_dir%\src\main\java\edu\unm\casaa\main\*.java %project_base_dir%\src\main\java\edu\unm\casaa\globals\*.java %project_base_dir%\src\main\java\edu\unm\casaa\utterance\*.java

cd %project_base_dir%\target

echo ""
echo "+++++++++++++++++++++++++++"
echo "    Create Jar"
echo "+++++++++++++++++++++++++++"
%jdk%\bin\jar --create --file cacti_%version%.jar --main-class=edu.unm.casaa.main.Main -C %project_base_dir%\target\classes . -C %project_base_dir%\target\resources .

echo ""
echo "+++++++++++++++++++++++++++"
echo "    Test Jar"
echo "+++++++++++++++++++++++++++"
%jdk%\bin\java -jar --module-path %jdk_mods% --add-modules java.xml,java.desktop,java.sql,java.prefs --module-path %openjfx_lib% --add-modules javafx.controls,javafx.fxml,javafx.media %project_base_dir%\target\cacti_%version%.jar
::%jdk%\bin\java -jar %project_base_dir%\target\windows\cacti_%version%.jar

echo ""
echo "+++++++++++++++++++++++++++"
echo "    Build Runtime Image    "
echo "+++++++++++++++++++++++++++"
%jdk%\bin\jlink --module-path %jdk_mods% --add-modules java.xml,java.desktop,java.sql,java.prefs --module-path %openjfx_mods% --add-modules javafx.controls,javafx.fxml,javafx.media --compress=2 --no-header-files --no-man-pages --output %project_base_dir%\target\cacti_jre

