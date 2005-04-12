Subclipse is an Eclipse plugin that enables Suvbersion support in
the Eclipse IDE

An initial release has been made that supports most of the major
Subversion features as well as Eclipse features such as refactoring.

* Current Release

Download the current release of which supports the following and
more ...

Share 
Checkout 
Update 
Commit 
History 
Add 
Move/Delete (file and folders) 
Authentication 
Compare 

Linked against Subversion 1.05

* Installation


Add the update site (http://subclipse.tigris.org/update) to
your update site configuration (Help->Software Updates->Find and
install) and check for updates. Select Subclipse, and let it install.
Restarting Eclipse is recommended.

Open a new SVNExplorer Perspective and use it in the same way as
the CVS Plugin

* Source

We are using a subversion-repository. 

You can browse the latest source  at http://svn.collab.net/repos/subclipse

Otherwise you can check out the newest Subclipse source using svn: 

   svn co http://svn.collab.net/repos/subclipse/trunk/ subclipse

This will check out subclipse, svnClientAdapter and svnant 

* FAQ

Why is Subclipse so particular about what version of Subclipse and
Berkley DB is used?

Subclipse uses a JNI interface (SVN-UP) to Subversion. The binaries
that come with Subclipse are linked with particular versions when
compiled. If you want to use a different version of Subversion and
Berkley DB then you'll need to change the version of the Subversion
libraries (libdb40.dll, libeay32.dll, ssleay32.dll, zlib.dll) and
recompile the SVN-UP library (good luck with that). Basically don't
bother and just upgrade your Subversion version.

So if Subclipse is tied to a Subversion version, will it be updated
when a new version of Subversion comes out?

Short answer is yes, but it will depend on the SVN-UP guys releasing
a new JNI library (which I'm sure they'de do quite quickly).

Why does Subclipse only support Windows?

It doesn't. Current supported platforms are Windows, OSX and Linux.

Will the plugin work under WSAD 5? 

Not currently. Subclipse uses some methods that are only supported
in Eclipse 2.1 such as isLinkedResource(). We'll look into supporting
it soon.

