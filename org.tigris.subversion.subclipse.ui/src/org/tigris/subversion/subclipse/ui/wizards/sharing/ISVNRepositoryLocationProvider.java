package org.tigris.subversion.subclipse.ui.wizards.sharing;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

public interface ISVNRepositoryLocationProvider {

	public ISVNRepositoryLocation getLocation() throws TeamException;
	
	public IProject getProject();
	
}
