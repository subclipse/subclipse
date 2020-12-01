#!/bin/bash
#Sample Usage: publishBuild.sh owner repo package version pathToP2Repo

API=https://api.bintray.com
BINTRAY_OWNER=$1
BINTRAY_REPO=$2
PCK_NAME=$3
PCK_VERSION=$4
PATH_TO_REPOSITORY=$5

function main() {
    echo "Owner:   ${BINTRAY_OWNER}"
    echo "Repos:   ${BINTRAY_REPO}"
    echo "Package: ${PCK_NAME}"
    echo "Version: ${PCK_VERSION}"
    echo "Path:    ${PATH_TO_REPOSITORY}"
    deploy_updatesite
}

function deploy_updatesite() {

if [ ! -z "$PATH_TO_REPOSITORY" ]; then
   cd $PATH_TO_REPOSITORY
fi


FILES=./*
PLUGINDIR=./plugins/*
FEATUREDIR=./features/*

for f in $FILES;
do
    if [ ! -d $f ]; then
      echo "Processing $f file..."
      curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_TOKEN} "${API}/content/${BINTRAY_OWNER}/${BINTRAY_REPO}/${PCK_NAME}/${PCK_VERSION}/$f;bt_package=${PCK_NAME};bt_version=${PCK_VERSION};publish=1;override=1"
      echo ""
    fi
done

echo "Processing features dir $FEATUREDIR file..."
for f in $FEATUREDIR;
do
  echo "Processing feature: $f file..."
  curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_TOKEN} "${API}/content/${BINTRAY_OWNER}/${BINTRAY_REPO}/${PCK_NAME}/${PCK_VERSION}/$f;bt_package=${PCK_NAME};bt_version=${PCK_VERSION};publish=1;override=1"
  echo ""
done

echo "Processing plugin dir $PLUGINDIR file..."

for f in $PLUGINDIR;
do
   # take action on each file. $f store current file name
  echo "Processing plugin: $f file..."
  curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_TOKEN} "${API}/content/${BINTRAY_OWNER}/${BINTRAY_REPO}/${PCK_NAME}/${PCK_VERSION}/$f;bt_package=${PCK_NAME};bt_version=${PCK_VERSION};publish=1;override=1"
  echo ""
done

}

main "$@"