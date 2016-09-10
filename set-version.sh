#!/usr/bin/env bash

if [ $# -ne 2 ]; 
    then
        echo "Usage: ./set-version.sh OLD NEW"
        echo "Example: ./set-version.sh 1.0.0 1.0.1"
    else
        SNAPSHOT="s/$1-SNAPSHOT/$2-SNAPSHOT/g"
        QUALIFIER="s/$1.qualifier/$2.qualifier/g"

        find . -type f -name "*.xml" -exec sed -i '' -e $SNAPSHOT {} \;
        find . -type f -name "*.xml" -exec sed -i '' -e $QUALIFIER {} \;
        find . -type f -name "*.MF" -exec sed -i '' -e $QUALIFIER {} \;

        echo "Versions updated.  Use git status and git diff to confirm"
        echo "Be sure to test build using mvn clean package before commit"
        echo "Update CHANGELOG.md for next dev cycle"
    fi

