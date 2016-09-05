/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
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

import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

/**
 * This class has an interface which is very similar to ISVNStatus but we make
 * sure to take as little memory as possible. This class also have a getBytes()
 * method and a constructor/factory method that takes bytes.
 *
 * This class does NOT stores resource revision intentionally.
 * The revision numbers changes too frequently and it does not provide too valuable imformation for synchronization
 * needs anyway. The lastChangedRevision() is more important here. 
 * 
 * @see org.tigris.subversion.svnclientadapter.ISVNStatus
 */
public class LocalResourceStatus extends ResourceStatus {

	/** Singleton instance of the status "None" */
	public static final LocalResourceStatus NONE = new LocalResourceStatusNone();
	
    protected String urlCopiedFrom = "";
    protected String pathConflictOld;
    protected String pathConflictWorking;
    protected String pathConflictNew;
    protected String lockOwner;
    protected long lockCreationDate;
    protected String lockComment;
    protected boolean readOnly;
    protected boolean isCopied;    
    protected boolean isWcLocked;
    protected boolean isSwitched;
    
    protected String movedFromAbspath;
    protected String movedToAbspath;

    
    static final long serialVersionUID = 1L;

    /**
     * Factory method created instance from byte[]
     * @param bytes
     * @return a new instance created from given bytes or null
     * @throws SVNException
     */
    public static LocalResourceStatus fromBytes(byte[] bytes) throws SVNException
    {
    	return ((bytes != null) && (bytes.length > 0)) ? new LocalResourceStatus(bytes) : null;
    }

    protected LocalResourceStatus()
    {
    	super();
    }

	/**
	 * @param status
	 * @param url - Only needed when status.getUrl is Null, such as
	 *  for an svn:externals folder
	 */
    public LocalResourceStatus(ISVNStatus status, String url, boolean checkForReadOnly) {
    	super(status, url, false);
    	
    	/** a temporary variable serving as immediate cache for various status values */
    	Object aValue = null;

    	if (checkForReadOnly) {
    		 this.readOnly = !getFile().canWrite();
    	}

        aValue = status.getConflictNew();
        if (aValue == null) {
            this.pathConflictNew = null;
        } else {
            this.pathConflictNew = ((File) aValue).getAbsolutePath();
        }

        aValue = status.getConflictOld();
        if (aValue == null) {
            this.pathConflictOld = null;
        } else {
            this.pathConflictOld = ((File) aValue).getAbsolutePath();
        }

        aValue = status.getConflictWorking();
        if (aValue == null) {
            this.pathConflictWorking = null;
        } else {
            this.pathConflictWorking = ((File) aValue).getAbsolutePath();
        }
        
        this.lockOwner = status.getLockOwner();
        this.lockComment = status.getLockComment();
        
        aValue = status.getLockCreationDate();
        if (aValue == null) 
            this.lockCreationDate = -1;
        else
            this.lockCreationDate = ((Date) aValue).getTime();
        
        this.isCopied = status.isCopied();
        this.isWcLocked = status.isWcLocked();
        this.isSwitched = status.isSwitched();
        
        movedFromAbspath = status.getMovedFromAbspath();
        movedToAbspath = status.getMovedToAbspath();
    }


