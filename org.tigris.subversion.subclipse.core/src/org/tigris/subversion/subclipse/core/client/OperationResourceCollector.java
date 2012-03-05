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
	private Set<IResource> operationResources = new LinkedHashSet<IResource>();
	private boolean revisionUpdated = false;
	private Map<String, SVNRevision> revisionMap = new HashMap<String, SVNRevision>();
	
	public void onNotify(File path, SVNNodeKind kind) {
		IPath pathEclipse = new Path(path.getAbsolutePath());
		IResource[] resources = SVNWorkspaceRoot.getResourcesFor(pathEclipse, false);
		for (IResource resource : resources) {
			operationResources.add(resource);
			
			if (!revisionUpdated) {
				ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				if (svnResource != null) {
					try {
						SVNRevision svnRevision = svnResource.getRevision();
						if (svnRevision == null) {
							revisionUpdated = true;
						}
						else {
							SVNRevision previousRevision = revisionMap.get(path.getAbsolutePath());
							if (previousRevision != null && !previousRevision.equals(svnRevision)) {
								revisionUpdated = true;
							}
							revisionMap.put(path.getAbsolutePath(), svnRevision);
						}
					} catch (SVNException e) {
						revisionUpdated = true;
					}
				}
				else {
					revisionUpdated = true;
				}
			}
			
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

	public boolean isRevisionUpdated() {
		return revisionUpdated;
	}
	
}
