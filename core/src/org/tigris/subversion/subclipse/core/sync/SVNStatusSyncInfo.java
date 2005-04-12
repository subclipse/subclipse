/*
 * Created on 20 Ιουλ 2004
 */
package org.tigris.subversion.subclipse.core.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Panagiotis K
 */
public class SVNStatusSyncInfo extends SyncInfo {
    private final StatusInfo localStatusInfo;
    private final StatusInfo remoteStatusInfo;

    public SVNStatusSyncInfo(IResource local,
            				 StatusInfo localStatusInfo,
            				 StatusInfo remoteStatusInfo,
            				 IResourceVariantComparator comparator) {
        super(local,
              createBaseResourceVariant( local, localStatusInfo, remoteStatusInfo ),
              createLatestResourceVariant( local, localStatusInfo, remoteStatusInfo),
              comparator);
        this.localStatusInfo = localStatusInfo;
        this.remoteStatusInfo = remoteStatusInfo;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.SyncInfo#calculateKind()
     */    
    protected int calculateKind() throws TeamException {
        SVNStatusKind localKind = localStatusInfo.getKind();
        SVNStatusKind repositoryKind = SVNStatusKind.NORMAL;
        if( remoteStatusInfo != null)
            repositoryKind = remoteStatusInfo.getKind();
        IResource local = getLocal();
        
        if (!local.exists()) {
            if (localKind == SVNStatusKind.UNVERSIONED) return SyncInfo.IN_SYNC;
            if (isDeletion(repositoryKind)) return SyncInfo.IN_SYNC;
            if (isDeletion(localKind)) {
                if (isChange(repositoryKind)) return SyncInfo.CONFLICTING | SyncInfo.DELETION;
                return SyncInfo.OUTGOING | SyncInfo.DELETION;
            } else return SyncInfo.INCOMING | SyncInfo.ADDITION;
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

    private static IResourceVariant createBaseResourceVariant(IResource local, StatusInfo localStatusInfo, StatusInfo remoteStatusInfo) {
        if( localStatusInfo == null
                || localStatusInfo.getRevision() == null )
          return null;
        return createResourceVariant(local, localStatusInfo.getRevision());
    }
    private static IResourceVariant createLatestResourceVariant(IResource local, StatusInfo localStatusInfo, StatusInfo remoteStatusInfo) {
        if( remoteStatusInfo == null
                || remoteStatusInfo.getKind() == SVNStatusKind.DELETED )
            return null;
        if( remoteStatusInfo.getKind() == SVNStatusKind.NONE && 
            localStatusInfo != null && isAddition(localStatusInfo.getKind()) ) {
            return null;
        }
        return createResourceVariant(local, remoteStatusInfo.getRevision());
    }

    private static IResourceVariant createResourceVariant(IResource local, SVNRevision.Number revision) {
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor( local );
        if( local.getType() == IResource.FILE ) {
            return new RemoteFile( null, 
                  svnResource.getRepository(),
                  svnResource.getUrl(),
    			  revision,
    			  revision,
    			  null,
    			  null);
        }
        else {
            return new RemoteFile( null,
                  svnResource.getRepository(),
                  svnResource.getUrl(),
      			  revision,
      			  revision,
      			  null,
      			  null);
        }
    }
}
