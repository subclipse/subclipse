package org.tigris.subversion.svnclientadapter;

public interface ISVNProgressListener {

	public void onProgress(SVNProgressEvent progressEvent);
	
}
