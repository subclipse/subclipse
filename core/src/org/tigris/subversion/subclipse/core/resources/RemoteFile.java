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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

/**
 * This class provides the implementation of ISVNRemoteFile and IManagedFile for
 * use by the repository and sync view.
 */
public class RemoteFile extends RemoteResource implements ISVNRemoteFile  {

    // buffer for file contents received from the server
    private byte[] contents;


    public RemoteFile(RemoteFolder parent, 
                      ISVNRepositoryLocation repository,
                      URL url,
                      Revision revision,
                      boolean hasProps,
                      Revision.Number lastChangedRevision,
                      Date date,
                      String author)
	{
		super(parent,repository,url,revision,hasProps,lastChangedRevision,date,author);
	}

    public RemoteFile(ISVNRepositoryLocation repository, URL url, Revision revision) {
        super(repository, url, revision);
    }

	/**
	 * @see ISVNRemoteFile#getContents()
	 */
	public InputStream getContents(IProgressMonitor monitor) throws SVNException {
        // we cache the contents as getContents can be called several times
        // on the same RemoteFile
        monitor.beginTask(Policy.bind("RemoteFile.getContents"), 100);//$NON-NLS-1$
        try
        {
            if (contents == null)
            {
                SVNClientAdapter svnClient = repository.getSVNClient();
                InputStream inputStream;
                try {
                    inputStream = svnClient.getContent(url, getLastChangedRevision());
                    contents = new byte[inputStream.available()];
                    inputStream.read(contents);
                } catch (IOException e) {
		        } catch (ClientException e) {
                    throw SVNException.wrapException(e);
		        }
                monitor.done();
            }
            return new ByteArrayInputStream(contents);
        } finally {
            monitor.done();
        }
	}

	/*
	 * @see IRemoteResource#members(IProgressMonitor)
	 */
	public IRemoteResource[] members(IProgressMonitor progress) throws TeamException {
		return new IRemoteResource[0];
	}

	/*
	 * @see IRemoteResource#isContainer()
	 */
	public boolean isContainer() {
		return false;
	}

	/*
	 * @see ISVNResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}


	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteFile))
			return false;
		RemoteFile remote = (RemoteFile) target;
		return super.equals(target) && remote.getLastChangedRevision() == getLastChangedRevision();
	}
}
