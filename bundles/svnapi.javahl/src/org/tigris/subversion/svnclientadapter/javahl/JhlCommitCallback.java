package org.tigris.subversion.svnclientadapter.javahl;

import java.util.Date;

import org.apache.subversion.javahl.CommitInfo;
import org.apache.subversion.javahl.callback.CommitCallback;

public class JhlCommitCallback implements CommitCallback {
	
	CommitInfo commitInfo;

	public void commitInfo(CommitInfo info) {
		commitInfo = info;
	}

    /**
     * retrieve the revision of the commit
     */
    public long getRevision() {
    	if (commitInfo == null) return 0L;
        return commitInfo.getRevision();
    }

    /**
     * return the date of the commit
     */
    public Date getDate() {
    	if (commitInfo == null) return null;
        return commitInfo.getDate();
    }

    /**
     * return the author of the commit
     */
    public String getAuthor() {
    	if (commitInfo == null) return null;
        return commitInfo.getAuthor();
    }
    
    public String getPostCommitError() {
    	if (commitInfo == null) return null;
    	return commitInfo.getPostCommitError();
    }
	
}
