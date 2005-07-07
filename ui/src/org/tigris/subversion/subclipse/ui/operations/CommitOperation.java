package org.tigris.subversion.subclipse.ui.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;

public class CommitOperation extends SVNOperation {
    private IResource[] selectedResources;
    private IResource[] resourcesToAdd;
    private IResource[] resourcesToCommit;
    private String commitComment;
    private boolean keepLocks;

    public CommitOperation(IWorkbenchPart part, IResource[] selectedResources, IResource[] resourcesToAdd, IResource[] resourcesToCommit, String commitComment, boolean keepLocks) {
        super(part);
        this.selectedResources = selectedResources;
        this.resourcesToAdd = resourcesToAdd;
        this.resourcesToCommit = resourcesToCommit;
        this.commitComment = commitComment;
        this.keepLocks = keepLocks;
    }

    protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
        try {
			if (resourcesToAdd.length > 0) {
			    Map table = getProviderMapping(resourcesToAdd);
				if (table.get(null) != null) {
					throw new SVNException(Policy.bind("RepositoryManager.addErrorNotAssociated"));  //$NON-NLS-1$
				}
				Set keySet = table.keySet();
				Iterator iterator = keySet.iterator();
				while (iterator.hasNext()) {
					SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
					List list = (List)table.get(provider);
					IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
					provider.add(providerResources, IResource.DEPTH_ZERO, null);
				}						
			}
			Map table = getProviderMapping(resourcesToCommit);
			Set keySet = table.keySet();
			Iterator iterator = keySet.iterator();
			while (iterator.hasNext()) {
				IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
				SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
				List list = (List)table.get(provider);
				IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
				provider.checkin(providerResources, commitComment, keepLocks, getDepth(providerResources), null);
			}			
//			for (int i = 0; i < selectedResources.length; i++) {
//				IResource projectHandle = selectedResources[i].getProject();
//				projectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);
//			}		
        } catch (TeamException e) {
			throw SVNException.wrapException(e);
//		} catch (CoreException e) {
//			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
		}
    }

    /**
	 * This method figures out of if we should commit with DEPTH_ZERO or DEPTH_INFINITE
	 * If there are any modified folders (which could only be a prop change) in the list of committed items,
	 * then it should return DEPTH_ZERO, otherwise it should return DEPTH_INFINITE.
	 */
	private int getDepth(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			if (resources[i].getType() != IResource.FILE) {
				ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
				try {
					if (svnResource.getStatus().isPropModified())
						return IResource.DEPTH_ZERO;
				} catch (SVNException e) {
				}
			}
		}
		return IResource.DEPTH_INFINITE;
	}

	protected String getTaskName() {
        return Policy.bind("CommitOperation.taskName"); //$NON-NLS-1$;
    }
    
	private Map getProviderMapping(IResource[] resources) {
		Map result = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject(), SVNProviderPlugin.getTypeId());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}

}
