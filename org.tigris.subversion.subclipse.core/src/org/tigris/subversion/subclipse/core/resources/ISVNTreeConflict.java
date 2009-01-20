package org.tigris.subversion.subclipse.core.resources;

import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public interface ISVNTreeConflict {
	
	public ISVNStatus getStatus();
	
	public SVNConflictDescriptor getConflictDescriptor();
	
	public String getDescription();

}
