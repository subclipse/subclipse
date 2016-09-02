package org.tigris.subversion.svnclientadapter.javahl;

import java.util.Map;
import java.util.Set;

import org.apache.subversion.javahl.types.ChangePath;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.callback.LogMessageCallback;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageCallback;

public class JhlLogMessageCallback implements LogMessageCallback {
	
	private ISVNLogMessageCallback worker = null;

	public JhlLogMessageCallback(ISVNLogMessageCallback callback) {
		super();
		worker = callback;
	}
	
	public JhlLogMessageCallback() {
		super();
	}


	public void singleMessage(Set<ChangePath> changedPaths, long revision,
		Map<String, byte[]> revprops, boolean hasChildren) {

		if (revision == Revision.SVN_INVALID_REVNUM) {
			worker.singleMessage(null);
		} else {
			worker.singleMessage(new JhlLogMessage(changedPaths, revision, revprops, hasChildren));
		}
	}
	
}
