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


import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.sync.ILocalSyncElement;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.LocalSyncElement;

/**
 * A <code>ILocalSyncElement</code> describes the relative synchronization of a <b>local</b> 
 * resource using a <b>base</b> resource for comparison.
 * <p>
 * Differences between the base and local resources are classified as <b>outgoing changes</b>; 
 * if there is a difference, the local resource is considered the <b>outgoing resource</b>. </p>
 */
public class SVNLocalSyncElement extends LocalSyncElement {

	protected IRemoteResource base;
	protected IResource local;

	public SVNLocalSyncElement(IResource local, IRemoteResource base) {
		this.local = local;
		this.base = base;						
	}

	/*
	 * @see RemoteSyncElement#create(IResource, IRemoteResource, IRemoteResource)
	 */
	public ILocalSyncElement create(IResource local, IRemoteResource base, Object data) {
		return new SVNLocalSyncElement(local, base);
	}

	/*
	 * @see ILocalSyncElement#getLocal()
	 */
	public IResource getLocal() {
		return local;
	}

	/*
	 * @see ILocalSyncElement#getBase()
	 */
	public IRemoteResource getBase() {		
		return base;
	}

	/*
	 * @see RemoteSyncElement#getData()
	 */
	protected Object getData() {
		return null;
	}
	

	/*
	 * @see LocalSyncElement#isIgnored(IResource)
	 */
	protected boolean isIgnored(IResource child) {
/*		ISVNLocalResource svnResource = getSVNResourceFor(getLocal());
		if(svnResource==null || !svnResource.isFolder() ) {
			return false;
		} else {
			try {
				ISVNResource managedChild = ((ISVNFolder)svnResource).getChild(child.getName());
				return managedChild.isIgnored();
			} catch(SVNException e) {
				return false;		
			}
		}*/
        return false;
	}
	

}