    /**
     * (Re)Construct an object from the given bytes 
     * @param bytes
     * @throws SVNException
     */
    protected LocalResourceStatus(byte[] bytes) throws SVNException {
    	super();    	
    	if (bytes.length < 4) {
    		return;
    	}
    	StatusFromBytesStream in = new StatusFromBytesStream(bytes);
    	initFromBytes(in);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#getBytesInto(org.tigris.subversion.subclipse.core.resources.ResourceStatus.StatusToBytesStream)
     */
    protected void getBytesInto(StatusToBytesStream dos) {
    	super.getBytesInto(dos);
        try {
            // urlCopiedFrom
            dos.writeString(urlCopiedFrom);

            // conflict old
            dos.writeString(pathConflictOld);

            // conflict new
            dos.writeString(pathConflictNew);

            // conflict working
            dos.writeString(pathConflictWorking);
            
            // lock owner
            dos.writeString(lockOwner);
            
            // lock creation date
            dos.writeLong(lockCreationDate);
            
            // lock comment
            dos.writeString(lockComment);
            
            dos.writeBoolean(isCopied);
            dos.writeBoolean(isWcLocked);
            dos.writeBoolean(isSwitched);

            //read only
            dos.writeBoolean(readOnly);

            // file
            dos.writeString(file.getAbsolutePath());
            
            dos.writeString(movedFromAbspath);
            dos.writeString(movedToAbspath);

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
            if (version == FORMAT_VERSION_5 || version == FORMAT_VERSION_4 || version == FORMAT_VERSION_3) {
                readFromVersion3Stream(dis);            	
            } else if (version == FORMAT_VERSION_2) {
            	readFromVersion2Stream(dis);
            } else {
            	readFromVersion1Stream(dis);            	
            }
            if (version == FORMAT_VERSION_5) {
            	readFromVersion5Stream(dis);
            }
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create LocalResourceStatus from bytes", e);
        }
        return version;
    }

	private void readFromVersion3Stream(StatusFromBytesStream dis) throws IOException {
        // urlCopiedFrom
		urlCopiedFrom = dis.readString();

        // conflict old
		pathConflictOld = dis.readString();

        // conflict new
		pathConflictNew = dis.readString();

        // conflict working
		pathConflictWorking = dis.readString();
        
        // lock owner
		lockOwner = dis.readString();
        
        // lock creation date
		lockCreationDate = dis.readLong();
        
        // lock comment
		lockComment = dis.readString();

		isCopied = dis.readBoolean();
		isWcLocked = dis.readBoolean();
		isSwitched = dis.readBoolean();
		
        //read only
		readOnly = dis.readBoolean();

        // file
		file = new File(dis.readString());

	}
	
	private void readFromVersion5Stream(StatusFromBytesStream dis) throws IOException {
		movedFromAbspath = dis.readString();
		movedToAbspath = dis.readString();
	}

	/**
	 * Just for backwards compatibility with workspaces stored with previous version
	 * @param dis
	 * @throws IOException
	 * @deprecated
	 */
	private void readFromVersion2Stream(StatusFromBytesStream dis) throws IOException {
		
		readFromVersion1Stream(dis);
		
		lockOwner = dis.readUTF();
		if (lockOwner.equals(""))
			lockOwner = null;
		lockCreationDate = dis.readLong();
		lockComment = dis.readUTF();
		if (lockComment.equals(""))
			lockComment = null;
		readOnly = dis.readBoolean();
	}

	/**
	 * Just for backwards compatibility with workspaces stored with previous version
	 * @param dis
	 * @throws IOException
	 * @deprecated
	 */
	private void readFromVersion1Stream(StatusFromBytesStream dis) throws IOException {
		// urlCopiedFrom
		String urlCopiedFromString = dis.readUTF();
		if (urlCopiedFromString.equals("")) {
		    urlCopiedFrom = null;
		} else {
		    urlCopiedFrom = url;
		}

		// file
		file = new File(dis.readUTF());

		// conflict old
		pathConflictOld = dis.readUTF();
		if (pathConflictOld.equals(""))
		    pathConflictOld = null;

		// conflict new
		pathConflictNew = dis.readUTF();
		if (pathConflictNew.equals(""))
		    pathConflictNew = null;

		// conflict new
		pathConflictWorking = dis.readUTF();
		if (pathConflictWorking.equals(""))
		    pathConflictWorking = null;
		
		//Pre version 2
	    lockOwner = null;
	    lockCreationDate = 0L;
	    lockComment = null;
	    readOnly = false;
	}

    /**
     * Returns if is managed by svn (added, normal, modified ...)
     * 
     * @return if managed by svn
     */
    public boolean isManaged() {
        return org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils.isManaged(getTextStatus());
    }

    /**
     * Returns if the resource has a remote counter-part
     * 
     * @return has version in repository
     */
    public boolean hasRemote() {
        return org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils.hasRemote(this);
    }

    /**
     * text is considered dirty if text status has status added, deleted,
     * replaced, modified, merged or conflicted.
     * 
     * @return true if the resource text is dirty
     */
    public boolean isTextDirty() {
        SVNStatusKind theTextStatus = getTextStatus();

        return ((theTextStatus.equals(SVNStatusKind.ADDED))
                || (theTextStatus.equals(SVNStatusKind.DELETED))
                || (theTextStatus.equals(SVNStatusKind.REPLACED))
                || (theTextStatus.equals(SVNStatusKind.MODIFIED))
                || (theTextStatus.equals(SVNStatusKind.MERGED)) || (theTextStatus
                .equals(SVNStatusKind.CONFLICTED)));
    }

    /**
     * prop is considered dirty if prop status is either conflicted or modified
     * 
     * @return true if the resource property is dirty
     */
    public boolean isPropDirty() {
        SVNStatusKind thePropStatus = getPropStatus();
        return thePropStatus.equals(SVNStatusKind.CONFLICTED)
                || thePropStatus.equals(SVNStatusKind.MODIFIED);
    }

    /**
     * resource is considered dirty if properties are dirty or text is dirty
     * @return true if the resource text or property is dirty
     */
    public boolean isDirty() {
        return isTextDirty() || isPropDirty();
    }

    public boolean isUnversioned() {
        return getTextStatus().equals(SVNStatusKind.UNVERSIONED);
    }

    public boolean isAdded() {
        return getTextStatus().equals(SVNStatusKind.ADDED);
    }

    public boolean isDeleted() {
        return getTextStatus().equals(SVNStatusKind.DELETED);
    }

    public boolean isMissing() {
    	return getTextStatus().equals(SVNStatusKind.MISSING);
    }
    
    public boolean isReplaced() {
    	return getTextStatus().equals(SVNStatusKind.REPLACED);
    }
    
    public boolean isIgnored() {
        return getTextStatus().equals(SVNStatusKind.IGNORED);
    }

    public boolean isTextMerged() {
        return getTextStatus().equals(SVNStatusKind.MERGED);
    }

    public boolean isTextModified() {
        return getTextStatus().equals(SVNStatusKind.MODIFIED);
    }

    public boolean isTextConflicted() {
        return getTextStatus().equals(SVNStatusKind.CONFLICTED);
    }

    public boolean isPropModified() {
        return getPropStatus().equals(SVNStatusKind.MODIFIED);
    }

    public boolean isPropConflicted() {
        return getPropStatus().equals(SVNStatusKind.CONFLICTED);
    }
    
    public boolean isLocked() {
        return lockOwner != null;
    }

    public void setMovedFromAbspath(String movedFromAbspath) {
		this.movedFromAbspath = movedFromAbspath;
	}

	public void setMovedToAbspath(String movedToAbspath) {
		this.movedToAbspath = movedToAbspath;
	}

	/**
     * the original file without your changes
     * 
     * @return
     */
    public File getConflictOld() {
        if (pathConflictOld == null) {
            return null;
        } else {
            return new File(pathConflictOld);
        }
    }

    /**
     * the file as it is in the repository
     * 
     * @return
     */
    public File getConflictNew() {
        if (pathConflictNew == null) {
            return null;
        } else {
            return new File(pathConflictNew);
        }
    }

    /**
     * your own file with your changes
     * 
     * @return
     */
    public File getConflictWorking() {
        if (pathConflictWorking == null) {
            return null;
        } else {
            return new File(pathConflictWorking);
        }
    }

    public Date getLockCreationDate() {
        if (lockCreationDate == -1) {
            return null;
        } else {
            return new Date(lockCreationDate);
        }
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public String getLockComment() {
        return lockComment;
    }
       
    /**
     * @return true when the resource was copied
     */
    public boolean isCopied()
    {
    	return isCopied;
    }
    
    
    /**
     * @return true when the working copy directory is locked. 
     */
    public boolean isWcLocked()
    {
    	return isWcLocked;
    }
    
    /**
     * @return true when the resource was switched relative to its parent.
     */
    public boolean isSwitched()
    {
    	return isSwitched;
    }

	public SVNStatusKind getRepositoryPropStatus() {
		throw new UnsupportedOperationException("LocalResourceStatus does not provide repository statuses");
	}

	public SVNStatusKind getRepositoryTextStatus() {
		throw new UnsupportedOperationException("LocalResourceStatus does not provide repository statuses");
	}

	public Number getRevision() {
		throw new UnsupportedOperationException("LocalResourceStatus does not provide (repository) revision");
	}
	
    public String getMovedFromAbspath() {
		return movedFromAbspath;
	}

	public String getMovedToAbspath() {
		return movedToAbspath;
	}

	/**
     * Special LocalResourceStatus subclass representing status "None".
     */
    public static class LocalResourceStatusNone extends LocalResourceStatus {
        static final long serialVersionUID = 1L;

    	protected LocalResourceStatusNone()
    	{
    		super();
    		this.nodeKind = SVNNodeKind.UNKNOWN.toInt();
            this.textStatus = SVNStatusKind.NONE.toInt();
            this.propStatus = SVNStatusKind.NONE.toInt();
            this.readOnly = false;
            //this.path = status.getFile().getAbsolutePath();
    	}
    }
}
