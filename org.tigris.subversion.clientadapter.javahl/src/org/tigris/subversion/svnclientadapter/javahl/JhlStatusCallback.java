package org.tigris.subversion.svnclientadapter.javahl;

import java.util.ArrayList;
import java.util.List;

import org.apache.subversion.javahl.callback.StatusCallback;
import org.apache.subversion.javahl.types.Status;
import org.tigris.subversion.svnclientadapter.ISVNStatusCallback;

public class JhlStatusCallback implements StatusCallback {
	private List<Status> statusList = new ArrayList<Status>();
	private ISVNStatusCallback worker;
	
	public JhlStatusCallback(ISVNStatusCallback callback) {
		super();
		worker = callback;
	}

	public void doStatus(String path, Status status) {
		worker.doStatus(path, new JhlStatus(status, null));
		if (status != null) {
			statusList.add(status);
		}
	}

	public List<Status> getStatusList() {
		return statusList;
	}

}
