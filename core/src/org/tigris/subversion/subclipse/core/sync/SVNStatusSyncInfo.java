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
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

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
        	if (isAddition(repositoryKind)) return SyncInfo.INCOMING | SyncInfo.ADDITION;
            if (localKind == SVNStatusKind.UNVERSIONED) return SyncInfo.IN_SYNC;
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
            return new RemoteFolder( null,
                  svnResource.getRepository(),
                  svnResource.getUrl(),
      			  revision,
      			  revision,
      			  null,
      			  null);
        }
    }
    
    public String toString()
    {
    	return "L: " + localStatusInfo.toString() + "R: " + remoteStatusInfo.toString();
    }

    protected static class StatusInfo {
    	
    	protected static final StatusInfo NONE = new StatusInfo(null, SVNStatusKind.NONE );
    	private final Number revision;
    	private final SVNStatusKind kind;
    	
    	private StatusInfo(SVNRevision.Number revision, SVNStatusKind kind) {
    		this.revision = revision;
    		this.kind = kind;
    	}
    	
    	private StatusInfo(SVNRevision.Number revision, SVNStatusKind textStatus, SVNStatusKind propStatus) {
    		this(revision, StatusInfo.mergeTextAndPropertyStatus(textStatus, propStatus));
    	}
    	
    	protected StatusInfo(LocalResourceStatus localStatus)
    	{
    		this(localStatus.getLastChangedRevision(), localStatus.getTextStatus(), localStatus.getPropStatus());	
    	}

    	protected StatusInfo(ISVNStatus svnStatus)
    	{
    		this(svnStatus.getRevision(), svnStatus.getRepositoryTextStatus(), svnStatus.getRepositoryPropStatus() );	
    	}

    	private StatusInfo(byte[] fromBytes) {
    		String[] segments = new String( fromBytes ).split(";");
    		if( segments[0].length() > 0 )
    			this.revision = new SVNRevision.Number( Long.parseLong( segments[0] ) );
    		else
    			this.revision = null;
    		this.kind = fromString( segments[1] );
    	}
    	
    	protected byte[] asBytes() {
    		return new String( ((revision != null) ? revision.toString() : "" ) + ";"+ kind).getBytes();
    	}
    	
    	protected SVNStatusKind getKind() {
    		return kind;
    	}
    	
    	protected Number getRevision() {
    		return revision;
    	}
    	
    	private static SVNStatusKind fromString(String kind) {
    		if( kind.equals( "non-svn" ) ) {
    			return SVNStatusKind.NONE;
    		}
    		if( kind.equals( "normal" ) ) {
    			return SVNStatusKind.NORMAL;
    		}
    		if( kind.equals( "added" ) ) {
    			return SVNStatusKind.ADDED;
    		}
    		if( kind.equals( "missing" ) ) {
    			return SVNStatusKind.MISSING;
    		}
    		if( kind.equals( "incomplete" ) ) {
    			return SVNStatusKind.INCOMPLETE;
    		}
    		if( kind.equals( "deleted" ) ) {
    			return SVNStatusKind.DELETED;
    		}
    		if( kind.equals( "replaced" ) ) {
    			return SVNStatusKind.REPLACED;
    		}
    		if( kind.equals( "modified" ) ) {
    			return SVNStatusKind.MODIFIED;
    		}
    		if( kind.equals( "merged" ) ) {
    			return SVNStatusKind.MERGED;
    		}
    		if( kind.equals( "conflicted" ) ) {
    			return SVNStatusKind.CONFLICTED;
    		}
    		if( kind.equals( "obstructed" ) ) {
    			return SVNStatusKind.OBSTRUCTED;
    		}
    		if( kind.equals( "ignored" ) ) {
    			return SVNStatusKind.IGNORED;
    		}
    		if( kind.equals( "external" ) ) {
    			return SVNStatusKind.EXTERNAL;
    		}
    		if( kind.equals( "unversioned" ) ) {
    			return SVNStatusKind.UNVERSIONED;
    		}
    		return SVNStatusKind.NONE;
    	}
    	
    	protected static StatusInfo fromBytes(byte[] bytes) {
    		if( bytes == null )
    			return null;
    		
    		return new StatusInfo( bytes );
    	}
    	
    	/**
    	 * Answer a 'merge' of text and property statuses.
    	 * The text has priority, i.e. the prop does not override the text status
    	 * unless it is harmless - SVNStatusKind.NORMAL
    	 * @param textStatus
    	 * @param propStatus
    	 * @return
    	 */
    	protected static SVNStatusKind mergeTextAndPropertyStatus(SVNStatusKind textStatus, SVNStatusKind propStatus)
    	{
    		if (!SVNStatusKind.NORMAL.equals(textStatus))
    		{
    			return textStatus; 
    		}
    		else
    		{
    			if (SVNStatusKind.MODIFIED.equals(propStatus) || SVNStatusKind.CONFLICTED.equals(propStatus))
    			{
    				return propStatus;
    			}
    			else
    			{
    				return textStatus;
    			}
    		}    		
    	}    
    }
}
