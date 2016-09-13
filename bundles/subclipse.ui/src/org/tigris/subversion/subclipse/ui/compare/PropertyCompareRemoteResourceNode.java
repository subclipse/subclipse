package org.tigris.subversion.subclipse.ui.compare;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class PropertyCompareRemoteResourceNode implements IStructureComparator, ITypedElement, IStreamContentAccessor, IPropertyProvider {
	private ISVNRemoteResource remoteResource;
	private SVNRevision pegRevision;
	private boolean recursive;
	private ISVNProperty[] properties;
	
	private Object[] children;
	
	public PropertyCompareRemoteResourceNode(ISVNRemoteResource remoteResource, SVNRevision pegRevision, boolean recursive, ISVNProperty[] properties) {
		this.remoteResource = remoteResource;
		this.pegRevision = pegRevision;
		this.recursive = recursive;
		this.properties = properties;
	}

	public void getProperties(boolean recursive) {
		ISVNClientAdapter svnClient = null;
		try {
			svnClient = remoteResource.getRepository().getSVNClient();
			properties = svnClient.getProperties(remoteResource.getUrl(), remoteResource.getRevision(), pegRevision, recursive);
		} catch (Exception e) {
			SVNUIPlugin.log(Status.ERROR, e.getMessage(), e);
		}
    	finally {
    		remoteResource.getRepository().returnSVNClient(svnClient);
    	}		
	}

	public String getLabel() {
		return Policy.bind("PropertyCompareRemoteResourceNode.0") + remoteResource.getName().replaceAll("%20", " ") + "@" + remoteResource.getRevision(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public boolean isEditable() {
		return false;
	}

	public InputStream getContents() throws CoreException {
		return null;
	}

	public String getName() {
		return remoteResource.getName();
	}

	public Image getImage() {
		return CompareUI.getImage(remoteResource);
	}

	public String getType() {
		return FOLDER_TYPE;
	}

	public Object[] getChildren() {
		if (children == null) {
			List<Object> childList = new ArrayList<Object>();
			for (ISVNProperty property : properties) {
				if (property.getUrl().toString().equals(remoteResource.getUrl().toString())) {
					childList.add(new PropertyComparePropertyNode(property));
				}
			}
			if (recursive && remoteResource.isContainer()) {
				try {
					ISVNRemoteResource[] childResources = remoteResource.members(new NullProgressMonitor());
					for (ISVNRemoteResource childResource : childResources) {
						PropertyCompareRemoteResourceNode childNode = new PropertyCompareRemoteResourceNode(childResource, pegRevision, recursive, properties);
						childList.add(childNode);
					}
				} catch (CoreException e) {
					SVNUIPlugin.log(e);
				}
			}
			children = new Object[childList.size()];
			childList.toArray(children);
		}
		return children;
	}
	
	public ISVNRemoteResource getRemoteResource() {
		return remoteResource;
	}

	@Override
	public boolean equals(Object other) {
		if (!recursive) {
			return true;
		}
		if (other instanceof ITypedElement) {
			String otherName = ((ITypedElement) other).getName();
			return getName().equals(otherName);
		}
		return super.equals(other);
	}
	
	public int hashCode() {
		return getName().hashCode();
	}

}
