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

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.BaseFile;
import org.tigris.subversion.subclipse.core.resources.BaseFolder;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

/**
 * @author Panagiotis K
 */
public class SVNStatusSyncInfo extends SyncInfo {
    private final LocalResourceStatus baseStatusInfo;
    private final RemoteResourceStatus remoteStatusInfo;

    public SVNStatusSyncInfo(IResource local,
    						 LocalResourceStatus baseStatusInfo,
    						 RemoteResourceStatus remoteStatusInfo,
            				 IResourceVariantComparator comparator) {
        super(local,
              createBaseResourceVariant( local, baseStatusInfo),
              createLatestResourceVariant( local, baseStatusInfo, remoteStatusInfo),
              comparator);
        this.baseStatusInfo = (baseStatusInfo != null) ? baseStatusInfo : LocalResourceStatus.NONE;
        this.remoteStatusInfo = remoteStatusInfo;
    }

    /**
     * Get the repository revision (from the remoteStatus).
     * If we can't get the revision from the remote status, return HEAD.
     * @return
     */
    public SVNRevision getRepositoryRevision()
    {
    	if (remoteStatusInfo != null)
    	{
    		SVNRevision rev = remoteStatusInfo.getRepositoryRevision();
    		if ((rev != null) && !SVNRevision.INVALID_REVISION.equals(rev))
    		{
    			return rev;
    		}
    		else
    		{
    			return SVNRevision.HEAD;
    		}
    	}
    	else
    	{
    		return SVNRevision.HEAD;
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.SyncInfo#getRemote()
	 */
	public IResourceVariant getRemote() {
//		 If we want to avoid the unnecessary roundtrip for fetching the contents of remote file in case of outgoing changes,
//		 (when we actually need/want to compare with base only), we should trick the eclipse somehow.
		//So what we do now is answer the remote only in case of incoming change or conflict.
		IResourceVariant theRemote = super.getRemote();
		if ((theRemote != null) && 
				((SyncInfo.INCOMING == SyncInfo.getDirection(getKind())) ||
				(SyncInfo.CONFLICTING == SyncInfo.getDirection(getKind()))))
		{
			return theRemote;
		}
		else
		{
			return (super.getBase() != null) ? super.getBase() : theRemote;
		}
	}

//	/* (non-Javadoc)
//	 * @see org.eclipse.team.core.synchronize.SyncInfo#getRemote()
//	 */
//	public IResourceVariant getBase() {
//		//TODO This should probably go away when the JavaHL will be fixed.
//		//There is a bug in JavaHL (1.2.2) which does not correctly translates the line endings.
//		//To avoid displaying the change as confilct, we will answer base only when really necessary
//		//So what we do now is answer the base only in case of outgoing change or conflict.
//		IResourceVariant theBase = super.getBase();
//		if ((theBase != null) && 
//				((SyncInfo.OUTGOING == SyncInfo.getDirection(getKind())) ||
//				(SyncInfo.CONFLICTING == SyncInfo.getDirection(getKind()))))
//		{
//			return theBase;
//		}
//		else
//		{
//			return (super.getRemote() != null) ? super.getRemote() : theBase;
//		}
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.SyncInfo#getLocalContentIdentifier()
	 */
	public String getLocalContentIdentifier() {
		return (baseStatusInfo.getLastChangedRevision() != null) ? baseStatusInfo.getLastChangedRevision().toString() : null;
	}
	
	/**
	 * Returns the remote status information of this SyncInfo
	 */
	public RemoteResourceStatus getRemoteResourceStatus() {
		return remoteStatusInfo;
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.SyncInfo#calculateKind()
     */    
    protected int calculateKind() throws TeamException {
        SVNStatusKind localKind = baseStatusInfo.getStatusKind();
        SVNStatusKind repositoryKind = (remoteStatusInfo != null)
            								? remoteStatusInfo.getStatusKind() : SVNStatusKind.NORMAL;
        IResource local = getLocal();
        
        if (local.getParent() != null && !local.getParent().exists()) {
        	return SyncInfo.IN_SYNC;
        }
        
        // If resource is ignored through Eclipse project's resource filters property, IResource.exists() returns false,
        // even if the file/folder exists in the file system.  So we need to check for the existence in the file system
        // so that these items aren't incorrectly shown as outgoing deletions.

        if (!local.exists() && !(local.getLocation() == null || local.getLocation().toFile().exists())) {
        	if (isAddition(repositoryKind)) return SyncInfo.INCOMING | SyncInfo.ADDITION;
            if (localKind == SVNStatusKind.UNVERSIONED) return SyncInfo.IN_SYNC;
            if (isDeletion(repositoryKind)) return SyncInfo.IN_SYNC;
            if (!repositoryKind.equals(SVNStatusKind.ADDED)) {        	
            	if (localKind == SVNStatusKind.NONE) {
            		return SyncInfo.IN_SYNC;
            	}
            	
                if (isChange(repositoryKind)) return SyncInfo.CONFLICTING | SyncInfo.DELETION;
                return SyncInfo.OUTGOING | SyncInfo.DELETION;
            } else return SyncInfo.INCOMING | SyncInfo.ADDITION;
        }

        else if ( isDeletion(localKind))
        {
    		if (isNotModified(repositoryKind)) {
    			if (isOutOfDate())
    				return SyncInfo.CONFLICTING | SyncInfo.DELETION;
    			else
    				return SyncInfo.OUTGOING | SyncInfo.DELETION;
    		} else
    			return SyncInfo.CONFLICTING | SyncInfo.DELETION;
        }
        else if( isChange(localKind) ) {
            if( isChange( repositoryKind )
             || isAddition( repositoryKind ) 
             || isDeletion( repositoryKind ))
                return SyncInfo.CONFLICTING | SyncInfo.CHANGE;
            else {
            	if ((IResource.FOLDER == local.getType() || IResource.PROJECT == local.getType()) && isOutOfDate())
            		return SyncInfo.CONFLICTING | SyncInfo.CHANGE;
            	else
            		return SyncInfo.OUTGOING | SyncInfo.CHANGE;
            }
        }
        else if( isAddition( localKind ) ) {
            if( isAddition( repositoryKind ) )
                return SyncInfo.CONFLICTING | SyncInfo.ADDITION;
            return SyncInfo.OUTGOING | SyncInfo.ADDITION;
        }
        else if( isNotModified(localKind) ) {
            if( isNotModified( repositoryKind) ) {
            	if ((IResource.FOLDER == local.getType() || IResource.PROJECT == local.getType()) && isOutOfDate())
            		return SyncInfo.INCOMING | SyncInfo.CHANGE;
                return SyncInfo.IN_SYNC;
            }
            if ((localKind == SVNStatusKind.IGNORED) && (repositoryKind == SVNStatusKind.ADDED))
	                return SyncInfo.CONFLICTING | SyncInfo.ADDITION;
            if( repositoryKind == SVNStatusKind.DELETED )
                return SyncInfo.INCOMING | SyncInfo.DELETION;
            if( repositoryKind == SVNStatusKind.ADDED )
                return SyncInfo.INCOMING | SyncInfo.ADDITION;
            if( repositoryKind == SVNStatusKind.EXTERNAL)
                return SyncInfo.IN_SYNC;
//TODO Is this really necessary here ?
//            if (getComparator().compare(getBase(), getRemote())) 
//                return SyncInfo.IN_SYNC;
            return SyncInfo.INCOMING | SyncInfo.CHANGE;
        }
        else if( repositoryKind == SVNStatusKind.EXTERNAL ) {
            if (localKind == SVNStatusKind.EXTERNAL)
            	return SyncInfo.IN_SYNC;
        }
        else if ((localKind == SVNStatusKind.EXTERNAL) && (remoteStatusInfo == null))
        {
        	return SyncInfo.IN_SYNC;
        }
        
        return super.calculateKind();
    }
    
    private boolean isOutOfDate() {
    	if (remoteStatusInfo == null || baseStatusInfo == null)
    		return false;
    	if (remoteStatusInfo.getLastChangedRevision() == null || baseStatusInfo.getLastChangedRevision() == null)
    		return false;
    	if (remoteStatusInfo.getLastChangedRevision().getNumber() > baseStatusInfo.getLastChangedRevision().getNumber())
    		return true;
    	else
    		return false;
    }
    
    private boolean isDeletion(SVNStatusKind kind) {
        return kind == SVNStatusKind.DELETED
			 || kind == SVNStatusKind.MISSING;
    }

    private boolean isChange(SVNStatusKind kind) {
        return kind == SVNStatusKind.MODIFIED 
              || kind == SVNStatusKind.REPLACED
              || kind == SVNStatusKind.OBSTRUCTED
              || kind == SVNStatusKind.CONFLICTED
              || kind == SVNStatusKind.MERGED;
    }
    private boolean isNotModified(SVNStatusKind kind) {
        return kind == SVNStatusKind.NORMAL
              || kind == SVNStatusKind.IGNORED 
              || kind == SVNStatusKind.NONE;
    }
    private static boolean isAddition(SVNStatusKind kind) {
        return kind == SVNStatusKind.ADDED || kind == SVNStatusKind.UNVERSIONED;
    }

    private static IResourceVariant createBaseResourceVariant(IResource local, LocalResourceStatus baseStatusInfo) {
        if( baseStatusInfo == null
                || baseStatusInfo.getLastChangedRevision() == null )
          return null;
        
        if( local.getType() == IResource.FILE ) {
        	String charset = null;
        	try {
        		charset = ((IEncodedStorage)local).getCharset();
        	} catch (CoreException e) {
        		SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
        	}
        	return new BaseFile(local, baseStatusInfo, charset);
        }
        else {
            return new BaseFolder(local, baseStatusInfo);
        }
    }
    
    private static IResourceVariant createLatestResourceVariant(IResource local, LocalResourceStatus baseStatusInfo, RemoteResourceStatus remoteStatusInfo) {
        if( remoteStatusInfo == null
                || remoteStatusInfo.getStatusKind() == SVNStatusKind.DELETED )
            return null;
        if( remoteStatusInfo.getStatusKind() == SVNStatusKind.NONE && 
            baseStatusInfo != null && isAddition(baseStatusInfo.getStatusKind()) ) {
            return null;
        }

        if( local.getType() == IResource.FILE ) {
            return new RemoteFile(remoteStatusInfo);
        }
        else {
            return new RemoteFolder(remoteStatusInfo);
        }
    }
    
    /**
     * Asnwer label describing revision status.
     * (E.g. the one displayed byt the resource in the synchronize view). 
     * @return
     */
    public String getLabel()
    {
    	IResourceVariant remote = getRemote();
		if ((remote != null) && 
				((SyncInfo.INCOMING == SyncInfo.getDirection(getKind())) ||
				(SyncInfo.CONFLICTING == SyncInfo.getDirection(getKind()))))
		{
			if (remote instanceof ISVNRemoteResource) {
				ISVNRemoteResource remoteResource = (ISVNRemoteResource)remote;
				if (remoteResource.getAuthor() != null) {
					return " (" + remote.getContentIdentifier() + " - " + remoteResource.getAuthor() + ")" ;
				}
			}
			return " (" + remote.getContentIdentifier() + ")" ;
		}
		else
		{
			if (baseStatusInfo != null && baseStatusInfo.getMovedFromAbspath() != null) {
				return " (" + Policy.bind("SVNStatusSyncInfo.movedFrom") + baseStatusInfo.getMovedFromAbspath().substring(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString().length()) + ")";
			}
			else if (baseStatusInfo != null && baseStatusInfo.getMovedToAbspath() != null) {
				return " (" + Policy.bind("SVNStatusSyncInfo.movedTo") + baseStatusInfo.getMovedToAbspath().substring(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString().length()) + ")";
			}
			return "";
		}
    }
    
    public String toString()
    {
    	return SyncInfo.kindToString(this.getKind()) + " L: " + this.baseStatusInfo + " R: " + this.remoteStatusInfo;
    }
}
