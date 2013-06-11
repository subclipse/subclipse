package org.tigris.subversion.subclipse.core.resourcesListeners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.SVNMoveDeleteHook;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.JobUtility;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
/**
 * RevertResourceManager reverts not yet commited deletes when a file is added which was scheduled for deletion. It also reverts folder deletes when
 * new resources are added to folders that are scheduled for deletion. 
 * @author Hugo Visser joegi at scene.nl
 */
public class RevertResourceManager implements IResourceChangeListener {

	public void resourceChanged(IResourceChangeEvent event) {		
        final List<IResourceDelta> addedFileResources = new ArrayList<IResourceDelta>();

        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {

                public boolean visit(IResourceDelta delta) throws CoreException {
                	IResource resource = delta.getResource();
                	if (resource.getType()==IResource.PROJECT) {
                		IProject project = (IProject)resource;
						if (!project.isAccessible()) {
							return false; // i.e., closed project
						}
						if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
							return false; // ignore project open
						} 
						if (!SVNWorkspaceRoot.isManagedBySubclipse(project)) {
							return false; // not a svn handled project
						}
                	}
                	else if (resource.getType() == IResource.FILE) {
                		if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED) {
                        	addedFileResources.add(delta);
                        }  
                        else if (delta.getKind() == IResourceDelta.REMOVED) {
                        	SVNMoveDeleteHook.removeFromDeletedFileList((IFile)delta.getResource());
                        }
                	}
                    return true;
                }

            });
            if (!addedFileResources.isEmpty()) {
                final IResourceDelta[] resources = (IResourceDelta[]) addedFileResources.toArray(new IResourceDelta[addedFileResources
                                                                                                     .size()]);                
                final ISVNLocalResource[] revertResources = processResources(resources);
                if (revertResources.length > 0) {
                	
                	List<IProject> projects = new ArrayList<IProject>();
                	for (ISVNLocalResource revertResource : revertResources) {
                		IProject project = revertResource.getResource().getProject();
						if (project != null && !projects.contains(project)) {
							projects.add(project);
						}
                	}
    				IProject[] projectArray = new IProject[projects.size()];
    				projects.toArray(projectArray);

    				JobUtility.scheduleJob("Revert Resources", new Runnable() {				
    					public void run() {
    						try {
	    						for (int i=0; i < revertResources.length; i++) {
	    							if (revertResources[i] instanceof ISVNLocalFile) {
	    								ISVNLocalFile res = (ISVNLocalFile)revertResources[i];
	    								
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
	    				    	            	if (file.exists()) {
	    				    	            		throw SVNException.wrapException(new IllegalStateException("Could not remove "+file));  
	    				    	            	}
	    				    	            }
	    				    	            if (!tmp.renameTo(file)) {
	    				    	                //TODO handle this in a better way?
	    				    	                throw SVNException.wrapException(new IllegalStateException("Could not rename "+tmp+" to "+file));                                
	    				    	            }
	    				    	            
	    				    	            try {
												res.getParent().getResource().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
											} catch (CoreException e) {}
	    				    	            
	    				    	        } else {
	    				    	            //TODO handle this in a better way?
	    				    	            throw SVNException.wrapException(new IllegalStateException("Could not rename "+file+" to "+tmp));
	    				    	        }					
	    							}
	    							else {
	    								revert(revertResources[i]);
	    							}
	    						}
    						} catch (SVNException e) {
    							SVNProviderPlugin.log(e.getStatus());
    						}
    					}
    				}, MultiRule.combine(projectArray), false);
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

	private ISVNLocalResource[] processResources(IResourceDelta[] resources) throws CoreException {
    	List<ISVNLocalResource> revertedResources = new ArrayList<ISVNLocalResource>();
        for (IResourceDelta resourceDelta : resources) {
        	IResource resource = resourceDelta.getResource();
            if (resource.getType() == IResource.FILE) {
                ISVNLocalFile res = SVNWorkspaceRoot.getSVNFileFor((IFile) resource);
                if (res.getFile().exists()) {
                	boolean deleted;
                	if (resourceDelta.getKind() == IResourceDelta.ADDED)
                		deleted = res.getStatusFromCache().isDeleted();
                	else {
                		deleted = SVNMoveDeleteHook.isDeleted((IFile)resource);
                		if (deleted) SVNMoveDeleteHook.removeFromDeletedFileList((IFile)resource);
                	}
                    if (deleted) {
                        revertedResources.add(res);
                    }
                    ISVNLocalFolder parentFolder = res.getParent();
                    while (parentFolder != null) {
                        if (parentFolder.getStatusFromCache().isDeleted() && !parentFolder.getResource().exists() && !revertedResources.contains(parentFolder)) {
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
    	ISVNClientAdapter svnClient = null;
        try {
            svnClient = resource.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.revert(resource.getFile(), false);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	resource.getRepository().returnSVNClient(svnClient);
            OperationManager.getInstance().endOperation();
        }
    }

}
