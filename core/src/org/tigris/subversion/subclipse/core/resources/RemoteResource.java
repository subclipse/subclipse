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
package org.tigris.subversion.subclipse.core.resources;

import java.net.URL;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

import com.qintsoft.jsvn.jni.ClientException;
import com.qintsoft.jsvn.jni.LogMessage;
import com.qintsoft.jsvn.jni.Revision;

/**
 * The purpose of this class and its subclasses is to implement the corresponding
 * ISVNRemoteResource interfaces for the purpose of communicating information about 
 * resources that reside in a SVN repository but have not necessarily been loaded
 * locally.
 */
public abstract class RemoteResource
	extends PlatformObject
	implements ISVNRemoteResource {

	protected RemoteFolder parent;
	// null when this is the repository location 
	protected URL url;
	protected ISVNRepositoryLocation repository;
	protected boolean hasProps;
	private long revision;
	private Date date;
	private String author;

	/**
	 * Constructor for RemoteResource.
	 */
	public RemoteResource(
		RemoteFolder parent,
		ISVNRepositoryLocation repository,
		URL url,
		boolean hasProps,
		long revision,
		Date date,
		String author) {

		this.parent = parent;
		this.repository = repository;
		this.url = url;
		this.hasProps = hasProps;
		this.revision = revision;
		this.date = date;
		this.author = author;
	}

    /**
     * this constructor is used for the folder corresponding to repository location
     */
    public RemoteResource(ISVNRepositoryLocation repository, URL url) {
        this.parent = null;
        this.repository = repository;
        this.url = url;
        this.hasProps = false;
        this.revision = -1;
        this.date = null;
        this.author = null;
    }


	/*
	 * @see ICVSRemoteResource#getName()
	 */
	public String getName() {
		return Util.getLastSegment(url.toString());
	}

    /**
     * get the path of this remote resource relatively to the repository
     */
    public String getRepositoryRelativePath() {
        return getUrl().toString().substring(getRepository().getUrl().toString().length());
    }    
	
    /*
	 * @see ISVNRemoteResource#exists(IProgressMonitor)
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException {
		return parent.exists(this, monitor);
	}
	
	/*
	 * @see ISVNRemoteResource#getParent()
	 */
	public ISVNRemoteFolder getParent() {
		return parent;
	}
	
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteResource))
			return false;
		RemoteResource remote = (RemoteResource) target;
		return remote.isContainer() == isContainer() && remote.getUrl().equals(getUrl());
	}


	public ISVNRepositoryLocation getRepository() {
		return repository;
	}

    /**
     * get the url of this remote resource
     */
    public URL getUrl() {
        return url;
    }

    /**
     * tells if this remote resource has properties
     */
    public boolean getHasProps() {
        return hasProps;
    }

    /**
     * get the revision
     */
	public long getRevision() {
		return revision;
	}

    /**
     * get the date 
     */
	public Date getDate() {
		return date;
	}

    /**
     * get the author
     */
	public String getAuthor() {
		return author;
	}

    /**
     * @see ISVNRemoteResource#getLogEntries()
     */
    public LogEntry[] getLogEntries(IProgressMonitor monitor) throws SVNException {
        SVNClientAdapter client = getRepository().getSVNClient();
        monitor = Policy.monitorFor(monitor);
        monitor.beginTask(Policy.bind("RemoteFile.getLogEntries"), 100); //$NON-NLS-1$
        
        LogMessage[] logMessages;
		try {
			logMessages =
				client.getLogMessages(
					getUrl(),
					new Revision.Number(0),
					Revision.HEAD);
		} catch (ClientException e) {
            throw SVNException.wrapException(e);
		}
        
        LogEntry[] logEntries = new LogEntry[logMessages.length];
        
        for (int i = 0; i < logMessages.length;i++) {
            logEntries[i] = new LogEntry(this, 
                              logMessages[i].getRevision(), 
                              logMessages[i].getAuthor(),
                              logMessages[i].getDate(),
                              logMessages[i].getMessage());
        }
             
        monitor.done();
        return logEntries;
    }

}
