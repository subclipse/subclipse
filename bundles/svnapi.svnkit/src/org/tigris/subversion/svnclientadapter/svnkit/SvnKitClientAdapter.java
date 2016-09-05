/*******************************************************************************
 * Copyright (c) 2005, 2006 svnClientAdapter project and others.
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
package org.tigris.subversion.svnclientadapter.svnkit;

import java.io.File;

import org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNStatusUnversioned;
import org.tigris.subversion.svnclientadapter.javahl.AbstractJhlClientAdapter;
import org.tigris.subversion.svnclientadapter.javahl.JhlNotificationHandler;
import org.tigris.subversion.svnclientadapter.javahl.JhlProgressListener;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.javahl17.SVNClientImpl;



/**
 * The SVNKit Adapter works by providing an implementation of the
 * JavaHL SVNClientInterface.  This allows to provide a common
 * JavaHL implementation (AbstractJhlClientAdapter) where the specific
 * adapters just need to initialize the correct underlying classes.
 *
 */
public class SvnKitClientAdapter extends AbstractJhlClientAdapter {

    public SvnKitClientAdapter() {
        svnClient = SVNClientImpl.newInstance();
        notificationHandler = new JhlNotificationHandler();
        progressListener = new JhlProgressListener();
        svnClient.notification2(notificationHandler);        
        svnClient.setPrompt(new DefaultPromptUserPassword());
        svnClient.setProgressCallback(progressListener);
    }

	public boolean isThreadsafe() {
		return false;
	}

    public void createRepository(File path, String repositoryType)
            throws SVNClientException {
    	if (REPOSITORY_FSTYPE_BDB.equalsIgnoreCase(repositoryType))
    		throw new SVNClientException("SVNKit only supports fsfs repository type.");
    	try {
    		boolean force = false;
    		boolean enableRevisionProperties = false;
			SVNRepositoryFactory.createLocalRepository(path, enableRevisionProperties, force);
		} catch (SVNException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
		}
     }
    
 
    public void addPasswordCallback(ISVNPromptUserPassword callback) {
        if (callback != null) {
	        SvnKitPromptUserPassword prompt = new SvnKitPromptUserPassword(callback);
	        this.setPromptUserPassword(prompt);
        }
    }
    
    public boolean statusReturnsRemoteInfo() {
        return true;
    }
    
    
    public boolean canCommitAcrossWC() {
    	// Native SVN now supports this with normal commit method, so support for
    	// the special method has been removed.
        return false;
    }

    /**
     * Returns the status of files and directory recursively.
     * Overrides method from parent class to work around SVNKit bug when status on resource within ignored folder
     * does not yield any status. 
     *
     * @param path File to gather status.
     * @param descend get recursive status information
     * @param getAll get status information for all files
     * @param contactServer contact server to get remote changes
     * @param ignoreExternals
     *  
     * @return a Status
     * @throws SVNClientException
     */
    public ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals) throws SVNClientException {
    	//Call the standard status first.
    	ISVNStatus[] statuses = super.getStatus(path, descend, getAll, contactServer, ignoreExternals);
    	//If status call return empty array it is either correct - the getAll was not specified and there's not
    	//interesting status in WC, or it is the bug on getting status on unversioned with ignored.
    	if (statuses.length == 0) {
    		if (getAll) {
    			//If the getAll was called and it returned nothing, it is probably the bug case
    			return new ISVNStatus[] { new SVNStatusUnversioned(path) };    			
    		} else {
    			//If the getAll was not called, we have to find out, so let's call it again with getAll set.
    			ISVNStatus[] reCheckStatuses = super.getStatus(path, false, true, false, true);
    			if (reCheckStatuses.length == 0) {
        			//If event after getAll the result is empty, we assume it's the bug.
    				return new ISVNStatus[] { new SVNStatusUnversioned(path) };
    			} else {
    				//The result after getAll was not empty, so the very first empty result was OK, there's nothing interesting in WC.
    				return new ISVNStatus[0];
    			}
    		}
    	} else {
    		return statuses;
    	}
    }
   
}
