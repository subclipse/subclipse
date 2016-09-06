#!/usr/bin/env bash
# Create the .m2 folder if it does not exist
[ -d $HOME/.m2 ] || mkdir $HOME/.m2

# Copy the settings.xml file to .m2
[ -f $HOME/.m2/settings.xml ] || rm $HOME/.m2/settings.xml
cp ./travis_settings.xml $HOME/.m2/settings.xml

