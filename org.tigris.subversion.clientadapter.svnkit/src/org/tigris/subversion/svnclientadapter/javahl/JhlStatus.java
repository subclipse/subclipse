/*******************************************************************************
 * Copyright (c) 2003, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter.javahl;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Date;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ConflictDescriptor;
import org.apache.subversion.javahl.ISVNClient;
import org.apache.subversion.javahl.callback.InfoCallback;
import org.apache.subversion.javahl.types.Depth;
import org.apache.subversion.javahl.types.Info;
import org.apache.subversion.javahl.types.Status;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * A JavaHL based implementation of {@link ISVNStatus}.
 * Actually just an adapter from {@link org.tigris.subversion.javahl.Status}
 *  
 * @author philip schatz
 */
public class JhlStatus implements ISVNStatus {

	protected Status _s;
	private SVNRevision.Number lastChangedRevision;
	private String lastChangedAuthor;
	private Date lastChangedDate;
	
	private boolean treeConflict = false;
	private ConflictDescriptor conflictDescriptor;
	private String conflictOld;
	private String conflictWorking;
	private String conflictNew;

	/**
	 * Constructor
	 * @param status
	 */
	public JhlStatus(Status status, ISVNClient client) {
		// note that status.textStatus must be different than 0 (the resource must exist)
        super();
		_s = status;
		
		// This is a workaround for an SVNKit bug that results in _s.isConflicted == false for an old format
		// working copy, even if the file is text conflicted.
		boolean textConflicted = _s.getTextStatus() != null && _s.getTextStatus().equals(Status.Kind.conflicted);
		
		try {
			if (client != null && (_s.isConflicted() || textConflicted))
				populateInfo(client, _s.getPath());
		} catch (ClientException e) {
			// Ignore
		}
	}

