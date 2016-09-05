package org.tigris.subversion.svnclientadapter;

public interface ISVNDirEntryWithLock {
	
	public ISVNDirEntry getDirEntry();
	
	public ISVNLock getLock();

}
