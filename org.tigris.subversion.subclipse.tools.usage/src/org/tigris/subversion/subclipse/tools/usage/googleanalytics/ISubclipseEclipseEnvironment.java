package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

import org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse.IEclipseEnvironment;

public interface ISubclipseEclipseEnvironment extends IEclipseEnvironment {
	
	public String getSubclipseVersion();

	public boolean isLinuxDistro();
	
}
