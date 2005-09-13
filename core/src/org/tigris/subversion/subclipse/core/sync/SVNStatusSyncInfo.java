/*
 * Created on 20 Ιουλ 2004
 */
package org.tigris.subversion.subclipse.core.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
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
    		SVNRevision rev = remoteStatusInfo.getRevision();
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
		return baseStatusInfo.getRevision().toString();
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.SyncInfo#calculateKind()
     */    
    protected int calculateKind() throws TeamException {
        SVNStatusKind localKind = baseStatusInfo.getStatusKind();
        SVNStatusKind repositoryKind = (remoteStatusInfo != null)
            								? remoteStatusInfo.getStatusKind() : SVNStatusKind.NORMAL;
        IResource local = getLocal();
        
        if (!local.exists()) {
        	if (isAddition(repositoryKind)) return SyncInfo.INCOMING | SyncInfo.ADDITION;
            if (localKind == SVNStatusKind.UNVERSIONED || localKind == SVNStatusKind.NONE) return SyncInfo.IN_SYNC;
            if (isDeletion(repositoryKind)) return SyncInfo.IN_SYNC;
            if (isDeletion(localKind)) {
                if (isChange(repositoryKind)) return SyncInfo.CONFLICTING | SyncInfo.DELETION;
                return SyncInfo.OUTGOING | SyncInfo.DELETION;
            } else return SyncInfo.INCOMING | SyncInfo.ADDITION;
        }
        //this makes sense for directories only - they still exists when they are being deleted
        else if ( isDeletion(localKind))
        {
        	if ((IResource.FOLDER == local.getType()) 
        			&& (isNotModified(repositoryKind))) return SyncInfo.OUTGOING | SyncInfo.DELETION;
        }
        else if( isChange(localKind) ) {
            if( isChange( repositoryKind )
             || isAddition( repositoryKind ) 
             || isDeletion( repositoryKind ))
                return SyncInfo.CONFLICTING | SyncInfo.CHANGE;
            else
                return SyncInfo.OUTGOING | SyncInfo.CHANGE;
        }
        else if( isAddition( localKind ) ) {
            if( isAddition( repositoryKind ) )
                return SyncInfo.CONFLICTING | SyncInfo.ADDITION;
            return SyncInfo.OUTGOING | SyncInfo.ADDITION;
        }
        else if( isNotModified(localKind) ) {
            if( isNotModified( repositoryKind) )
                return SyncInfo.IN_SYNC;
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
        
        return super.calculateKind();
    }
    
    private boolean isDeletion(SVNStatusKind kind) {
        return kind == SVNStatusKind.DELETED;
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
            return new BaseFile(baseStatusInfo);
        }
        else {
            return new BaseFolder(baseStatusInfo);
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
		if ((getRemote() != null) && 
				((SyncInfo.INCOMING == SyncInfo.getDirection(getKind())) ||
				(SyncInfo.CONFLICTING == SyncInfo.getDirection(getKind()))))
		{
			return " (" + getRemote().getContentIdentifier() + ")" ;
		}
		else
		{
			return "";
		}
    }
    
    public String toString()
    {
    	return SyncInfo.kindToString(this.getKind()) + " L: " + this.baseStatusInfo + " R: " + this.remoteStatusInfo;
    }
}
