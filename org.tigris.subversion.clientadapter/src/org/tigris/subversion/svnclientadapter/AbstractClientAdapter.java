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
package org.tigris.subversion.svnclientadapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Default implementation of some of the methods of ISVNClientAdapter
 * 
 * @author Cédric Chabanois (cchabanois at no-log.org)
 * @author Panagiotis Korros (pkorros at bigfoot.com)   
 */
public abstract class AbstractClientAdapter implements ISVNClientAdapter {

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#setKeywords(java.io.File, org.tigris.subversion.svnclientadapter.SVNKeywords, boolean)
     */
    public void setKeywords(File path, SVNKeywords keywords, boolean recurse) throws SVNClientException {
        propertySet(path, ISVNProperty.KEYWORDS, keywords.toString(), recurse);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addKeywords(java.io.File, org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public SVNKeywords addKeywords(File path, SVNKeywords keywords) throws SVNClientException {
        SVNKeywords currentKeywords = getKeywords(path);
        if (keywords.isHeadUrl())
            currentKeywords.setHeadUrl(true);
        if (keywords.isId())
            currentKeywords.setId(true);
        if (keywords.isLastChangedBy())
            currentKeywords.setLastChangedBy(true);
        if (keywords.isLastChangedDate())
            currentKeywords.setLastChangedBy(true);
        if (keywords.isLastChangedRevision())
            currentKeywords.setLastChangedRevision(true);
        setKeywords(path,currentKeywords,false);
        
        return currentKeywords;                
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#removeKeywords(java.io.File, org.tigris.subversion.svnclientadapter.SVNKeywords)
     */
    public SVNKeywords removeKeywords(File path, SVNKeywords keywords) throws SVNClientException {
        SVNKeywords currentKeywords = getKeywords(path);
        if (keywords.isHeadUrl())
            currentKeywords.setHeadUrl(false);
        if (keywords.isId())
            currentKeywords.setId(false);
        if (keywords.isLastChangedBy())
            currentKeywords.setLastChangedBy(false);
        if (keywords.isLastChangedDate())
            currentKeywords.setLastChangedBy(false);
        if (keywords.isLastChangedRevision())
            currentKeywords.setLastChangedRevision(false);
        setKeywords(path,currentKeywords,false);
        
        return currentKeywords;                
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getIgnoredPatterns(java.io.File)
     */
    public List getIgnoredPatterns(File path) throws SVNClientException {
        if (!path.isDirectory())
            return null;
        List list = new ArrayList();
        ISVNProperty pd = propertyGet(path, ISVNProperty.IGNORE);
        if (pd == null)
            return list;
        String patterns = pd.getValue();
        StringTokenizer st = new StringTokenizer(patterns,"\n\r");
        while (st.hasMoreTokens()) {
            String entry = st.nextToken();
            if ((entry != null) && (entry.length() > 0)) {
                list.add(entry);
            }
        }
        return list;
    }
    
    /* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.client.ISVNClientAdapter#getLogMessages(java.io.File, org.tigris.subversion.subclipse.client.ISVNRevision, org.tigris.subversion.subclipse.client.ISVNRevision)
	 */
	public ISVNLogMessage[] getLogMessages(File arg0, SVNRevision arg1, SVNRevision arg2)
		throws SVNClientException {
		return getLogMessages(arg0, arg1, arg2, true);
	}
    
    /* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.client.ISVNClientAdapter#getLogMessages(java.net.URL, org.tigris.subversion.subclipse.client.ISVNRevision, org.tigris.subversion.subclipse.client.ISVNRevision)
	 */
	public ISVNLogMessage[] getLogMessages(SVNUrl arg0, SVNRevision arg1, SVNRevision arg2)
		throws SVNClientException {
		return getLogMessages(arg0, arg1, arg2, true);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getLogMessages(org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String[], org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean)
	 */
	public ISVNLogMessage[] getLogMessages(final SVNUrl url,
			final String[] paths, SVNRevision revStart, SVNRevision revEnd,
			boolean stopOnCopy, boolean fetchChangePath)
			throws SVNClientException {
        return this.getLogMessages(url, SVNRevision.HEAD, revStart, revEnd, stopOnCopy, fetchChangePath, 0, false);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getLogMessages(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public ISVNLogMessage[] getLogMessages(File path,
			SVNRevision revisionStart, SVNRevision revisionEnd,
			boolean fetchChangePath) throws SVNClientException {
		return this.getLogMessages(path, SVNRevision.HEAD, revisionStart, revisionEnd, false,
				fetchChangePath, 0, false);
	}    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getLogMessages(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean)
     */
    public ISVNLogMessage[] getLogMessages(File path,
			SVNRevision revisionStart, SVNRevision revisionEnd,
			boolean stopOnCopy, boolean fetchChangePath)
			throws SVNClientException {
		return this.getLogMessages(path, SVNRevision.HEAD, revisionStart, revisionEnd,
				stopOnCopy, fetchChangePath, 0, false);
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getLogMessages(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean, long)
     */
    public ISVNLogMessage[] getLogMessages(File path,
			SVNRevision revisionStart, SVNRevision revisionEnd,
			boolean stopOnCopy, boolean fetchChangePath, long limit)
			throws SVNClientException {
		//If the file is an uncommitted rename/move, we have to refer to original/source, not the new copy.
		ISVNInfo info = getInfoFromWorkingCopy(path);
		if ((SVNScheduleKind.ADD == info.getSchedule()) && (info.getCopyUrl() != null)) {
			return this.getLogMessages(info.getCopyUrl(), SVNRevision.HEAD, revisionStart, revisionEnd,
					stopOnCopy, fetchChangePath, limit, false);
		} else {
			return this.getLogMessages(path, SVNRevision.HEAD, revisionStart, revisionEnd,
				stopOnCopy, fetchChangePath, limit, false);
		}
	}
           
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getLogMessages(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public ISVNLogMessage[] getLogMessages(SVNUrl url,
			SVNRevision revisionStart, SVNRevision revisionEnd,
			boolean fetchChangePath) throws SVNClientException {
		return this.getLogMessages(url, SVNRevision.HEAD, revisionStart, revisionEnd, false,
				fetchChangePath, 0, false);
	} 

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getLogMessages(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean, long)
     */
    public ISVNLogMessage[] getLogMessages(SVNUrl url, SVNRevision pegRevision,
            SVNRevision revisionStart, SVNRevision revisionEnd,
            boolean stopOnCopy, boolean fetchChangePath, long limit)
            throws SVNClientException {
	        return this.getLogMessages(url, pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath, limit, false);
    }

    public ISVNLogMessage[] getLogMessages(File path, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, boolean fetchChangePath, long limit, boolean includeMergedRevisions) throws SVNClientException {
		SVNLogMessageCallback worker = new SVNLogMessageCallback();
		this.getLogMessages(path, pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath, limit, includeMergedRevisions, ISVNClientAdapter.DEFAULT_LOG_PROPERTIES, worker);
		return worker.getLogMessages();
	}

	public ISVNLogMessage[] getLogMessages(SVNUrl url, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, boolean fetchChangePath, long limit, boolean includeMergedRevisions) throws SVNClientException {
		SVNLogMessageCallback worker = new SVNLogMessageCallback();
        this.getLogMessages(url, pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath, limit, includeMergedRevisions, ISVNClientAdapter.DEFAULT_LOG_PROPERTIES, worker);
		return worker.getLogMessages();
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#setIgnoredPatterns(java.io.File, java.util.List)
     */
    public void setIgnoredPatterns(File path, List patterns) throws SVNClientException {
        if (!path.isDirectory())
            return;
        String separator = System.getProperty("line.separator");
        StringBuffer value = new StringBuffer();
        for (Iterator it = patterns.iterator(); it.hasNext();) {
            String pattern = (String)it.next();
            value.append(pattern + separator);
        }
        propertySet(path, ISVNProperty.IGNORE, value.toString(), false);
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addToIgnoredPatterns(java.io.File, java.lang.String)
     */
    public void addToIgnoredPatterns(File path, String pattern)  throws SVNClientException {
        List patterns = getIgnoredPatterns(path);
        if (patterns == null) // not a directory
            return;
 
        // verify that the pattern has not already been added
        for (Iterator it = patterns.iterator(); it.hasNext();) {
            if (((String)it.next()).equals(pattern))
                return; // already added
        }
            
        patterns.add(pattern);
        setIgnoredPatterns(path,patterns);
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getKeywords(java.io.File)
     */
    public SVNKeywords getKeywords(File path) throws SVNClientException {
        ISVNProperty prop = propertyGet(path, ISVNProperty.KEYWORDS);
        if (prop == null)
            return new SVNKeywords(); 

        // value is a space-delimited list of the keywords names
        String value = prop.getValue();
        
        return new SVNKeywords(value);
    }    
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addPasswordCallback(org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword)
     */
    public void addPasswordCallback(ISVNPromptUserPassword callback) {
        // Default implementation does nothing
    }
 
    public boolean statusReturnsRemoteInfo() {
         return false;
    }
    public long[] commitAcrossWC(File[] paths, String message, boolean recurse,
            boolean keepLocks, boolean Atomic) throws SVNClientException {
        notImplementedYet();
        return null;
    }
    
    protected void notImplementedYet() throws SVNClientException {
        throw new SVNClientException("Not implemented yet");
    }

    public boolean canCommitAcrossWC() {
        return false;
    }
    
    public void mkdir(SVNUrl url, boolean makeParents, String message)
            throws SVNClientException {
        if (makeParents) {
            SVNUrl parent = url.getParent();
            if (parent != null) {
		        ISVNInfo info = null;
		        try {
		            info = this.getInfo(parent);
		        } catch (SVNClientException e) {
		        }
		        if (info == null)
		            this.mkdir(parent, makeParents, message);
            }
        }
        this.mkdir(url, message);
    }
    

	/**
	 * Answer whether running on Windows OS.
	 * (Actual code extracted from org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS)
	 * (For such one simple method it does make sense to introduce dependency on whole commons-lang.jar)
	 * @return true when the underlying 
	 */
	public static boolean isOsWindows()
	{
        try {
            return System.getProperty("os.name").startsWith("Windows");
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            return false;
        }
	}

	public ISVNInfo getInfo(SVNUrl url) throws SVNClientException {
		return getInfo(url, SVNRevision.HEAD, SVNRevision.HEAD);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#merge(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, boolean, boolean)
	 */
	public void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2, SVNRevision revision2, File localPath, boolean force, boolean recurse, boolean dryRun) throws SVNClientException {
		merge(path1, revision1, path2, revision2, localPath, force, recurse, dryRun, false);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#merge(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, boolean)
	 */
	public void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2, SVNRevision revision2, File localPath, boolean force, boolean recurse) throws SVNClientException {
		merge(path1, revision1, path2, revision2, localPath, force, recurse, false, false);
	}
	

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertyGet(org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String)
	 */
	public ISVNProperty propertyGet(SVNUrl url, String propertyName)
		throws SVNClientException {
		return propertyGet(url, SVNRevision.HEAD, SVNRevision.HEAD, propertyName);
	}
	
	public void diff(File[] paths, File outFile, boolean recurse) throws SVNClientException {
		FileOutputStream os = null;
		try {
			ArrayList tempFiles = new ArrayList();
			for (int i = 0; i < paths.length; i++) {
				File tempFile = File.createTempFile("tempDiff", ".txt");
				tempFile.deleteOnExit();
				diff(paths[i], tempFile, recurse);
				tempFiles.add(tempFile);
			}
			os = new FileOutputStream(outFile);
			Iterator iter = tempFiles.iterator();
			while (iter.hasNext()) {
				File tempFile = (File)iter.next();
				FileInputStream is = new FileInputStream(tempFile);
				byte[] buffer = new byte[4096];
				int bytes_read;
				while ((bytes_read = is.read(buffer)) != -1)
					os.write(buffer, 0, bytes_read);				
				is.close();
			}
		} catch (Exception e) {
			throw new SVNClientException(e);
		} finally {
			if (os != null) try {os.close();} catch (IOException e) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#createPatch(java.io.File[], java.io.File, java.io.File, boolean)
	 */
	public void createPatch(File[] paths, File relativeToPath, File outFile,
			boolean recurse) throws SVNClientException {
		File tmpFile;
		try {
			tmpFile = File.createTempFile("svn","patch");
	        tmpFile.deleteOnExit();
		} catch (IOException e) {
			throw new SVNClientException(e);
		} 
		this.diff(paths, tmpFile, recurse);
		stripPathsFromPatch(tmpFile, outFile, relativeToPath);
	}

	/**
	 * Takes svn diff output and processes it to remove absolute paths and
	 * convert them to relative paths which makes it easier to apply patch
	 * 
	 * @param tmpFile - disk file containing diff output
	 * @param outFile - file to store the updated diff output
	 * @param relativeToPath - path to make file references relative to or null
	 *             for absolute paths
	 * @throws SVNClientException 
	 */
	private void stripPathsFromPatch(File tmpFile, File outFile, File relativeToPath) throws SVNClientException {
		String relativeStr = null;
		if (relativeToPath != null) {
			try {
				if (relativeToPath.isDirectory())
					relativeStr = relativeToPath.getCanonicalPath();
				else
					relativeStr = relativeToPath.getParentFile().getCanonicalPath();
			} catch (IOException e1) {
				if (relativeToPath.isDirectory())
					relativeStr = relativeToPath.getAbsolutePath();
				else
					relativeStr = relativeToPath.getParentFile().getAbsolutePath();
			}
			relativeStr += "/";
			relativeStr = relativeStr.replace('\\', '/');
		}
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(tmpFile);
			fos = new FileOutputStream(outFile);
			byte b[] = new byte[fis.available()];
			fis.read(b);
			if (relativeToPath != null) {
				byte o[] = new String(b).replaceAll(relativeStr, "").getBytes();
				fos.write(o);
			} else {
				fos.write(b);
			}
		} catch (FileNotFoundException e) {
			throw new SVNClientException(e);
		} catch (IOException e) {
			throw new SVNClientException(e);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
			}
			try {
				if (fos != null)
					fos.close();		
			} catch (IOException e) {
			}
		}
 	}	

}
