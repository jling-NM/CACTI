#!/bin/bash

version=0.4.0
bundleversion=0.4.0
appname=CACTI

### jar ###
javapackager \
-createjar \
-appclass edu.unm.casaa.main.Main \
-srcdir /Users/josef/projects/cacti2/out/production/CACTI \
-outdir /Users/josef/projects/cacti2/out/bundles \
-outfile $appname.$version -v

### package icons ###
iconutil --convert icns --output /Users/josef/projects/cacti2/media/osx.icns /Users/josef/projects/cacti2/media/osx.iconset

### dmg ###
javapackager \
-deploy \
-native dmg \
-name $appname \
-srcdir /Users/josef/projects/cacti2/out/bundles \
-srcfiles $appname.$version.jar \
-outdir /Users/josef/projects/cacti2/out \
-appclass edu.unm.casaa.main.Main \
-name $appname -title $appname \
-outfile $appname.$version.dmg \
\
-BappVersion=$version \
-Bicon=/Users/josef/projects/cacti2/media/osx.icns \
-Bmac.category=Education -BjvmOptions=-Xmx128m \
-BjvmOptions=-Xms128m \
-Bruntime=/Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home \
-Bmac.CFBundleIdentifier=edu.unm.casaa.cacti \
-Bmac.CFBundleName=$appname \
-Bmac.CFBundleVersion=040


