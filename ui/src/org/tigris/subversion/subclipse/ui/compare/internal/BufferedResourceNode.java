/*******************************************************************************
 * copied from: org.eclipse.compare.internal.BufferedResourceNode
 * 
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.compare.internal;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;

/**
 * A buffer for a workspace resource.
 */
public class BufferedResourceNode extends ResourceNode {
	
	private boolean fDirty= false;
	private IFile fDeleteFile;
		
	/**
	 * Creates a <code>ResourceNode</code> for the given resource.
	 *
	 * @param resource the resource
	 */
	public BufferedResourceNode(IResource resource) {
		super(resource);
	}
	
    /*
     * Returns <code>true</code> if buffer contains uncommitted changes.
     */
	public boolean isDirty() {
	    return fDirty;
	}
	
	protected IStructureComparator createChild(IResource child) {
		return new BufferedResourceNode(child);
	}
		
	public void setContent(byte[] contents) {
		fDirty= true;
		super.setContent(contents);
	}	

	/*
	 * Commits buffered contents to resource.
	 */
	public void commit(IProgressMonitor pm) throws CoreException {
		if (fDirty) {
			
			if (fDeleteFile != null) {
				fDeleteFile.delete(true, true, pm);
				return;
			}
			
			IResource resource= getResource();
			if (resource instanceof IFile) {

				byte[] bytes= getContent();
				ByteArrayInputStream is= new ByteArrayInputStream(bytes);
				try {
					IFile file= (IFile) resource;
					if (file.exists())
						file.setContents(is, false, true, pm);
					else
						file.create(is, false, pm);
					fDirty= false;
				} finally {
					if (is != null)
						try {
							is.close();
						} catch(IOException ex) {
							// Silently ignored
						}
				}
			}
		}
	}
	
	public ITypedElement replace(ITypedElement child, ITypedElement other) {
		
		if (child == null) {	// add resource
			// create a node without a resource behind it!
			IResource resource= getResource();
			if (resource instanceof IFolder) {
				IFolder folder= (IFolder) resource;
				IFile file= folder.getFile(other.getName());
				child= new BufferedResourceNode(file);
			}
		}
		
		if (other == null) {	// delete resource
			IResource resource= getResource();
			if (resource instanceof IFolder) {
				IFolder folder= (IFolder) resource;
				IFile file= folder.getFile(child.getName());
				if (file != null && file.exists()) {
					fDeleteFile= file;
					fDirty= true;
				}
			}
			return null;
		}
		
		if (other instanceof IStreamContentAccessor && child instanceof IEditableContent) {
			IEditableContent dst= (IEditableContent) child;
			
			try {
				InputStream is= ((IStreamContentAccessor)other).getContents();
				byte[] bytes= Utilities.readBytes(is);
				if (bytes != null)
					dst.setContent(bytes);
			} catch (CoreException ex) {
				// NeedWork
			}
		}
		return child;
	}

}
