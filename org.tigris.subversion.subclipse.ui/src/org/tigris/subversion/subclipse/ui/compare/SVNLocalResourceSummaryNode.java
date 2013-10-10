package org.tigris.subversion.subclipse.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.compare.internal.BufferedResourceNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;

public class SVNLocalResourceSummaryNode extends BufferedResourceNode {
	private final ISVNLocalResource svnResource;
	private ArrayList fChildren = null;
	private SVNDiffSummary[] diffSummary;
	private String rootLocation;
    
    public SVNLocalResourceSummaryNode(ISVNLocalResource svnResource, SVNDiffSummary[] diffSummary, String rootLocation) {
		super(svnResource.getIResource());
        this.svnResource = svnResource;
        this.diffSummary = diffSummary;
        this.rootLocation = rootLocation;
	}
	protected InputStream createStream() throws CoreException {
		return ((IFile)getResource()).getContents();
	}
	
    public ISVNLocalResource getLocalResource() {
        return svnResource;
    }
    
	//	@Override
	public String getName() {
		String name = null;
		try {
			ISVNRemoteResource baseResource = svnResource.getBaseResource();
			if (baseResource != null) {
				name = baseResource.getName();
			}
		} catch (SVNException e) {}
		
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
            if (svnResource instanceof ISVNLocalFolder) {
                try {
                    ISVNLocalResource[] members = (ISVNLocalResource[])((ISVNLocalFolder)svnResource).members(null, ISVNFolder.ALL_EXISTING_UNIGNORED_MEMBERS);
                    for (int i= 0; i < members.length; i++) {
                    	if (include(members[i])) {
	                        IStructureComparator child= createChild(members[i]);
	                        if (child != null)
	                            fChildren.add(child);
                    	}
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
		return new SVNLocalResourceSummaryNode(child, diffSummary, rootLocation);
	}
	
	private boolean include(ISVNLocalResource member) {
		String relativeLocation = member.getResource().getLocation().toString().substring(rootLocation.length() + 1);
		for (int i = 0; i < diffSummary.length; i++) {
			if (diffSummary[i].getPath().equals(relativeLocation)) {
				return true;
			}
		}
		return false;
	}
}