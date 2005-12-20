package org.tigris.subversion.subclipse.ui.subscriber;

import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

public class AddSynchronizeAction extends SynchronizeModelAction {

	public AddSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}
	
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			public boolean select(SyncInfo info) {
			    IStructuredSelection selection = getStructuredSelection();
			    Iterator iter = selection.iterator();
			    while (iter.hasNext()) {
			    	ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
			    	IResource resource = element.getResource();
			    	if (resource.isLinked()) return false;
	                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);			    
	                try {
	                	if (svnResource.isManaged()) return false;
	                } catch (SVNException e) {
	                    return false;
	                }
			    }
                return true;
			}
		};
	}    

	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new AddSynchronizeOperation(configuration, elements);
	}

}
