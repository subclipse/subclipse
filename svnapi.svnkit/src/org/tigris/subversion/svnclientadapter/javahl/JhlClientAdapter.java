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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.types.CopySource;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.SVNClient;
import org.apache.subversion.javahl.SVNRepos;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNBaseDir;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * A JavaHL base implementation of {@link org.tigris.subversion.svnclientadapter.ISVNClientAdapter}.
 *
 * @author C�dric Chabanois (cchabanois at no-log.org)
 * @author Panagiotis Korros (pkorros at bigfoot.com) 
 *
 */
public class JhlClientAdapter extends AbstractJhlClientAdapter {

    private SVNRepos svnAdmin;
    
	/**
	 * Default constructor
	 */
    public JhlClientAdapter() {
        svnClient = new SVNClient();
        svnAdmin = new SVNRepos();
        notificationHandler = new JhlNotificationHandler();
        progressListener = new JhlProgressListener();
        svnClient.notification2(notificationHandler);
        svnClient.setPrompt(new DefaultPromptUserPassword());
        svnClient.setProgressCallback(progressListener);
    }

	public boolean isThreadsafe() {
		return false;
	}

	/**
     * tells if JhlClientAdapter is usable
     * @return true if Jhl client adapter is available
     * @deprecated
     */
    public static boolean isAvailable() {
       	return JhlClientAdapterFactory.isAvailable();
    }
    
    /**
     * @return an error string describing problems during loading platform native libraries (if any)
     * @deprecated
     */
    public static String getLibraryLoadErrors() {
    	return JhlClientAdapterFactory.getLibraryLoadErrors();
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#createRepository(java.io.File)
	 */
	public void createRepository(File path, String repositoryType) throws SVNClientException {
		try {
			String fsType = (repositoryType == null) ? REPOSITORY_FSTYPE_FSFS : repositoryType; 
		    notificationHandler.setCommand(ISVNNotifyListener.Command.CREATE_REPOSITORY);
		     
		    notificationHandler.logCommandLine(
		    		MessageFormat.format(
		    				"create --fstype {0} {1}", 
							(Object[])new String[] { fsType, fileToSVNPath(path, false) }));
		    svnAdmin.create(path, false, false, null, fsType);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);            
		}        
	    
	}

	public boolean statusReturnsRemoteInfo() {
		return true;
	}
	
	/* 
	 * Overridden to fix issue where move of a file with svn:keywords updates
	 * the file contents.  Only an issue for JavaHL.
	 * 
	 * If you are moving a file and it has svn:keywords, then we change to
	 * copy, overwrite contents and delete.  This is so that file contents
	 * are not modified by the move.
	 * 
	 * For folders, and files without keywords, we just delegate to super()
	 * 
	 */
	public void move(File srcPath, File destPath, boolean force) throws SVNClientException {
		if (srcPath.isFile()) {
	        ISVNProperty prop = this.propertyGet(srcPath, ISVNProperty.KEYWORDS);
	        if (prop != null) {
	            try {
	                notificationHandler.setCommand(ISVNNotifyListener.Command.MOVE);
	    		    String src = fileToSVNPath(srcPath, false);
	                String dest = fileToSVNPath(destPath, false);
	                notificationHandler.logCommandLine(
	                        "move "+src+' '+dest);
	    			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File[] {srcPath, destPath}));        
	    			List<CopySource> copySources = new ArrayList<CopySource>();
	    			copySources.add(new CopySource(src, Revision.WORKING, Revision.WORKING));
	    			svnClient.copy(copySources, dest, true, true, true, null, null, null);
	    			try {
						overwriteFile(srcPath, destPath);
					} catch (IOException e) {
						// If file contents do not copy, just
						// proceed.
					}
	    			Set<String> paths = new HashSet<String>();
	    			paths.add(src);
	    			svnClient.remove(paths, true, false, null, null, null);
	            } catch (ClientException e) {
	                notificationHandler.logException(e);
	                throw new SVNClientException(e);
	            }                   	
		        return;
	        }
		}
    	super.move(srcPath, destPath, force);
	}

	private void overwriteFile(File srcFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(srcFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	public String getNativeLibraryVersionString() {
		return svnClient.getVersion().toString();
	}
}
