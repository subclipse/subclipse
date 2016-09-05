package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.svnclientadapter.ISVNInfo;

public class RevisionGraphEditorInput implements IEditorInput {
	
	private IResource resource;
	private ISVNRemoteResource remoteResource;
	private ISVNInfo info;

	public RevisionGraphEditorInput(IResource resource) {
		this.resource = resource;
	}
	
	public RevisionGraphEditorInput(ISVNRemoteResource remoteResource) {
		this.remoteResource = remoteResource;
	}
	
	public IResource getResource() {
		return resource;
	}
	
	public ISVNRemoteResource getRemoteResource() {
		return remoteResource;
	}

	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return PlatformUI.getWorkbench().getEditorRegistry()
			.getImageDescriptor(getName());
	}

	public String getName() {
		if (resource == null) return remoteResource.getName();
		else return resource.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return getName();
	}

	public Object getAdapter(Class adapter) {
		if (resource == null) {
			if (adapter == ISVNRemoteResource.class) {
				return remoteResource;
			}
			return remoteResource.getAdapter(adapter);
		} else {
			if(adapter == IResource.class) {
				return resource;
			}
			return resource.getAdapter(adapter);
		}
	}
	
	public ISVNInfo getInfo() {
		return info;
	}

	public void setInfo(ISVNInfo info) {
		this.info = info;
	}

	public boolean equals(Object obj) {
		if (obj instanceof RevisionGraphEditorInput) {
			RevisionGraphEditorInput compareTo = (RevisionGraphEditorInput)obj;
			if (resource != null && compareTo.getResource() != null) return resource.equals(compareTo.getResource());
			if (remoteResource != null && compareTo.getRemoteResource() != null) return remoteResource.equals(compareTo.getRemoteResource());
		}
		return super.equals(obj);
	}

}
