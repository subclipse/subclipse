# Subclipse  [![Build Status](https://travis-ci.org/subclipse/subclipse.svg?branch=master)](https://travis-ci.org/subclipse/subclipse)

Subclipse is an Eclipse Team Provider plugin for Apache Subversion. 

See the wiki for information on Requirements, Installation, Development etc.
https://github.com/subclipse/subclipse/wiki

# Need Help?

Report bugs or feature requests in the [Issue Tracker](https://github.com/subclipse/subclipse/issues)

For Q&A we recommend [StackOverflow](http://stackoverflow.com/questions/tagged/subclipse)

# Builds

Subclipse uses Maven/Tycho for builds.  Doing a full build using Maven can be done from the root of the repository:

    mvnw clean package

This will result in a p2 repository at `releng/update-site/target/repository`

Every commit to master initiates a CI process as Travis-CI which posts the new p2 repository to Bintray.  Installation of
these snapshot builds can be done via this update site URL:  https://dl.bintray.com/subclipse/snapshots/

You can skip the CI build by including [skip ci] in your commit message.  Useful when updating this README or other files
that do not need to trigger a new build.

Currently, we are only using SNAPSHOT versioning on the main core and ui plugins.  If the less frequently changed plugins are modified be sure to update the version information in the pom.xml and manifest as needed.

# License
The source code for this project is licensed under Eclipse 1.0 license.
