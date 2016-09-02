package org.tigris.subversion.clientadapter;

import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public interface ISVNClientWrapper {
	
	public ISVNClientAdapter getAdapter();

	public boolean isAvailable();

	public String getVersionString();

	public void setDisplayName(String string);
	
	public String getDisplayName();
	
	public String getLoadErrors();

	public String getAdapterID();

}
