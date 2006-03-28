/* ***************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *
 * ***************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

/**
 * This class has an interface which is very similar to ISVNStatus but we make
 * sure to take as little memory as possible. This class also have a getBytes()
 * method and a constructor/factory method that takes bytes.
 * (However, the bytes are not complete representation of this status, just subset of interesting attributes) 
 * 
 * Since we want to add/modify the nodeKind the setter is also there
 * @see org.tigris.subversion.svnclientadapter.ISVNStatus
 * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus
 */
/**
 * @author Martin
 *
 */
public class RemoteResourceStatus extends ResourceStatus {

	public static final RemoteResourceStatus NONE = new RemoteResourceStatusNone();

    static final long serialVersionUID = 1L;

    protected long revision;

    public static RemoteResourceStatus fromBytes(byte[] bytes) throws SVNException
    {
    	return ((bytes != null) && (bytes.length > 0)) ? new RemoteResourceStatus(bytes) : null;
    }

    protected RemoteResourceStatus() {}

    /**
     * 
     * @param realStatus
     * @param revision
     */
	public RemoteResourceStatus(ISVNStatus realStatus, SVNRevision.Number revision) {
		super(realStatus, null);
		
        this.textStatus = realStatus.getRepositoryTextStatus().toInt();
        this.propStatus = realStatus.getRepositoryPropStatus().toInt();
        
        if (revision == null) {
        	this.revision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
        	this.revision = revision.getNumber();
        }
        
        if (SVNStatusKind.EXTERNAL.equals(realStatus.getTextStatus()))
        {
        	this.textStatus = realStatus.getTextStatus().toInt();
        }
	}
	
	/**
	 * Answer the revision number. Contrary to getRevision() of
	 * localResourceStatus, this is the revision of the repository at the time
	 * of fetching this status via svn status call ... 
	 * (And meanwhile the localResourceStatus was changed, 
	 * so it even does not store it's revision anymore)
	 */
	public Number getRepositoryRevision() {
		if (revision == SVNRevision.SVN_INVALID_REVNUM) {
			return null;
		} else {
			return new SVNRevision.Number(revision);
		}
	}
    
	/**
	 * Update missing data from the supplied info
	 * 
	 * @param info
	 */
	public void updateFromInfo(ISVNInfo info)
	{
		if (info == null) return;
		
    	/** a temporary variable serving as immediate cache for various status values */
    	Object aValue = null;

		this.nodeKind = info.getNodeKind().toInt();

        aValue = info.getLastChangedDate();
        if (aValue == null) {
            this.lastChangedDate = -1;
        } else {
            this.lastChangedDate = ((Date) aValue).getTime();
        }

        aValue = info.getLastChangedRevision();
        if (aValue == null) {
            this.lastChangedRevision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
            this.lastChangedRevision = ((SVNRevision.Number) aValue).getNumber();
        }

        this.lastCommitAuthor = info.getLastCommitAuthor();

//        aValue = info.getRevision();
//        if (aValue == null) {
//            this.revision = SVNRevision.SVN_INVALID_REVNUM;
//        } else {
//            this.revision = ((SVNRevision.Number) aValue).getNumber();
//        }

    	aValue = info.getUrl();
        if (aValue == null) {
            this.url = null;
        } else {
            this.url = ((SVNUrl) aValue).toString();
        }
	}
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#getBytes()
     */
    public byte[] getBytes() {    	
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getBytesInto(new DataOutputStream(out));
        return out.toByteArray();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#getBytesInto(java.io.DataOutputStream)
     */
    protected DataOutputStream getBytesInto(DataOutputStream dos) {
    	super.getBytesInto(dos);
        try {
            // file
            dos.writeUTF(path);

        } catch (IOException e) {
            return null;
        }
        return dos;
    }

    /**
     * (Re)Construct an object from the given bytes 
     * @param bytes
     * @throws SVNException
     */
    protected RemoteResourceStatus(byte[] bytes) throws SVNException {
    	super();    	
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(in);
    	initFromBytes(dis);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#initFromBytes(java.io.DataInputStream)
     */
    protected int initFromBytes(DataInputStream dis) throws SVNException {
    	int version = super.initFromBytes(dis);
        try {
            // path
            path = dis.readUTF();
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create LocalResourceStatus from bytes", e);
        }
        return version;
    }

    /**
     * Answer the revision number - for internal purposes only.
     * This method is expected to be called from getBytesInto() method only!  
     *
     * @return
     */
    protected long getRevisionNumber()
    {
    	return revision;
    }
    
    /**
     * Set the revision number - for internal purposes only.
     * This method is expected to be called from initFromBytes() method only!  
     *
     * @return
     */
    protected void setRevisionNumber(long revision)
    {
    	this.revision = revision;
    }

    
    public static class RemoteResourceStatusNone extends RemoteResourceStatus {
        static final long serialVersionUID = 1L;

    	public RemoteResourceStatusNone()
    	{
    		super();
    		this.nodeKind = SVNNodeKind.UNKNOWN.toInt();
            this.revision = SVNRevision.SVN_INVALID_REVNUM;
            this.textStatus = SVNStatusKind.NONE.toInt();
            this.propStatus = SVNStatusKind.NONE.toInt();
            //this.path = status.getFile().getAbsolutePath();
    	}
    	
    	public IResource getResource()
    	{
    		return null;
    	}
    }   
}
