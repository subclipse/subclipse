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
package org.tigris.subversion.subclipse.core.sync;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.sync.ILocalSyncElement;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.core.sync.RemoteSyncElement;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

/**
 * A <code>IRemoteSyncElement</code> describes the relative synchronization of a <b>local</b> 
 * and <b>remote</b> resource using a <b>base</b> resource for comparison.
 * <p>
 * Differences between the base and remote resources are classified as <b>incoming changes</b>; 
 * if there is a difference, the remote resource is considered the <b>incoming resource</b>. </p>
 */
public class SVNRemoteSyncElement extends RemoteSyncElement {

	SVNLocalSyncElement localSync;
	IRemoteResource remote;
	boolean isThreeWay = true;

	public SVNRemoteSyncElement(boolean isThreeWay, IResource local, IRemoteResource base, IRemoteResource remote) {
		localSync = new SVNLocalSyncElement(local, base);
		this.remote = remote;	
		this.isThreeWay = isThreeWay;		
	}

	/*
	 * @see RemoteSyncElement#create(IResource, IRemoteResource, IRemoteResource)
	 */
	public IRemoteSyncElement create(boolean isThreeWay, IResource local, IRemoteResource base, IRemoteResource remote, Object data) {
		return new SVNRemoteSyncElement(isThreeWay, local, base, remote);
	}

	/*
	 * @see IRemoteSyncElement#getRemote()
	 */
	public IRemoteResource getRemote() {
		return remote;
	}

	/*
	 * @see LocalSyncElement#getData()
	 */
	protected Object getData() {
		return localSync.getData();
	}

	/*
	 * @see ILocalSyncElement#getLocal()
	 */
	public IResource getLocal() {
		return localSync.getLocal();
	}

	/*
	 * @see ILocalSyncElement#getBase()
	 */
	public IRemoteResource getBase() {
		return localSync.getBase();
	}


	/*
	 * @see LocalSyncElement#create(IResource, IRemoteResource, Object)
	 */
	public ILocalSyncElement create(IResource local, IRemoteResource base, Object data) {
		return localSync.create(local, base, data);
	}
	/*
	 * @see LocalSyncElement#isIgnored(IResource)
	 */
	protected boolean isIgnored(IResource resource) {
		return localSync.isIgnored(resource);
	}
	/*
	 * @see IRemoteSyncElement#ignoreBaseTree()
	 */
	public boolean isThreeWay() {
		return isThreeWay;
	}
	
	/**
	 * @see RemoteSyncElement#timestampEquals(IRemoteResource, IRemoteResource)
	 */
	protected boolean timestampEquals(IRemoteResource e1, IRemoteResource e2) {
		if(e1.isContainer()) {
			if(e2.isContainer()) {
				return true;
			}
			return false;
		}
		return e1.equals(e2);
	}

	/**
	 * @see RemoteSyncElement#timestampEquals(IResource, IRemoteResource)
	 */
	protected boolean timestampEquals(IResource e1, IRemoteResource e2) {
		if(e1.getType() != IResource.FILE) {
			if(e2.isContainer()) {
				return true;
			}
			return false;
		}
		ISVNLocalFile svnFile = SVNWorkspaceRoot.getSVNFileFor((IFile)e1);
        ISVNRemoteResource svnRemoteResource = (ISVNRemoteResource)e2;
        
        try{
            ISVNStatus status = svnFile.getStatus();
            if (status != null) {
                if (status.isDeleted() || status.isMerged() || status.isModified())
                    return false;
                
                return svnRemoteResource.getLastChangedRevision().equals(status.getLastChangedRevision());
            }
        
            return false;
        }catch(SVNException e) {
            SVNProviderPlugin.log(e.getStatus());
            return false;
        }        
	}
}
