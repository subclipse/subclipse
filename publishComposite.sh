#!/bin/bash
#Sample Usage: publishComposite.sh owner repo package version

API=https://api.bintray.com
BINTRAY_OWNER=$1
BINTRAY_REPO=$2
PCK_NAME=$3
PCK_VERSION=$4

function main() {
    deploy_updatesite
}

function deploy_updatesite() {
echo "Owner:   ${BINTRAY_OWNER}"
echo "Repos:   ${BINTRAY_REPO}"
echo "Package: ${PCK_NAME}"
echo "Version: ${PCK_VERSION}"

FILES=( "compositeContent.xml" "compositeArtifacts.xml" )
for f in "${FILES[@]}"
do
  if [ ! -d $f ]; then
      echo "Processing $f file..."
      curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_TOKEN} "${API}/content/${BINTRAY_OWNER}/${BINTRAY_REPO}/${PCK_NAME}/${PCK_VERSION}/$f;bt_package=${PCK_NAME};bt_version=${PCK_VERSION};publish=1;override=1"
      echo ""
      echo "Cleaning up $f file..."
      rm $f
      echo ""
  fi
done

}

main "$@"