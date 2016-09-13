package org.tigris.subversion.subclipse.core.client;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

public class OperationResourceCollector implements ISVNNotifyListener {
	private Set<IResource> operationResources = new LinkedHashSet<IResource>();
	
	public void onNotify(File path, SVNNodeKind kind) {
		IPath pathEclipse = new Path(path.getAbsolutePath());
		IResource[] resources = SVNWorkspaceRoot.getResourcesFor(pathEclipse, false);
		for (IResource resource : resources) {
			operationResources.add(resource);
		}
	}
	
	public Set<IResource> getOperationResources() {
		return operationResources;
	}

	public void setCommand(int command) {}
	public void logCommandLine(String commandLine) {}
	public void logMessage(String message) {}
	public void logError(String message) {}
	public void logRevision(long revision, String path) {}
	public void logCompleted(String message) {}
}