/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResourceVisitor;
import org.tigris.subversion.subclipse.core.SVNException;

import com.qintsoft.jsvn.jni.Status;

/**
 * Represents handles to SVN resource on the local file system. Synchronization
 * information is taken from the .svn subdirectories. 
 */
public class LocalFile extends LocalResource implements ISVNLocalFile {

	/**
	 * Create a handle based on the given local resource.
	 */
	public LocalFile(IFile file) {
		super(file);
	}

    /**
     * get the remote resource corresponding to this local file
     * @return null if file is not managed
     */	
	public ISVNRemoteResource getRemoteResource() throws SVNException {
		if (!isManaged())
			return null;
		Status status = getStatus();
		return new RemoteFile(
			null, // parent : we don't know it 
			getRepository(),
			status.getUrl(), // url
			false, // hasProps
			status.getLastChangedRevision(),
			status.getLastChangedDate(),
			status.getLastCommitAuthor());
	}

    /**
     * @see ISVNLocalResource#refreshStatus
     */
    public void refreshStatus() {
        try {
            resource.setSessionProperty(RESOURCE_SYNC_KEY, null);
        } catch (CoreException e) {
            // the resource does not exist, we ignore the exception
        }
        
    }
	
	/*
	 * @see ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}
	
	/*
	 * @see ISVNLocalResource#isModified()
	 */
	public boolean isModified() throws SVNException {
		if (!exists()) return true;
		
		Status status = getStatus();
		return status.getTextStatus() == Status.Kind.modified;
	}

    /*
     * @see ISVNResource#accept(ISVNResourceVisitor)
     */
    public void accept(ISVNResourceVisitor visitor) throws SVNException {
        visitor.visitFile(this);
    }

}


