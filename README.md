# Subclipse  [![Build Status](https://github.com/subclipse/subclipse/workflows/Subclipse%20Build/badge.svg)](https://github.com/subclipse/subclipse/actions)

Subclipse is an Eclipse Team Provider plugin for Apache Subversion. 

Information on Requirements, Installation, Development etc. is available in the [wiki](https://github.com/subclipse/subclipse/wiki)

**NOTE:** Unfortunately this project is only minimally maintained and is reliant solely on contributions from others via PR. We can help make new releases if enough interesting contributions show up but there is otherwise no one who actively works on this code any longer.

We recommend installing the [Subclipse snapshot release](https://github.com/subclipse/subclipse/wiki#snapshot-builds) so that when there are contributions that are merged you receive them and are not dependent on a new official release being published.

## SVNKit

Subclipse has never maintained or been directly involved with [SVNKit](https://svnkit.com), which is provided by TMate Software.

Subclipse is written to Subversion's JavaHL API interface. SVNKit works with Subclipse because it provides an implementation of that interface. We host an Eclipse Marketplace-compatible update site with the SVNKit library out of convenience because SVNKit has never published an update site that works with the Eclipse Marketplace installer. (For those interested in these details, they are providing the old Eclipse 3.0-style update site and not the replacement P2-style site that came with Eclipse 3.2+). The work to republish their site and keep the version up to date and test is more work than we can continue to provide. **The version of the SVNKit library on the site we host is not the latest**.

Newer versions of the SVNKit library are available for download at: https://svnkit.com/download.php. The zipped Eclipse archive is the best way to get the latest version of their library and can be easily installed via the Eclipse install interface by adding the zipped archive or unzipped local update site.

When it is possible, we have always recommended using the native Subversion JavaHL implementation but we recognize this is not always easy to do and using SVNKit is often the simpler path to take.

# License
The source code for this project is licensed under Eclipse 1.0 license.
