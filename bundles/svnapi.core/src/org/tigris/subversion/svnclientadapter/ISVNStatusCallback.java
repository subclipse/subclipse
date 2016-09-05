package org.tigris.subversion.svnclientadapter;

public interface ISVNStatusCallback {

	public void doStatus(String path, ISVNStatus status);

}
