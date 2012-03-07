package org.tigris.subversion.subclipse.core.client;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class OperationResourceCollector implements ISVNNotifyListener {
	private Map<String, SVNRevision> revisionMap;
	private Set<IResource> operationResources = new LinkedHashSet<IResource>();
	private boolean revisionUpdated = false;
	
	public void setRevisionMap(Map<String, SVNRevision> revisionMap) {
		this.revisionMap = revisionMap;
	}

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
	
	public void logRevision(long revision, String path) {
		if (!revisionUpdated && revisionMap != null) {
			SVNRevision svnRevision = revisionMap.get(path);
			if (svnRevision != null && svnRevision instanceof SVNRevision.Number) {
				if (((SVNRevision.Number)svnRevision).getNumber() != revision) {
					revisionUpdated = true;
				}
			}
		}
	}
	
	public void logCompleted(String message) {}

	public boolean isRevisionUpdated() {
		return revisionUpdated;
	}
	
}
