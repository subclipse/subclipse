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
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNKeywords;
import org.tigris.subversion.svnclientadapter.SVNUrl;

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
		ISVNStatus status = getStatus();
        SVNUrl url = status.getUrl();
		return new RemoteFile(
			null, // parent : we don't know it 
			getRepository(),
            url,
            status.getRevision(), // can't use BASE for remote resources
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
            // by removing the session property we force LocalFile to refresh its
            // status next time getStatus is called
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
		
		ISVNStatus status = getStatus();
		return status.getTextStatus() == ISVNStatus.Kind.MODIFIED;
	}

    /*
     * @see ISVNResource#accept(ISVNResourceVisitor)
     */
    public void accept(ISVNResourceVisitor visitor) throws SVNException {
        visitor.visitFile(this);
    }
    
    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#setKeywords(org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public void setKeywords(SVNKeywords svnKeywords) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.setKeywords(getFile(),svnKeywords,false);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#addKeywords(org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public void addKeywords(SVNKeywords svnKeywords) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.addKeywords(getFile(),svnKeywords);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }        
    }
    
    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#removeKeywords(org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public void removeKeywords(SVNKeywords svnKeywords) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.removeKeywords(getFile(),svnKeywords);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }        
    }
    
    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#getKeywords()
     */
    public SVNKeywords getKeywords() throws SVNException {
    try {
        ISVNClientAdapter svnClient = getRepository().getSVNClient();
        OperationManager.getInstance().beginOperation(svnClient);
        return svnClient.getKeywords(getFile());
    } catch (SVNClientException e) {
        throw SVNException.wrapException(e); 
    } finally {
        OperationManager.getInstance().endOperation();
    }        
}

}


