Subclipse is an Eclipse plugin that enables Suvbersion support in the Eclipse IDE 

An initial release has been made that supports most of the major Subversion features as well as Eclipse features such as refactoring. 

* Current Release

Download the current release of 0.7.0 which supports the following and more ...

Share 
Checkout 
Update 
Commit 
History 
Add 
Move/Delete (file and folders) 
Authentication 
Compare 

Linked against subversion 0.26.0 and db 4.0.14

* Installation

Download the subclipse-0.7.0.zip file 

Unzip the file into the Eclipse home directory. 

Restart Eclipse 

Open a new SVNExplorer Perspective and use it in the same way as the CVS Plugin 

* Source

We are using a subversion-repository. 

You can browse the latest source 

Otherwise you can check out the newest Subclipse source using svn: 

   svn co http://svn.collab.net/repos/subclipse/trunk/ subclipse
This will check out subclipse, svnClientAdapter and svnant 

* FAQ

Why is Subclipse so particular about what version of Subclipse and Berkley DB is used?

Subclipse uses a JNI interface (SVN-UP) to Subversion. The binaries that come with Subclipse are linked with particular versions when compiled. If you want to use a different version of Subversion and Berkley DB then you'll need to change the version of the Subversion libraries (libdb40.dll, libeay32.dll, ssleay32.dll, zlib.dll) and recompile the SVN-UP library (good luck with that). Basically don't bother and just upgrade your Subversion version. 

So if Subclipse is tied to a Subversion version, will it be updated when a new version of Subversion comes out? 

Short answer is yes, but it will depend on the SVN-UP guys releasing a new JNI library (which I'm sure they'de do quite quickly). 

Why does Subclipse only support Windows? 

Currently the only pre-compiled SVN-UP library is released for Windows as a dll. The library can compiled for linux, so when the binary is released in that form I'll add another release. 

Will the plugin work under WSAD 5? Not currently. Subclipse uses some methods that are only supported in Eclipse 2.1 such as isLinkedResource(). We'll look into supporting it soon. 

