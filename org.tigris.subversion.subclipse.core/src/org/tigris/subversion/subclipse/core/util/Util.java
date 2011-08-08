/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.util;


import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URLDecoder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Unsorted static helper-methods 
 */
public class Util {
	public static final String CURRENT_LOCAL_FOLDER = "."; //$NON-NLS-1$
	public static final String SERVER_SEPARATOR = "/"; //$NON-NLS-1$
	
	/**
	 * Return the last segment of the given path
	 * <br>
	 * Do not abuse this unnecesarily !
	 * When there is a SVNUrl instance available use direct
	 * {@link SVNUrl#getLastPathSegment()}
	 * @param path
	 * @return String
	 */
	public static String getLastSegment(String path) {
		int index = path.lastIndexOf(SERVER_SEPARATOR);
		if (index == -1)
			return path;
		else
			return path.substring(index + 1);
		
	}
	
	/**
	 * Append the prefix and suffix to form a valid SVN path.
	 * <br>
	 * Do not abuse this unnecesarily !
	 * When there is a SVNUrl instance available use direct
	 * {@link SVNUrl#appendPath(java.lang.String)}
	 */
	public static String appendPath(String prefix, String suffix) {
		if (prefix.length() == 0 || prefix.equals(CURRENT_LOCAL_FOLDER)) {
			return suffix;
		} else if (prefix.endsWith(SERVER_SEPARATOR)) {
			if (suffix.startsWith(SERVER_SEPARATOR))
				return prefix + suffix.substring(1);
			else
				return prefix + suffix;
		} else if (suffix.startsWith(SERVER_SEPARATOR))
			return prefix + suffix;
		else
			return prefix + SERVER_SEPARATOR + suffix;
	}

	public static void logError(String message, Throwable throwable) {
		SVNProviderPlugin.log(new Status(IStatus.ERROR, SVNProviderPlugin.ID, IStatus.ERROR, message, throwable));
	}
	
	/**
	 * Get the url string of the parent resource
	 * @param svnResource
	 * @return parent's url, null if none of parents has an url
	 * @throws SVNException
	 */
	public static String getParentUrl(ISVNLocalResource svnResource) throws SVNException {
        ISVNLocalFolder parent = svnResource.getParent();
        while (parent != null) {
            String url = parent.getStatus().getUrlString();
            if (url != null) return url;
            parent = parent.getParent();
        }
        return null;
    }

	public static String flattenText(String string) {
		StringBuffer buffer = new StringBuffer(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(SERVER_SEPARATOR); 
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}

	/**
	 * unescape UTF8/URL encoded strings
	 * 
	 * @param s
	 * @return
	 */
	public static String unescape(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (Exception e) {
			return s;
		}
	}
	
	/**
	 * Save local history
	 * 
	 * @param resource
	 * @throws CoreException
	 */
	public static void saveLocalHistory(IResource resource) throws CoreException {
		if (resource instanceof IFile && resource.exists()) {
			if (!resource.isSynchronized(IResource.DEPTH_ZERO))
				resource.refreshLocal(IResource.DEPTH_ZERO, null);
			((IFile)resource).appendContents(new ByteArrayInputStream(new byte[0]),IResource.KEEP_HISTORY, null);
		}
	}
	
	public static boolean isSpecialEclipseFile(IResource resource) {
		return resource.getName().equals(".project") || resource.getName().equals(".classpath");
	}
	
	public static SVNUrl getUrlForRevision(ISVNRemoteResource resource, SVNRevision.Number revision, IProgressMonitor pm) throws SVNException {
		SVNUrl url = resource.getUrl();
		SVNRevision revisionStart = new SVNRevision.Number(revision.getNumber());
		GetLogsCommand getLogsCommand = new GetLogsCommand(resource, SVNRevision.HEAD, revisionStart, SVNRevision.HEAD, false, 0, null, true);
		getLogsCommand.run(pm);
		ILogEntry[] logEntries = getLogsCommand.getLogEntries();
		String path = resource.getRepositoryRelativePath();
		for (int i = logEntries.length - 1; i > -1 ; i--) {
			ILogEntry logEntry = logEntries[i];
			if (!logEntry.getRevision().equals(revision)) {
				LogEntryChangePath[] changePaths = logEntry.getLogEntryChangePaths();
				for (LogEntryChangePath changePath : changePaths) {	
					if (changePath.getPath().equals(path) && changePath.getCopySrcPath() != null) {
						try {
							path = changePath.getCopySrcPath();
							url = new SVNUrl(resource.getRepository().getRepositoryRoot().toString() + changePath.getCopySrcPath());
						} catch (MalformedURLException e) {}
					}
				}
			}
		}
		return url;
	}
}
