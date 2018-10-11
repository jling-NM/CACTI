#!/bin/bash

version=0.90.0
bundleversion=0.90.0
appname=CACTI

### build the jar in IDE to get classes ###
### jar ###
#/Library/Java/JavaVirtualMachines/jdk1.8/Contents/Home/bin/javapackager \
#-createjar \
#-appclass edu.unm.casaa.main.Main \
#-srcdir /Users/josef/projects/CACTI/out/production/CACTI \
#-outdir /Users/josef/projects/CACTI/out/bundles \
#-outfile $appname.$version -v

### package icons ###
iconutil \
--convert icns \
--output /Users/josef/projects/CACTI/media/osx.icns /Users/josef/projects/CACTI/media/osx.iconset

### dmg ###
/Library/Java/JavaVirtualMachines/jdk1.8/Contents/Home/bin/javapackager \
-deploy \
-native dmg \
-name $appname \
-srcdir /Users/josef/projects/CACTI/out/artifacts/CACTI_jar \
-srcfiles $appname.jar \
-outdir /Users/josef/projects/CACTI/out \
-appclass edu.unm.casaa.main.Main \
-name $appname -title $appname \
-outfile $appname.$version.dmg \
\
-BappVersion=$version \
-Bicon=/Users/josef/projects/CACTI/media/osx.icns \
-Bmac.category=Education -BjvmOptions=-Xmx128m \
-BjvmOptions=-Xms128m \
-Bruntime=/Library/Java/JavaVirtualMachines/jdk1.8/Contents/Home \
-Bmac.CFBundleIdentifier=edu.unm.casaa.cacti \
-Bmac.CFBundleName=$appname \
-Bmac.CFBundleVersion=090


