#!/bin/bash

###
### Avoid Maven plugins for java 1.8 jfx jar output
###

version=0.9.1
bundleversion=0.9.1
appname=CACTI

cd ..
project_base_dir=`pwd`

# clean and compile with maven
mvn clean compile

# copy jfx files into class path
cp $project_base_dir/src/main/java/edu/unm/casaa/main/*.fxml $project_base_dir/target/classes/edu/unm/casaa/main/
cp $project_base_dir/src/main/java/edu/unm/casaa/main/*.css $project_base_dir/target/classes/edu/unm/casaa/main/

# extract classes directly into class path so they are just built into jar
cd $project_base_dir/target/classes
jar xf $project_base_dir/lib/sqlite-jdbc-3.25.2.jar

# create the fx jar file
javapackager \
    -createjar \
    -appclass edu.unm.casaa.main.Main \
    -srcdir $project_base_dir/target/classes \
    -outdir $project_base_dir/target/out \
    -outfile ${appname}_${version} \
    -v

# create debian installer
javapackager \
    -deploy \
    -native deb \
    -name $appname -title $appname \
    -vendor CASAA \
    -appclass edu.unm.casaa.main.Main \
    -srcfiles $project_base_dir/target/out/${appname}_${version}.jar \
    -outdir $project_base_dir/target/out \
    -outfile ${appname}_${version}.deb \
    \
    -BappVersion=$version \
    -BlicenseType=MIT \
    -Bicon=$project_base_dir/target/classes/media/windows.iconset/icon_32x32.png \
    -Bmac.category=Education -BjvmOptions=-Xmx128m \
    -BjvmOptions=-Xms128m \
    -Bruntime=${JAVA_HOME}/jre
    
