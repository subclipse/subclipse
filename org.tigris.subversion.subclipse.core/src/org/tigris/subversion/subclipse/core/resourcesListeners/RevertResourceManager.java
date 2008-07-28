package org.tigris.subversion.subclipse.core.resourcesListeners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
/**
 * RevertResourceManager reverts not yet commited deletes when a file is added which was scheduled for deletion. It also reverts folder deletes when
 * new resources are added to folders that are scheduled for deletion. 
 * @author Hugo Visser joegi at scene.nl
 */
public class RevertResourceManager implements IResourceChangeListener {

	private class RevertWorkspaceJob extends WorkspaceJob {
		
		private final ISVNLocalResource[] resources;

		public RevertWorkspaceJob(ISVNLocalResource[] resources) {
			super(Policy.bind("RevertResourceManager.jobName"));
			this.resources = resources;
		}

		private void revertResources(ISVNLocalResource[] resources, IProgressMonitor monitor) throws SVNException {
			monitor.beginTask(Policy.bind("RevertResourceManager.reverting"), resources.length);
			for (int i=0; i < resources.length; i++) {
				if (monitor.isCanceled()) {
					break;
				}
				if (resources[i] instanceof ISVNLocalFile) {
					ISVNLocalFile res = (ISVNLocalFile)resources[i];
					File file = res.getFile().getAbsoluteFile();
	    	        // TODO a better location?
	    	        File tmp = new File(res.getFile().getAbsolutePath() + ".svntmp");
	    	        if (tmp.exists()) {
	    	        	tmp.delete();
	    	        }
	    	        if (file.renameTo(tmp)) {
	    	            // res.revert is recursive and updates local history
	    	            revert(res);
	    	            if (!file.delete()) {
	    	                //TODO handle this in a better way?
	    	                throw SVNException.wrapException(new IllegalStateException("Could not remove "+file));                                                            	
	    	            }
	    	            if (!tmp.renameTo(file)) {
	    	                //TODO handle this in a better way?
	    	                throw SVNException.wrapException(new IllegalStateException("Could not rename "+tmp+" to "+file));                                
	    	            }
	    	        } else {
	    	            //TODO handle this in a better way?
	    	            throw SVNException.wrapException(new IllegalStateException("Could not rename "+file+" to "+tmp));
	    	        }					
				}
				else {
					revert(resources[i]);
				}
				monitor.worked(1);
			}
			monitor.done();
		}

		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			revertResources(resources, monitor);
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

	}

	public void resourceChanged(final IResourceChangeEvent event) {
        final List addedFileResources = new ArrayList();

        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {

                public boolean visit(IResourceDelta delta) throws CoreException {
                    if (delta.getResource().getType() == IResource.FILE && delta.getKind() == IResourceDelta.ADDED) {
                        addedFileResources.add(delta.getResource());
                    }
                    return true;
                }

            });
            if (!addedFileResources.isEmpty()) {
                final IResource[] resources = (IResource[]) addedFileResources.toArray(new IResource[addedFileResources
                        .size()]);
                ISVNLocalResource[] revertResources = processResources(resources);
                if (revertResources.length > 0) {
                	new RevertWorkspaceJob(revertResources).schedule(500);
                }
            }
        } catch (CoreException e) {
            SVNProviderPlugin.log(e.getStatus());
        }

    }

    /**
     * Revert previously deleted resources that are added again. When a file is
     * reverted, it's parent directories are also reverted.
     * When new files are added in folders that are scheduled for deletion, the parent
     * folder tree is reverted.
     * 
     * @param resources
     */
    private ISVNLocalResource[] processResources(IResource[] resources) throws CoreException {
    	List revertedResources = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            if (resource.getType() == IResource.FILE) {
                ISVNLocalFile res = SVNWorkspaceRoot.getSVNFileFor((IFile) resource);
                if (res.getFile().exists()) {
                    boolean deleted = res.getStatus().isDeleted();
                    if (deleted) {
                        revertedResources.add(res);
                    }
                    ISVNLocalFolder parentFolder = res.getParent();
                    while (parentFolder != null) {
                        if (parentFolder.getStatus().isDeleted() && !revertedResources.contains(parentFolder)) {
                            revertedResources.add(parentFolder);
                        } else {
                            break;
                        }
                        if (parentFolder.getParent() == null) {
                            break;
                        }
                        parentFolder = parentFolder.getParent();
                    }                    
                }
            }
        }
        return (ISVNLocalResource[]) revertedResources.toArray(new ISVNLocalResource[revertedResources.size()]);
    }
    
    
    /**
     * Like revert on ISVNLocalFile but without updating the local history. Non
     * recursive revert for folders
     * 
     * @param resource
     * @throws SVNException
     */
    private void revert(ISVNLocalResource resource) throws SVNException {
        try {
            ISVNClientAdapter svnClient = resource.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.revert(resource.getFile(), false);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
            OperationManager.getInstance().endOperation();
        }
    }

}
