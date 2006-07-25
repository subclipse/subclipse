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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;

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
	public static final LocalResourceStatus NONE = new LocalResourceStatusNone();
	
    protected String urlCopiedFrom;
    protected String pathConflictOld;
    protected String pathConflictWorking;
    protected String pathConflictNew;
    protected String lockOwner;
    protected long lockCreationDate;
    protected String lockComment;
    protected boolean readOnly;
    
    static final long serialVersionUID = 1L;

	/**
	 * @param status
	 * @param url - Only needed when status.getUrl is Null, such as
	 *  for an svn:externals folder
	 */
    public LocalResourceStatus(ISVNStatus status, SVNUrl url) {
    	super(status, url);
    	
    	/** a temporary variable serving as immediate cache for various status values */
    	Object aValue = null;
    	
        aValue = status.getUrlCopiedFrom();
        if (aValue == null) {
            this.urlCopiedFrom = null;
        } else {
            this.urlCopiedFrom = ((SVNUrl) aValue).toString();
        }

        this.path = status.getFile().getAbsolutePath();        
        this.readOnly = !status.getFile().canWrite();

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
    }

    public SVNUrl getUrlCopiedFrom() {
        if (urlCopiedFrom == null) {
            return null;
        } else {
            try {
                return new SVNUrl(urlCopiedFrom);
            } catch (MalformedURLException e) {
                return null;
            }
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
            // urlCopiedFrom
            if (urlCopiedFrom == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(urlCopiedFrom);
            }

            // file
            dos.writeUTF(path);

            // conflict old
            if (pathConflictOld == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(pathConflictOld);
            }

            // conflict new
            if (pathConflictNew == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(pathConflictNew);
            }

            // conflict working
            if (pathConflictWorking == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(pathConflictWorking);
            }
            
            // lock owner
            if(lockOwner == null)
                dos.writeUTF("");
            else
                dos.writeUTF(lockOwner);
            
            // lock creation date
            dos.writeLong(lockCreationDate);
            
            // lock comment
            if (lockComment == null)
                dos.writeUTF("");
            else
                dos.writeUTF(lockComment);
            
            //read only
            dos.writeBoolean(readOnly);

        } catch (IOException e) {
            return null;
        }
        return dos;
    }

    public static LocalResourceStatus fromBytes(byte[] bytes) throws SVNException
    {
    	return ((bytes != null) && (bytes.length > 0)) ? new LocalResourceStatus(bytes) : null;
    }

    protected LocalResourceStatus() {}

    /**
     * (Re)Construct an object from the given bytes 
     * @param bytes
     * @throws SVNException
     */
    protected LocalResourceStatus(byte[] bytes) throws SVNException {
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
            // urlCopiedFrom
            String urlCopiedFromString = dis.readUTF();
            if (urlCopiedFromString.equals("")) {
                urlCopiedFrom = null;
            } else {
                urlCopiedFrom = url;
            }

            // path
            path = dis.readUTF();

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
            
            if (version >= FORMAT_VERSION_2) {
                lockOwner = dis.readUTF();
                if (lockOwner.equals(""))
                    lockOwner = null;
                lockCreationDate = dis.readLong();
                lockComment = dis.readUTF();
                if (lockComment.equals(""))
                    lockComment = null;
                readOnly = dis.readBoolean();
            } else {
                lockOwner = null;
                lockCreationDate = 0L;
                lockComment = null;
                readOnly = false;
            }
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create LocalResourceStatus from bytes", e);
        }
        return version;
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
        return org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils.hasRemote(getTextStatus());
    }

    /**
     * text is considered dirty if text status has status added, deleted,
     * replaced, modified, merged or conflicted.
     * 
     * @return
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
     * @return
     */
    public boolean isPropDirty() {
        SVNStatusKind thePropStatus = getPropStatus();
        return thePropStatus.equals(SVNStatusKind.CONFLICTED)
                || thePropStatus.equals(SVNStatusKind.MODIFIED);
    }

    /**
     * resource is considered dirty if properties are dirty or text is dirty
     * @return
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
    
    public boolean isCopied() {
        return urlCopiedFrom != null;
    }
    
    public boolean isLocked() {
        return lockOwner != null;
    }

    /**
     * the original file without your changes
     * 
     * @return
     */
    public File getFileConflictOld() {
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
    public File getFileConflictNew() {
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
    public File getFileConflictWorking() {
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
            
    public static class LocalResourceStatusNone extends LocalResourceStatus {
        static final long serialVersionUID = 1L;

    	public LocalResourceStatusNone()
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
