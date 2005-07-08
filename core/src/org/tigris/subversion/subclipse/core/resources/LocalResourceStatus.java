/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Date;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNStatusUtils;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

/**
 * This class has an interface which is very similar to ISVNStatus but we make
 * sure to take as little memory as possible. This class also have a getBytes()
 * method and a constructor that takes bytes.
 * 
 * @see org.tigris.subversion.svnclientadapter.ISVNStatus
 */
public class LocalResourceStatus implements Serializable {
    private static int FORMAT_VERSION_1 = 1;

    protected String url;

    protected long lastChangedRevision;

    protected long lastChangedDate;

    protected String lastCommitAuthor;

    protected int textStatus;

    protected int propStatus;

    protected long revision;

    protected int nodeKind;

    protected String urlCopiedFrom;

    protected String path; // absolute path

    protected String pathConflictOld;

    protected String pathConflictWorking;

    protected String pathConflictNew;
    
    protected String lockOwner;
    
    protected long lockCreationDate;
    
    protected String lockComment;
    
    protected boolean readOnly;
    
    static final long serialVersionUID = 1L;

    public LocalResourceStatus(ISVNStatus status) {
    	/** a temporary variable serving as immediate cache for various status values */
    	Object aValue = null;
    	
    	aValue = status.getUrl();
        if (aValue == null) {
            this.url = null;
        } else {
            this.url = ((SVNUrl) aValue).toString();
        }

        aValue = status.getLastChangedRevision();
        if (aValue == null) {
            this.lastChangedRevision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
            this.lastChangedRevision = ((SVNRevision.Number) aValue).getNumber();
        }

        aValue = status.getLastChangedDate();
        if (aValue == null) {
            this.lastChangedDate = -1;
        } else {
            this.lastChangedDate = ((Date) aValue).getTime();
        }

        this.lastCommitAuthor = status.getLastCommitAuthor();
        this.textStatus = status.getTextStatus().toInt();
        this.propStatus = status.getPropStatus().toInt();

        aValue = status.getRevision();
        if (aValue == null) {
            this.revision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
            this.revision = ((SVNRevision.Number) aValue).getNumber();
        }

        this.nodeKind = status.getNodeKind().toInt();

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

    public SVNUrl getUrl() {
        if (url == null) {
            return null;
        } else {
            try {
                return new SVNUrl(url);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    public Number getLastChangedRevision() {
        if (lastChangedRevision == SVNRevision.SVN_INVALID_REVNUM) {
            return null;
        } else {
            return new SVNRevision.Number(lastChangedRevision);
        }
    }

    public Date getLastChangedDate() {
        if (lastChangedDate == -1) {
            return null;
        } else {
            return new Date(lastChangedDate);
        }
    }

    public String getLastCommitAuthor() {
        return lastCommitAuthor;
    }

    public SVNStatusKind getTextStatus() {
        return SVNStatusKind.fromInt(textStatus);
    }

    /**
     * get the prop status. Returns either SVNStatusKind.NORMAL,
     * SVNStatusKind.CONFLICTED or SVNStatusKind.MODIFIED
     */
    public SVNStatusKind getPropStatus() {
        return SVNStatusKind.fromInt(propStatus);
    }

    public Number getRevision() {
        if (revision == SVNRevision.SVN_INVALID_REVNUM) {
            return null;
        } else {
            return new SVNRevision.Number(revision);
        }
    }

    public SVNNodeKind getNodeKind() {
        return SVNNodeKind.fromInt(nodeKind);
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

    /**
     * @return Returns the file.
     */
    public File getFile() {
        return new File(path);
    }

    /**
     * @return Returns the absolute resource path.
     * (It is absolute since it was constructed as status.getFile().getAbsolutePath())
     */
    public IPath getPath() {
        return new Path(path);
    }

    public byte[] getBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        try {
            dos.writeInt(FORMAT_VERSION_1);

            // url
            if (url == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(url);
            }

            // lastChangedRevision
            dos.writeLong(lastChangedRevision);

            // lastChangedDate
            dos.writeLong(lastChangedDate);

            // lastCommitAuthor
            if (lastCommitAuthor == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(lastCommitAuthor);
            }

            // textStatus
            dos.writeInt(textStatus);

            // propStatus
            dos.writeInt(propStatus);

            // revision
            dos.writeLong(revision);

            // nodeKind
            dos.writeInt(nodeKind);

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

        } catch (IOException e) {
            return null;
        }
        return out.toByteArray();
    }

    public static LocalResourceStatus fromBytes(byte[] bytes) throws SVNException
    {
    	return ((bytes != null) && (bytes.length > 0)) ? new LocalResourceStatus(bytes) : null;
    }
    
    private LocalResourceStatus(byte[] bytes) throws SVNException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(in);
        try {
            if (dis.readInt() != FORMAT_VERSION_1) {
                throw new SVNException("Invalid format");
            }

            // url
            String urlString = dis.readUTF();
            if (urlString.equals("")) {
                url = null;
            } else {
                url = urlString;
            }

            // lastChangedRevision
            lastChangedRevision = dis.readLong();

            // lastChangedDate
            lastChangedDate = dis.readLong();

            // lastCommitAuthor
            String lastCommitAuthorString = dis.readUTF();
            if ((url == null) && (lastCommitAuthorString.equals(""))) {
                lastCommitAuthor = null;
            } else {
                lastCommitAuthor = lastCommitAuthorString;
            }

            // textStatus
            textStatus = dis.readInt();

            // propStatus
            propStatus = dis.readInt();

            // revision
            revision = dis.readLong();

            // nodeKind
            nodeKind = dis.readInt();

            // urlCopiedFrom
            String urlCopiedFromString = dis.readUTF();
            if (urlCopiedFromString.equals("")) {
                urlCopiedFrom = null;
            } else {
                urlCopiedFrom = urlString;
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
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create LocalResourceStatus from bytes", e);
        }
    }

    /**
     * Returns if is managed by svn (added, normal, modified ...)
     * 
     * @return if managed by svn
     */
    public boolean isManaged() {
        return SVNStatusUtils.isManaged(getTextStatus());
    }

    /**
     * Returns if the resource has a remote counter-part
     * 
     * @return has version in repository
     */
    public boolean hasRemote() {
        return SVNStatusUtils.hasRemote(getTextStatus());
    }

    /**
     * text is considered dirty if text status has status added, deleted,
     * replaced, modified, merged or conflicted.
     * 
     * @return
     */
    public boolean isTextDirty() {
        SVNStatusKind textStatus = getTextStatus();

        return ((textStatus.equals(SVNStatusKind.ADDED))
                || (textStatus.equals(SVNStatusKind.DELETED))
                || (textStatus.equals(SVNStatusKind.REPLACED))
                || (textStatus.equals(SVNStatusKind.MODIFIED))
                || (textStatus.equals(SVNStatusKind.MERGED)) || (textStatus
                .equals(SVNStatusKind.CONFLICTED)));
    }

    /**
     * prop is considered dirty if prop status is either conflicted or modified
     * 
     * @return
     */
    public boolean isPropDirty() {
        SVNStatusKind propStatus = getPropStatus();
        return propStatus.equals(SVNStatusKind.CONFLICTED)
                || propStatus.equals(SVNStatusKind.MODIFIED);
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

    public String toString()
    {
    	return ((path != null) ? path : "") + " (" + getRevision().toString() + ") " + getTextStatus().toString();
    }
}
