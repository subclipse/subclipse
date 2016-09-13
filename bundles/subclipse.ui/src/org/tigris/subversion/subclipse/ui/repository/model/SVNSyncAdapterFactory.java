package org.tigris.subversion.subclipse.ui.repository.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;

public class SVNSyncAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ISynchronizeModelElement) {
			ISynchronizeModelElement modelElement = (ISynchronizeModelElement)adaptableObject;
			return modelElement.getResource();
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IResource.class };
	}

}
