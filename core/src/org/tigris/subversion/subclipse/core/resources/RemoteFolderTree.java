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


import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

/**
 * Whereas the RemoteFolder class provides access to a remote hierarchy using
 * lazy retrieval via <code>getMembers()</code>, the RemoteFolderTree will force 
 * a recursive retrieval of the remote hierarchy in one round trip.
 */
public class RemoteFolderTree extends RemoteFolder  {
    private static final ISVNRemoteResource[] EMPTY = new ISVNRemoteResource[] {};
    /**
     * @param resource
     * @param bytes
     */
    public RemoteFolderTree(IResource resource, byte[] bytes) {
        super(resource, bytes);
        this.children = EMPTY;
    }
    /**
     * @param repository
     * @param url
     * @param revision
     */
    public RemoteFolderTree(ISVNRepositoryLocation repository, SVNUrl url,
            SVNRevision revision) {
        super(repository, url, revision);
        this.children = EMPTY;
    }
    /**
     * @param parent
     * @param repository
     * @param url
     * @param revision
     * @param lastChangedRevision
     * @param date
     * @param author
     */
    public RemoteFolderTree(RemoteFolder parent,
            ISVNRepositoryLocation repository, SVNUrl url,
            SVNRevision revision, Number lastChangedRevision, Date date,
            String author) {
        super(parent, repository, url, revision, lastChangedRevision, date,
                author);
        this.children = EMPTY;
    }

	/* 
	 * This method is public to allow access by the RemoteFolderTreeBuilder utility class.
	 * No other external classes should use this method.
	 */
	public void setChildren(ISVNRemoteResource[] children) {
		this.children = children;
	}
}

