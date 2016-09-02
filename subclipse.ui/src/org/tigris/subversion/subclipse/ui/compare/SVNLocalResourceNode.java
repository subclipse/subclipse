package org.tigris.subversion.subclipse.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.internal.BufferedResourceNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Node representing a local SVN file.  We can query the status of the resource to determine if
 * it has changed.  It is also used to write the contents back to the file when setContent is called.
 */
public class SVNLocalResourceNode extends BufferedResourceNode {
	private final ISVNLocalResource svnResource;
	private ResourceEditionNode remoteResource = null;
	private ArrayList fChildren = null;
    
    public SVNLocalResourceNode(ISVNLocalResource svnResource) {
		super(svnResource.getIResource());
        this.svnResource = svnResource;
	}
	protected InputStream createStream() throws CoreException {
		return ((IFile)getResource()).getContents();
	}
	
    public ISVNLocalResource getLocalResource() {
        return svnResource;
    }
    public void setRemoteResource(ResourceEditionNode remote) {
    	remoteResource = remote;
    }
    
	//	@Override
	public String getName() {
		String name = svnResource.getFile().getName();
		if (name == null) {
			name = svnResource.getName();
		}
		if (name != null) {
			return name;
		}
		return super.getName();
	}
    
	// used by getContentsAction
	public void setContent(byte[] contents) {
		if (contents == null) contents = new byte[0];
		final InputStream is = new ByteArrayInputStream(contents);
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException  {
				try {
					IFile file = (IFile)getResource();
					if (is != null) {
						if (!file.exists()) {
							file.create(is, false, monitor);
						} else {
							file.setContents(is, false, true, monitor);
						}
					} else {
						file.delete(false, true, monitor);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(false, false, runnable);
		} catch (InvocationTargetException e) {
			SVNUIPlugin.openError(SVNUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell(), Policy.bind("TeamFile.saveChanges", svnResource.getName()), null, e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Ignore
		}
		fireContentChanged();
	}
	
    public Object[] getChildren() {
        if (fChildren == null) {
            fChildren= new ArrayList();
 	        if (remoteResource instanceof ResourceEditionNode) {
	            try {
	            	if (!getLocalResource().isDirty() && getLocalResource().getResource().getProjectRelativePath().toString().equals(remoteResource.getRemoteResource().getProjectRelativePath()) &&
	            			getLocalResource().getStatus().getLastChangedRevision().equals(remoteResource.getRemoteResource().getLastChangedRevision())) {
	            		return fChildren.toArray();
	            	}
	            }
	            catch(CoreException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
	        }

            if (svnResource instanceof ISVNLocalFolder) {
                try {
                    ISVNLocalResource[] members = (ISVNLocalResource[])((ISVNLocalFolder)svnResource).members(null, ISVNFolder.ALL_EXISTING_UNIGNORED_MEMBERS);
                    for (int i= 0; i < members.length; i++) {
                        IStructureComparator child= createChild(members[i]);
                        if (child != null)
                            fChildren.add(child);
                    }
                } catch (CoreException ex) {
                    // NeedWork
                }
            }
        }
        return fChildren.toArray();
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ResourceNode#createChild(org.eclipse.core.resources.IResource)
	 */
	protected IStructureComparator createChild(ISVNLocalResource child) {
		return new SVNLocalResourceNode(child);
	}

}
