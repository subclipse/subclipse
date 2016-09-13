package org.tigris.subversion.subclipse.ui.compare;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class PropertyCompareLocalResourceNode implements IStructureComparator, ITypedElement, IStreamContentAccessor, IPropertyProvider {
	private IResource resource;
	private boolean recursive;
	private ISVNProperty[] properties;
	
	private Object[] children;
	
	public PropertyCompareLocalResourceNode(IResource resource, boolean recursive, ISVNProperty[] properties) {
		this.resource = resource;
		this.recursive = recursive;
		this.properties = properties;
	}

	public InputStream getContents() throws CoreException {
		return null;
	}

	public String getName() {
		return resource.getName();
	}

	public Image getImage() {
		return CompareUI.getImage(resource);
	}

	public String getType() {
		return FOLDER_TYPE;
	}

	public Object[] getChildren() {
		if (children == null) {
			List<Object> childList = new ArrayList<Object>();
			for (ISVNProperty property : properties) {
				if (property.getFile().getAbsolutePath().equals(resource.getLocation().toOSString())) {
					childList.add(new PropertyComparePropertyNode(property));
				}
			}
			if (recursive && resource instanceof IContainer) {
				try {
					IResource[] childResources = ((IContainer)resource).members();
					for (IResource childResource : childResources) {
						PropertyCompareLocalResourceNode childNode = new PropertyCompareLocalResourceNode(childResource, true, properties);
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

	public void getProperties(boolean recursive) {
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
    	ISVNClientAdapter svnClient = null;
    	try {
        	svnClient = svnResource.getRepository().getSVNClient();
        	properties = svnClient.getProperties(resource.getLocation().toFile(), recursive);
    	} catch (Exception e) {
    		SVNUIPlugin.log(Status.ERROR, e.getMessage(), e);
		}
    	finally {
    		svnResource.getRepository().returnSVNClient(svnClient);
    	}		
	}

	public String getLabel() {
		return Policy.bind("PropertyCompareLocalResourceNode.0") + resource.getName(); //$NON-NLS-1$
	}
	
	public boolean isEditable() {
		return true;
	}

	public IResource getResource() {
		return resource;
	}

}
