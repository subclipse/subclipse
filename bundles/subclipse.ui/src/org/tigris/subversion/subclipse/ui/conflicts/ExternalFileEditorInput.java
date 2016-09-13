/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ExternalFileEditorInput implements IPathEditorInput, IStorageEditorInput, ILocationProvider  {
	
	private class WorkbenchAdapter implements IWorkbenchAdapter {

		public Object[] getChildren(Object o) {
			return null;
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		public String getLabel(Object o) {
			return ((ExternalFileEditorInput)o).getName();
		}

		public Object getParent(Object o) {
			return null;
		}
	}

	private IFileStore fFileStore;
	private WorkbenchAdapter fWorkbenchAdapter= new WorkbenchAdapter();
	private IStorage fStorage;
	private IPath fPath;
	
	public ExternalFileEditorInput(IFileStore fileStore) {
		fFileStore= fileStore;
		fWorkbenchAdapter= new WorkbenchAdapter();
	}

	public boolean exists() {
		return fFileStore.fetchInfo().exists();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return fFileStore.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return fFileStore.toString();
	}

	public Object getAdapter(Class adapter) {
		if (ILocationProvider.class.equals(adapter))
			return this;
		if (IWorkbenchAdapter.class.equals(adapter))
			return fWorkbenchAdapter;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public IPath getPath(Object element) {
		if (element instanceof ExternalFileEditorInput)
			return ((ExternalFileEditorInput)element).getPath();
		
		return null;
	}

    public IPath getPath() {
    	if (fPath == null)
    		fPath= new Path(fFileStore.toURI().getPath());
    	return fPath;
    }

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof ExternalFileEditorInput) {
			ExternalFileEditorInput input= (ExternalFileEditorInput) o;
			return fFileStore.equals(input.fFileStore);
		}

        if (o instanceof IPathEditorInput) {
            IPathEditorInput input= (IPathEditorInput)o;
            return getPath().equals(input.getPath());
        }

		return false;
	}

	public int hashCode() {
		return fFileStore.hashCode();
	}

	public IStorage getStorage() throws CoreException {
		if (fStorage == null)
			fStorage= new ExternalFileStorage(fFileStore);
		return fStorage;
	}
}
