/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion  
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.history;

 
import java.util.Date;

import org.eclipse.core.runtime.PlatformObject;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;

public class LogEntry extends PlatformObject implements ILogEntry {

	private RemoteResource resource; // the corresponding remote resource
	private String author;
	private Date date;
	private String comment;

	public LogEntry(RemoteResource resource, long revision, String author, Date date, String comment) {
		
        if (resource.isFolder()) {
            this.resource = new RemoteFolder(null,resource.getRepository(), resource.getUrl(), resource.getHasProps(),
                revision, date, author);
        } 
        else
        {
            this.resource = new RemoteFile(null,resource.getRepository(), resource.getUrl(), resource.getHasProps(),
                                       revision, date, author);  
        }

		this.author = author;
		this.date = date;
		this.comment = comment;
	}
	
	/**
	 * @see ILogEntry#getRevision()
	 */
	public long getRevision() {
		return resource.getRevision();
	}

	/**
	 * @see ILogEntry#getAuthor()
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @see ILogEntry#getDate()
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @see ILogEntry#getComment()
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @see ILogEntry#getRemoteFile()
	 */
	public ISVNRemoteResource getRemoteResource() {
		return resource;
	}
	

}

