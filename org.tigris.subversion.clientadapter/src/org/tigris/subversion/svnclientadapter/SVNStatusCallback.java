package org.tigris.subversion.svnclientadapter;

import java.util.ArrayList;
import java.util.List;

public class SVNStatusCallback implements ISVNStatusCallback {
	private List statuses = new ArrayList();

	public void doStatus(String path, ISVNStatus status) {
		if (status != null) {
			statuses.add(status);
		}
	}
	
	public ISVNStatus[] getStatuses() {
		ISVNStatus[] statusArray = new ISVNStatus[statuses.size()];
		statuses.toArray(statusArray);
		return statusArray;
	}

}
