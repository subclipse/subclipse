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
            if (getComparator().compare(local, getRemote())) 
                return SyncInfo.IN_SYNC;
            return SyncInfo.INCOMING | SyncInfo.CHANGE;
        }
        else if( localKind == SVNStatusKind.EXTERNAL)
            return SyncInfo.IN_SYNC;       
        
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
    
    public String toString()
    {
    	return SyncInfo.kindToString(this.getKind()) + " L: " + this.baseStatusInfo + " R: " + this.remoteStatusInfo;
    }
}
