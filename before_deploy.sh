#!/usr/bin/env bash

if [[ $TRAVIS_TAG ]]
 then
  export BUILD_TIME=`date +%s`
  a=( ${TRAVIS_TAG//./ } )        # split into array
  export RELEASE_VERSION="${a[0]}.${a[1]}.x"     # compose new major.minor.x
  cat ./releng/release/tag.json | sed s/\$\{TRAVIS_TAG\}/${TRAVIS_TAG}/g > ./tag.json
  cat ./releng/release/release.json | sed s/\$\{RELEASE_VERSION\}/${RELEASE_VERSION}/g > ./release.json
  cat ./releng/release/latest.json  > ./latest.json
  cat ./releng/release/release-compositeArtifacts.xml | sed s/\$\{TRAVIS_TAG\}/${TRAVIS_TAG}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./release-compositeArtifacts.xml
  cat ./releng/release/release-compositeContent.xml | sed s/\$\{TRAVIS_TAG\}/${TRAVIS_TAG}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./release-compositeContent.xml
  cat ./releng/release/latest-compositeArtifacts.xml | sed s/\$\{TRAVIS_TAG\}/${TRAVIS_TAG}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./latest-compositeArtifacts.xml
  cat ./releng/release/latest-compositeContent.xml | sed s/\$\{TRAVIS_TAG\}/${TRAVIS_TAG}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./latest-compositeContent.xml
else
  echo "Not a release tag, nothing to do ..." 
fi
