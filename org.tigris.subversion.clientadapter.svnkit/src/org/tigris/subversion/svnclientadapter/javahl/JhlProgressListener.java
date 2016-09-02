package org.tigris.subversion.svnclientadapter.javahl;

import org.apache.subversion.javahl.ProgressEvent;
import org.apache.subversion.javahl.callback.ProgressCallback;
import org.tigris.subversion.svnclientadapter.ISVNProgressListener;
import org.tigris.subversion.svnclientadapter.SVNProgressEvent;

public class JhlProgressListener implements ProgressCallback {
	ISVNProgressListener worker;
	
	public JhlProgressListener() {
		super();
	}

	public void onProgress(ProgressEvent event) {
		if (worker != null )
			worker.onProgress(new SVNProgressEvent(event.getProgress(), event.getTotal()));
	}
	
	public void setWorker(ISVNProgressListener worker) {
		this.worker = worker;
	}

}
