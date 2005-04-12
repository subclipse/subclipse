/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.sync.notused;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

/**
 * Describes the synchronization of a <b>local</b> resource 
 * relative to a <b>remote</b> resource variant. 
 */
public class SVNSyncInfo extends SyncInfo {

	/**
	 * @param local
	 * @param base
	 * @param remote
	 * @param comparator
	 * @todo Generated comment
	 */
	public SVNSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, IResourceVariantComparator comparator) {
		super(local, base, remote, comparator);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.synchronize.SyncInfo#calculateKind()
	 */
//	protected int calculateKind() throws TeamException {
//
//		RemoteResource remote = (RemoteResource) getRemote();
//		RemoteResource base = (RemoteResource)getBase();
//		ISVNLocalResource local = SVNWorkspaceRoot.getSVNResourceFor(getLocal());
//		boolean remoteExists = (remote != null);
//		//TODO: make 3 way
//		//diff between base & local
//		//diff between base & remote
//		//diff between base & both
//		if (!remoteExists && local.exists()) {
//			return ADDITION;
//		} else if (local.getStatus().getTextStatus() == ISVNStatus.Kind.MODIFIED) {
//			return CHANGE;
//		} else if (!local.exists() && remoteExists) {
//			return DELETION;
//		} else if (!getComparator().compare(getLocal(), getRemote())) {
//			return CHANGE;
//		} else {
//			return IN_SYNC;
//		}
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.synchronize.SyncInfo#getLocalContentIdentifier()
	 */
	public String getLocalContentIdentifier() {
		// return a string could be displayed to the user to identify this resource
        // we return the revision
        IResource local = getLocal();
		try {
			if (local != null) {
                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(local);
                return svnResource.getStatus().getRevision().toString();
			}
		} catch (SVNException e) {
			SVNProviderPlugin.log(e);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		return getLocal().getName() + " " + getLocalContentIdentifier();
	}

}