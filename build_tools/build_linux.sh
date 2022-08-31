#!/bin/bash

###
### Avoid Maven plugins
###

version=1.0.0
bundleversion=1.0.0
appname=CACTI

export openjdk=~/apps/jdk-18.0.2
export openjfx_lib=~/apps/javafx-sdk-18.0.2/lib
export openjfx_mods=~/apps/javafx-jmods-18.0.2
cd ..
project_base_dir=`pwd`



# 
# clean and compile
#
rm -fr $project_base_dir/target
mkdir -p $project_base_dir/target/classes


# extract classes directly into class path so they are just built into jar
cd $project_base_dir/target/classes
jar xf $project_base_dir/lib/sqlite-jdbc-3.25.2.jar

echo "+++++++++++++++++++++++++++"
echo "    Compile"
echo "+++++++++++++++++++++++++++"
javac \
-verbose \
-Xlint:unchecked \
--class-path $project_base_dir/target/classes \
-d $project_base_dir/target/classes \
--module-path ${openjdk}/jmods --add-modules java.xml,java.desktop,java.sql,java.prefs \
--module-path ${openjfx_mods} --add-modules javafx.controls,javafx.fxml,javafx.media \
$project_base_dir/src/main/java/edu/unm/casaa/*/*.java
    
    

# copy resources directory
rsync -av $project_base_dir/src/main/resources $project_base_dir/target/

cd $project_base_dir/target

echo "+++++++++++++++++++++++++++"
echo "    Create Jar"
echo "+++++++++++++++++++++++++++"
jar \
--create \
--file cacti_${version}.jar \
--main-class=edu.unm.casaa.main.Main \
-C $project_base_dir/target/classes . \
-C $project_base_dir/target/resources . \

# test
echo "Test Jar"
java -jar --module-path ${openjfx_lib} --add-modules javafx.controls,javafx.fxml,javafx.media $project_base_dir/target/cacti_${version}.jar




# # link and package (doesn't work in ubuntu 18.04)
# echo "+++++++++++++++++++++++++++"
# echo "    Package for Linux"
# echo "+++++++++++++++++++++++++++"
# jpackage \
# --name CACTI \
# --install-dir /opt \
# --type deb \
# --app-version ${bundleversion} \
# --copyright CASAA \
# --vendor CASAA \
# -i $project_base_dir/target \
# --dest $project_base_dir/target \
# --main-class edu.unm.casaa.main.Main \
# --main-jar $project_base_dir/target/cacti_${version}.jar \
# --module-path ${openjfx} --add-modules javafx.controls,javafx.fxml,javafx.media \
# --jlink-options "--strip-native-commands --strip-debug --no-man-pages --no-header-files" \
# --icon $project_base_dir/target/resources/edu/unm/casaa/main/media/windows.iconset/icon_32x32.png \
# --linux-deb-maintainer jling@swcp.com \
# --linux-menu-group Education \
# --linux-app-category Science \
# --linux-rpm-license-type MIT \
# --linux-shortcut
# 
#     

echo "+++++++++++++++++++++++++++"
echo "    Clean up"
echo "+++++++++++++++++++++++++++"
rm -fr $project_base_dir/target/classes
rm -fr $project_base_dir/target/resources

