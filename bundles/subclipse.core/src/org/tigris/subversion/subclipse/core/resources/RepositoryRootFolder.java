package org.tigris.subversion.subclipse.core.resources;

import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class RepositoryRootFolder extends RemoteFolder {

	public RepositoryRootFolder(ISVNRepositoryLocation repository, SVNUrl url, SVNRevision revision) {
		super(repository, url, revision);
	}

	public String getName() {
		return "Root";
	}

}
