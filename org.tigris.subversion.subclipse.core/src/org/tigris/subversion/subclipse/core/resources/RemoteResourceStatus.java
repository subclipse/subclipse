/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;
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
public class RemoteResourceStatus extends ResourceStatus {

	/** Singleton instance of the status "None" */
	public static final RemoteResourceStatus NONE = new RemoteResourceStatusNone();

    static final long serialVersionUID = 1L;

    protected long repositoryRevision;

    /**
     * Factory method created instance from byte[]
     * @param bytes
     * @return a new instance created from given bytes or null
     * @throws SVNException
     */
    public static RemoteResourceStatus fromBytes(byte[] bytes) throws SVNException
    {
    	return ((bytes != null) && (bytes.length > 0)) ? new RemoteResourceStatus(bytes) : null;
    }

    protected RemoteResourceStatus() 
    {
    	super();
    }
    
    /**
     * Constructor
     * @param realStatus
     * @param revision
     */
	public RemoteResourceStatus(ISVNStatus realStatus, SVNRevision.Number revision) {
		super(realStatus, null, true);
		
        this.textStatus = realStatus.getRepositoryTextStatus().toInt();
        this.propStatus = realStatus.getRepositoryPropStatus().toInt();
        
        if (revision == null) {
        	this.repositoryRevision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
        	this.repositoryRevision = revision.getNumber();
        }
        
        if (SVNStatusKind.EXTERNAL.equals(realStatus.getTextStatus()))
        {
        	this.textStatus = realStatus.getTextStatus().toInt();
        }
	}

    /**
     * (Re)Construct an object from the given bytes 
     * @param bytes
     * @throws SVNException
     */
    protected RemoteResourceStatus(byte[] bytes) throws SVNException {
    	super();    	
    	StatusFromBytesStream in = new StatusFromBytesStream(bytes);
    	initFromBytes(in);
    }

	/**
	 * Answer the revision number. Contrary to getRevision() of
	 * localResourceStatus, this is the revision of the repository at the time
	 * of fetching this status via svn status call ... 
	 * (And meanwhile the localResourceStatus was changed, 
	 * so it even does not store it's revision anymore)
	 * @return revision of the resource in repository
	 */
	public Number getRepositoryRevision() {
		if (repositoryRevision == SVNRevision.SVN_INVALID_REVNUM) {
			return null;
		} else {
			return new SVNRevision.Number(repositoryRevision);
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

    	aValue = info.getNodeKind();
    	if (aValue != null)
    		this.nodeKind = ((SVNNodeKind) aValue).toInt();

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

    	aValue = info.getUrl();
        if (aValue == null) {
            this.url = null;
        } else {
            this.url = ((SVNUrl) aValue).toString();
        }
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#getBytesInto(org.tigris.subversion.subclipse.core.resources.ResourceStatus.StatusToBytesStream)
     */
    protected void getBytesInto(StatusToBytesStream dos) {
    	super.getBytesInto(dos);
        try {
        	
        	// repositoryRevision
            dos.writeLong(repositoryRevision);

            // file
            dos.writeString(file.getAbsolutePath());

        } catch (IOException e) {
            return;
        }
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#initFromBytes(java.io.DataInputStream)
     */
    protected int initFromBytes(StatusFromBytesStream dis) throws SVNException {
    	int version = super.initFromBytes(dis);    	
        try {
        	if (version >= FORMAT_VERSION_3) {
        		readFromVersion3Stream(dis);            	
        	} else {
        		readFromVersion2Stream(dis);
        	}
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create RemoteResourceStatus from bytes", e);
        }
        return version;
    }

	private void readFromVersion3Stream(StatusFromBytesStream dis) throws IOException {

    	// repositoryRevision
		repositoryRevision = dis.readLong();

        // file
        file = new File(dis.readString());
	}

	/**
	 * Just for backwards compatibility with workspaces stored with previous versions
	 * @param dis
	 * @throws IOException
	 * @deprecated
	 */
	private void readFromVersion2Stream(StatusFromBytesStream dis) throws IOException {
        file = new File(dis.readUTF());		
	}
    
    /**
     * Set the revision number - for internal purposes only.
     * This method is expected to be called from initFromBytes() method only!  
     */
    protected void setRevisionNumber(long revision)
    {
    	this.repositoryRevision = revision;
    }

	public SVNStatusKind getRepositoryPropStatus() {
		return getPropStatus();
	}

	public SVNStatusKind getRepositoryTextStatus() {
		return getTextStatus();
	}

	public Number getRevision() {
		return getRepositoryRevision();
	}
	
	public String getMovedFromAbspath() {
		//This is remote/repository status. It's irrelevant here. 
		return null;
	}
	
	public String getMovedToAbspath() {
		//This is remote/repository status. It's irrelevant here. 
		return null;
	}

	public File getConflictNew() {
		//This is remote/repository status. It's irrelevant here. 
		return null;
	}

	public File getConflictOld() {
		//This is remote/repository status. It's irrelevant here. 
		return null;
	}

	public File getConflictWorking() {
		//This is remote/repository status. It's irrelevant here. 
		return null;
	}

	public boolean isCopied() {
		//This is remote/repository status. It's irrelevant here. 
		return false;
	}

	public boolean isSwitched() {
		//This is remote/repository status. It's irrelevant here. 
		return false;
	}

	public boolean isWcLocked() {
		//This is remote/repository status. It's irrelevant here. 
		return false;
	}   

	public String getLockComment() {
		// TODO Locks are not yet supported for RemoteResourceStatus
		throw new UnsupportedOperationException("Locks are not suported for RemoteResourceStatus");
	}

	public Date getLockCreationDate() {
		// TODO Locks are not yet supported for RemoteResourceStatus
		throw new UnsupportedOperationException("Locks are not suported for RemoteResourceStatus");
	}

	public String getLockOwner() {
		// TODO Locks are not yet supported for RemoteResourceStatus
		throw new UnsupportedOperationException("Locks are not suported for RemoteResourceStatus");
	}

    /**
     * Special RemoteResourceStatus subclass representing status "None".
     */
    public static class RemoteResourceStatusNone extends RemoteResourceStatus {
        static final long serialVersionUID = 1L;

    	protected RemoteResourceStatusNone()
    	{
    		super();
    		this.nodeKind = SVNNodeKind.UNKNOWN.toInt();
            this.repositoryRevision = SVNRevision.SVN_INVALID_REVNUM;
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