	private void populateInfo(ISVNClient aClient, String path)
			throws ClientException {
		
		class MyInfoCallback implements InfoCallback {
			Info info;

			public void singleInfo(Info aInfo) {
				info = aInfo;
			}

			public Info getInfo() {
				return info;
			}
		}

		MyInfoCallback callback = new MyInfoCallback();

		aClient.info2(path, null, null, Depth.empty, null,
				callback);

		Info aInfo = callback.getInfo();
		if (aInfo == null)
			return;
		
		if (aInfo.getConflicts() != null) {
			for (ConflictDescriptor conflict : aInfo.getConflicts()) {
				switch (conflict.getKind()) {
				case tree:
					this.treeConflict = true;
					this.conflictDescriptor = conflict;
					break;
	
				case text:
					this.conflictOld = conflict.getBasePath();
					this.conflictWorking = conflict.getMyPath();
					this.conflictNew = conflict.getTheirPath();
					break;
	
				case property:
					// Ignore
					break;
				}
			}
		}
	}
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getUrl()
	 */
	public SVNUrl getUrl() {
		try {
            return (_s.getUrl() != null) ? new SVNUrl(_s.getUrl()) : null;
        } catch (MalformedURLException e) {
            //should never happen.
            return null;
        }
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getUrlString()
	 */
	public String getUrlString()
	{
		return _s.getUrl();
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastChangedRevision()
	 */
	public SVNRevision.Number getLastChangedRevision() {
        // we don't use 
        // return (SVNRevision.Number)JhlConverter.convert(_s.getLastChangedRevision());
        // as _s.getLastChangedRevision() is currently broken if revision is -1 
		if (lastChangedRevision != null)
			return lastChangedRevision;
		if (_s.getReposLastCmtAuthor() == null)
			return JhlConverter.convertRevisionNumber(_s.getLastChangedRevisionNumber());
		else
			if (_s.getReposLastCmtRevisionNumber() == 0)
				return null;
			return JhlConverter.convertRevisionNumber(_s.getReposLastCmtRevisionNumber());
	}
	
	public SVNRevision.Number getReposLastChangedRevision() {
		return JhlConverter.convertRevisionNumber(_s.getReposLastCmtRevisionNumber());
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastChangedDate()
	 */
	public Date getLastChangedDate() {
		if (lastChangedDate != null)
			return lastChangedDate;
		if (_s.getReposLastCmtAuthor() == null)
			return _s.getLastChangedDate();
		else
			return _s.getReposLastCmtDate();
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastCommitAuthor()
	 */
	public String getLastCommitAuthor() {
		if (lastChangedAuthor != null)
			return lastChangedAuthor;
		if (_s.getReposLastCmtAuthor() == null)
			return _s.getLastCommitAuthor();
		else
			return _s.getReposLastCmtAuthor();
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getTextStatus()
	 */
	public SVNStatusKind getTextStatus() {
        return JhlConverter.convertStatusKind(_s.getTextStatus());
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getPropStatus()
	 */
	public SVNStatusKind getPropStatus() {
		return JhlConverter.convertStatusKind(_s.getPropStatus());
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRevision()
	 */
	public SVNRevision.Number getRevision() {
		return JhlConverter.convertRevisionNumber(_s.getRevisionNumber());
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#isCopied()
	 */
	public boolean isCopied() {
		return _s.isCopied();
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#isWcLocked()
	 */
	public boolean isWcLocked() {
		return _s.isLocked();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#isSwitched()
	 */
	public boolean isSwitched() {
		return _s.isSwitched();
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getPath()
	 */
	public String getPath() {
		return _s.getPath();
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getMovedFromAbspath()
	 */
	public String getMovedFromAbspath() {
		return _s.getMovedFromAbspath();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getMovedToAbspath()
	 */
	public String getMovedToAbspath() {
		return _s.getMovedToAbspath();
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getFile()
	 */
    public File getFile() {
        return new File(getPath()).getAbsoluteFile();
    }

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getNodeKind()
	 */
	public SVNNodeKind getNodeKind() {
		SVNNodeKind nodeKind;
		if (_s.getReposLastCmtAuthor() == null)
			nodeKind = JhlConverter.convertNodeKind(_s.getNodeKind());
		else
			nodeKind = JhlConverter.convertNodeKind(_s.getReposKind());
        return nodeKind;
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRepositoryTextStatus()
     */
    public SVNStatusKind getRepositoryTextStatus() {
        return JhlConverter.convertStatusKind(_s.getRepositoryTextStatus());
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRepositoryPropStatus()
     */
    public SVNStatusKind getRepositoryPropStatus() {
        return JhlConverter.convertStatusKind(_s.getRepositoryPropStatus());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getPath() + " "+getTextStatus().toString();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictNew()
     */
    public File getConflictNew() {
		String path = conflictNew;
		return (path != null) ? new File(path)
		.getAbsoluteFile() : null;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictOld()
	 */
    public File getConflictOld() {
		String path = conflictOld;
		return (path != null) ? new File(path)
		.getAbsoluteFile() : null;
	}

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictWorking()
	 */
    public File getConflictWorking() {
		String path = conflictWorking;
		return (path != null) ? new File(path)
		.getAbsoluteFile() : null;
	}
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLockCreationDate()
	 */
    public Date getLockCreationDate() {
    	if (_s.getLocalLock() == null)
    		return null;
        return _s.getLocalLock().getCreationDate();
    }
 
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLockOwner()
     */
    public String getLockOwner() {
    	if (_s.getLocalLock() == null)
    		return null;
        return _s.getLocalLock().getOwner();
    }
 
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLockComment()
     */
    public String getLockComment() {
    	if (_s.getLocalLock() == null)
    		return null;
        return _s.getLocalLock().getComment();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getTreeConflicted()
     */
	public boolean hasTreeConflict() {
		return treeConflict;
	}
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#isFileExternal()
     */
	public boolean isFileExternal() {
		return _s.isFileExternal();
	}	

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getConflictDescriptor()
     */
	public SVNConflictDescriptor getConflictDescriptor() {
		return JhlConverter.convertConflictDescriptor(conflictDescriptor);
	}
    
   
    public void updateFromStatus(JhlStatus info) {
    	lastChangedRevision = info.getLastChangedRevision();
    	lastChangedAuthor = info.getLastCommitAuthor();
    	lastChangedDate = info.getLastChangedDate();
    }
    
    /**
     * A special JhlStatus subclass representing svn:external resource.
     */
    public static class JhlStatusExternal extends JhlStatus
    {

    	/**
    	 * Constructor
    	 * @param status
    	 */
    	public JhlStatusExternal(JhlStatus status, ISVNClient client) {
            super(status._s, client);
    	}

    	public SVNStatusKind getTextStatus() {
            return SVNStatusKind.EXTERNAL;
    	}    	
    	
    }

}
