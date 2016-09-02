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
package org.tigris.subversion.subclipse.core.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;

/**
 * @author mml
 */
public class SVNRevisionComparator implements IResourceVariantComparator {

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariantComparator#compare(org.eclipse.core.resources.IResource, org.eclipse.team.core.variants.IResourceVariant)
	 */
	public boolean compare(IResource local, IResourceVariant remote) {
		if( local == null && remote == null )
		    return true;
		if( local == null || remote == null )
		    return false;
		ISVNLocalResource a = SVNWorkspaceRoot.getSVNResourceFor(local);
		ISVNRemoteResource b = (ISVNRemoteResource)remote;
		try {
			return a.getStatus().getLastChangedRevision().getNumber() == b.getLastChangedRevision().getNumber();
		} catch (SVNException e) {
            Util.logError("Cannot compare local resource with remote resource",e);
		} catch(NullPointerException npe) {
			// When svn:externals are used several of the above methods can return null
			// We have already checked for the important/expected nulls
			return true;
		}
		return false;
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariantComparator#compare(org.eclipse.team.core.variants.IResourceVariant, org.eclipse.team.core.variants.IResourceVariant)
	 */
	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		if( base == remote )
		    return true;
		if( base == null || remote == null )
		    return false;
		ISVNRemoteResource a = (ISVNRemoteResource)base;
		ISVNRemoteResource b = (ISVNRemoteResource)remote;
		try {
			return a.getLastChangedRevision().getNumber()==b.getLastChangedRevision().getNumber();
		} catch(NullPointerException npe) {
			// When svn:externals are used several of the above methods can return null
			// We have already checked for the important/expected nulls
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariantComparator#isThreeWay()
	 */
	public boolean isThreeWay() {
		return true;
	}

}
