/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResourceVisitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNKeywords;

/**
 * Represents handles to SVN file on the local file system.
 */
public class LocalFile extends LocalResource implements ISVNLocalFile {

	/**
	 * Create a handle based on the given local resource.
	 */
	public LocalFile(IFile file) {
		super(file);
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#getBaseResource()
     */
    public ISVNRemoteResource getBaseResource() throws SVNException {   	
		if (!hasRemote()) {// no base if no remote
			return null;
		}
		return new BaseFile(getStatus());
    }	
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#refreshStatus()
     */
    public void refreshStatus() throws SVNException {
    	SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(resource, IResource.DEPTH_ZERO);
    }
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#isDirty()
     */
    public boolean isDirty() throws SVNException {
        return getStatus().isDirty();
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#accept(org.tigris.subversion.subclipse.core.ISVNResourceVisitor)
     */
    public void accept(ISVNResourceVisitor visitor) throws SVNException {
        visitor.visitFile(this);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#setKeywords(org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public void setKeywords(SVNKeywords svnKeywords) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.setKeywords(getFile(), svnKeywords, false);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#addKeywords(org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public void addKeywords(SVNKeywords svnKeywords) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.addKeywords(getFile(), svnKeywords);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }        
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#removeKeywords(org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public void removeKeywords(SVNKeywords svnKeywords) throws SVNException {
        try {
            ISVNClientAdapter svnClient = getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            svnClient.removeKeywords(getFile(), svnKeywords);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e); 
        } finally {
            OperationManager.getInstance().endOperation();
        }        
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalFile#getKeywords()
     */
    public SVNKeywords getKeywords() throws SVNException {
		try {
			ISVNClientAdapter svnClient = getRepository().getSVNClient();
			return svnClient.getKeywords(getFile());
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e);
		}
	}
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.ISVNLocalResource#revert()
     */
    public void revert() throws SVNException {
       super.revert(false);
    }    
}


