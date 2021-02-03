#!/usr/bin/env bash

OWNER="subclipse"
REPO="releases"
NAME="subclipse"
PATH_NAME="./releng/update-site/target/repository"
LATEST="latest"

#  Extract tag name from environment
#  The tag will look like this:
#  GITHUB_REF=refs/tags/4.4.2

a=( ${GITHUB_REF//// } )

# Only continue if there are 3 parts and the middle
# part is tags
if [ ${#a[@]} -eq 3 ] && [ ${a[1]} = "tags" ];
 then
     # Tag name will be the last part
     BRANCH_NAME=${a[2]}
     b=( ${BRANCH_NAME//./ } )
     # Verify this looks like x.y.z
     if [ ${#b[@]} -eq 3 ]; 
      then
          echo "We have a valid tag: ${BRANCH_NAME}"
          RELEASE_VERSION="${b[0]}.${b[1]}.x"     # compose new major.minor.x
  
          echo "Publishing release ..." 
          # Upload the release contents
          ./publishBuild.sh $1 $2 "${OWNER}" "${REPO}" "${NAME}" "${BRANCH_NAME}" "${PATH_NAME}"
          echo "" 

          echo "Preparing XML files for ${RELEASE_VERSION} ..."
          export BUILD_TIME=`date +%s`
          # Prepare the composite XML files
          cat ./releng/release/release-compositeArtifacts.xml | sed s/\$\{BRANCH_NAME\}/${BRANCH_NAME}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./compositeArtifacts.xml
          cat ./releng/release/release-compositeContent.xml | sed s/\$\{BRANCH_NAME\}/${BRANCH_NAME}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./compositeContent.xml
          echo "" 

          # Upload the composite XML files
          echo "Publishing composite site for ${RELEASE_VERSION} ..." 
          ./publishComposite.sh "${OWNER}" "${REPO}" "${NAME}" "${RELEASE_VERSION}"
          echo "" 

          echo "Preparing XML files for latest ..."
          export BUILD_TIME=`date +%s`
          # Prepare the composite XML files
          cat ./releng/release/latest-compositeArtifacts.xml | sed s/\$\{BRANCH_NAME\}/${BRANCH_NAME}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./compositeArtifacts.xml
          cat ./releng/release/latest-compositeContent.xml | sed s/\$\{BRANCH_NAME\}/${BRANCH_NAME}/g | sed s/\$\{BUILD_TIME\}/${BUILD_TIME}/g  > ./compositeContent.xml
          echo "" 

          # Upload the composite XML files
          echo "Publishing composite site for latest ..." 
          ./publishComposite.sh "${OWNER}" "${REPO}" "${NAME}" "${LATEST}"
          echo "" 

      else
        echo "Not a release tag, nothing to do ..." 
     fi
 else
   echo "Not a release tag, nothing to do ..." 
fi
