/*******************************************************************************
 * Copyright (c) 2004, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter.javahl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.subversion.javahl.types.ChangePath;
import org.apache.subversion.javahl.ConflictDescriptor;
import org.apache.subversion.javahl.ConflictResult;
import org.apache.subversion.javahl.DiffSummary;
import org.apache.subversion.javahl.ISVNClient;
import org.apache.subversion.javahl.types.DirEntry;
import org.apache.subversion.javahl.types.Info;
import org.apache.subversion.javahl.types.Lock;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.types.RevisionRange;
import org.apache.subversion.javahl.types.Status;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNConflictResult;
import org.tigris.subversion.svnclientadapter.SVNConflictVersion;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNScheduleKind;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.utils.Depth;

/**
 * Convert from javahl types to subversion.svnclientadapter.* types 
 *  
 * @author philip schatz
 */
public class JhlConverter {

	private static final Logger log = Logger.getLogger(JhlConverter.class.getName());	
	
	private JhlConverter() {
		//non-instantiable
	}
	
	/**
	 * Convert clientAdapter's {@link SVNRevision} into JavaHL's {@link Revision}
	 * @param svnRevision
	 * @return a {@link Revision} representing suppplied SVNRevision
	 */
    public static Revision convert(SVNRevision svnRevision) {
    	if (svnRevision == null)
    		return null;
        switch(svnRevision.getKind()) {
            case SVNRevision.Kind.base : return Revision.BASE;
            case SVNRevision.Kind.committed : return Revision.COMMITTED;
            case SVNRevision.Kind.date : return new Revision.DateSpec(((SVNRevision.DateSpec)svnRevision).getDate());
            case SVNRevision.Kind.head : return Revision.HEAD;
            case SVNRevision.Kind.number : return new Revision.Number(((SVNRevision.Number)svnRevision).getNumber());
            case SVNRevision.Kind.previous : return Revision.PREVIOUS;
            case SVNRevision.Kind.unspecified : return Revision.START;
            case SVNRevision.Kind.working : return Revision.WORKING;
            default: {
        		log.severe("unknown revision kind :"+svnRevision.getKind());
            	return Revision.START; // should never go here
            }
        }
    }
    
	/**
	 * Convert clientAdapter's {@link SVNRevisionRange} into JavaHL's {@link RevisionRange}
	 * @param svnRevisionRange
	 * @return a {@link RevisionRange} representing suppplied SVNRevisionRange
	 */
    public static RevisionRange convert(SVNRevisionRange svnRevisionRange) {
    	return new RevisionRange(JhlConverter.convert(svnRevisionRange.getFromRevision()), JhlConverter.convert(svnRevisionRange.getToRevision()));
    }

    /**
	 * Convert JavaHL's {@link RevisionRange} into clientAdapter's {@link SVNRevisionRange}
	 * @param RevisionRange
	 * @return a {@link SVNRevisionRange} representing suppplied RevisionRange
	 */
    public static SVNRevisionRange convert(RevisionRange svnRevisionRange) {
    	return new SVNRevisionRange(JhlConverter.convert(svnRevisionRange.getFromRevision()), JhlConverter.convert(svnRevisionRange.getToRevision()));
    }

    public static SVNRevisionRange[] convertRevisionRange(List<RevisionRange> jhlRange) {
        SVNRevisionRange[] range = new SVNRevisionRange[jhlRange.size()];
        int i=0;
        for (RevisionRange item : jhlRange) {
			range[i] = JhlConverter.convert(item);
			i++;
		}
        return range;
	}
    
    public static  List<RevisionRange> convert(SVNRevisionRange[] range) {
        List<RevisionRange> jhlRange = new ArrayList<RevisionRange>(range.length);
        for(int i=0; i < range.length; i++) {
            jhlRange.add(JhlConverter.convert(range[i]));
        }
        return jhlRange;
    }

	/**
	 * Convert JavaHL's {@link Revision} into clientAdapter's {@link SVNRevision} 
	 * @param rev
	 * @return a {@link SVNRevision} representing suppplied Revision
	 */
	public static SVNRevision convert(Revision rev) {
		if (rev == null) return null;
		switch (rev.getKind()) {
			case base :
				return SVNRevision.BASE;
			case committed :
				return SVNRevision.COMMITTED;
			case number :
				Revision.Number n = (Revision.Number) rev;
				if (n.getNumber() == -1) {
					// we return null when resource is not managed ...
					return null;
				} else {
					return new SVNRevision.Number(n.getNumber());
				}
			case previous :
				return SVNRevision.PREVIOUS;
			case working :
				return SVNRevision.WORKING;
			default :
				return SVNRevision.HEAD;
		}
	}
    
    static SVNRevision.Number convertRevisionNumber(long revisionNumber) {
    	if (revisionNumber == -1) {
    		return null;
        } else {
        	return new SVNRevision.Number(revisionNumber); 
        }
    }

    public static SVNNodeKind convertNodeKind(org.apache.subversion.javahl.types.NodeKind javahlNodeKind) {
    	if (javahlNodeKind == null) {
    		return null;
    	}
        switch(javahlNodeKind) {
            case dir  : return SVNNodeKind.DIR; 
            case file : return SVNNodeKind.FILE; 
            case none : return SVNNodeKind.NONE; 
            case unknown : return SVNNodeKind.UNKNOWN;
            default: {
            	log.severe("unknown node kind :"+javahlNodeKind);
            	return SVNNodeKind.UNKNOWN; // should never go here
            }
        }
    }

	public static JhlStatus convert(Status status, ISVNClient client) {
		return new JhlStatus(status, client);
	}

    public static SVNStatusKind convertStatusKind(Status.Kind kind) {
    	if (kind == null) {
    		return null;
    	}
        switch (kind) {
            case none :
                return SVNStatusKind.NONE;
            case normal :
                return SVNStatusKind.NORMAL;                
            case added :
                return SVNStatusKind.ADDED;
            case missing :
                return SVNStatusKind.MISSING;
            case incomplete :
                return SVNStatusKind.INCOMPLETE;
            case deleted :
                return SVNStatusKind.DELETED;
            case replaced :
                return SVNStatusKind.REPLACED;                                                
            case modified :
                return SVNStatusKind.MODIFIED;
            case merged :
                return SVNStatusKind.MERGED;                
            case conflicted :
                return SVNStatusKind.CONFLICTED;
            case obstructed :
                return SVNStatusKind.OBSTRUCTED;
            case ignored :
                return SVNStatusKind.IGNORED;  
            case external:
                return SVNStatusKind.EXTERNAL;
            case unversioned :
                return SVNStatusKind.UNVERSIONED;
            default : {
            	log.severe("unknown status kind :"+kind);
                return SVNStatusKind.NONE;
            }
        }
    }


	static JhlDirEntry convert(DirEntry dirEntry) {
		return new JhlDirEntry(dirEntry);
	}

    public static JhlStatus[] convertStatus(List<Status> status, ISVNClient client) {
        JhlStatus[] jhlStatus = new JhlStatus[status.size()];
        int i=0;
        for (Status stat : status) {
            jhlStatus[i] = new JhlStatus(stat, client);
            i++;
		}
        return jhlStatus;
    }
    
    static ISVNLogMessageChangePath[] convertChangePaths(Set<ChangePath> changePaths) {
        if (changePaths == null)
            return new SVNLogMessageChangePath[0];
        SVNLogMessageChangePath[] jhlChangePaths = new SVNLogMessageChangePath[changePaths.size()];
        int i =0;
        for (ChangePath path : changePaths) {
        	jhlChangePaths[i] = new JhlLogMessageChangePath(path);
        	i++;
		}
        return jhlChangePaths;
    }
    
    public static SVNScheduleKind convertScheduleKind(Info.ScheduleKind kind) {
    	if (kind == null) {
    		return null;
    	}
        switch (kind) {
        	case normal:
        		return SVNScheduleKind.NORMAL;
        	case delete:
        		return SVNScheduleKind.DELETE;
        	case add:
        		return SVNScheduleKind.ADD;
        	case replace:
        		return SVNScheduleKind.REPLACE;        	
        	default : {
        		log.severe("unknown schedule kind :"+kind);
        		return SVNScheduleKind.NORMAL;
        	}
        }
    }
    
    public static JhlLock convertLock(Lock lock) {
        return new JhlLock(lock);
    }
    
    public static SVNConflictDescriptor convertConflictDescriptor(ConflictDescriptor d) {
    	if (d == null) return null;
    	SVNConflictVersion srcLeftVersion = null;
    	if (d.getSrcLeftVersion() != null) {
    		srcLeftVersion = new SVNConflictVersion(d.getSrcLeftVersion().getReposURL(), d.getSrcLeftVersion().getPegRevision(), d.getSrcLeftVersion().getPathInRepos(), d.getSrcLeftVersion().getNodeKind().ordinal());
    	}
    	SVNConflictVersion srcRightVersion = null;
    	if (d.getSrcRightVersion() != null) {
    		srcRightVersion = new SVNConflictVersion(d.getSrcRightVersion().getReposURL(), d.getSrcRightVersion().getPegRevision(), d.getSrcRightVersion().getPathInRepos(), d.getSrcRightVersion().getNodeKind().ordinal());
    	}
    	return new SVNConflictDescriptor(d.getPath(), d.getKind().ordinal(), d.getNodeKind().ordinal(),
    			d.getPropertyName(), d.isBinary(),
                d.getMIMEType(), d.getAction().ordinal(), d.getReason().ordinal(), d.getOperation().ordinal(),
                srcLeftVersion, srcRightVersion,
                d.getBasePath(), d.getTheirPath(),
                d.getMyPath(), d.getMergedPath());
    }
    
    public static SVNConflictResult convertConflictResult(ConflictResult r) {
    	return new SVNConflictResult(r.getChoice().ordinal(), r.getMergedPath());
    }

	public static SVNDiffSummary convert(DiffSummary d) {
		return new SVNDiffSummary(d.getPath(), JhlConverter.convert(d.getDiffKind()),
				d.propsChanged(), d.getNodeKind().ordinal());
	}
	
	public static SVNDiffSummary.SVNDiffKind convert(DiffSummary.DiffKind d) {
		if (d == DiffSummary.DiffKind.added) {
			return SVNDiffSummary.SVNDiffKind.ADDED;
		} else if (d == DiffSummary.DiffKind.modified) {
			return SVNDiffSummary.SVNDiffKind.MODIFIED;
		} else if (d == DiffSummary.DiffKind.deleted) {
			return SVNDiffSummary.SVNDiffKind.DELETED;
		} else {
			return SVNDiffSummary.SVNDiffKind.NORMAL;
		}
	}
	
	public static ConflictResult.Choice convert(SVNConflictResult result) {
		if (result == null) {
			return null;
		}
		switch (result.getChoice()) {
		case SVNConflictResult.chooseBase:
			return ConflictResult.Choice.chooseBase;
		case SVNConflictResult.chooseMerged:
			return ConflictResult.Choice.chooseMerged;
		case SVNConflictResult.chooseMine:
			return ConflictResult.Choice.chooseMineConflict;
		case SVNConflictResult.chooseMineFull:
			return ConflictResult.Choice.chooseMineFull;
		case SVNConflictResult.chooseTheirs:
			return ConflictResult.Choice.chooseTheirsConflict;
		case SVNConflictResult.chooseTheirsFull:
			return ConflictResult.Choice.chooseTheirsFull;
		case SVNConflictResult.postpone:
			return ConflictResult.Choice.postpone;
		default:
			return ConflictResult.Choice.postpone;
		}
	}
    
	public static char convert(ChangePath.Action action) {
		switch (action) {
		case add:
			return 'A';
		case delete:
			return 'D';
		case modify:
			return 'M';
		case replace:
			return 'R';
		default:
			return '?';	
		}
	}
	
	public static org.apache.subversion.javahl.types.Depth depth(int depthValue) {
		switch(depthValue) {
		case Depth.empty:
			return org.apache.subversion.javahl.types.Depth.empty;
		case Depth.files:
			return org.apache.subversion.javahl.types.Depth.files;
		case Depth.immediates:
			return org.apache.subversion.javahl.types.Depth.immediates;
		case Depth.infinity:
			return org.apache.subversion.javahl.types.Depth.infinity;
		case Depth.exclude:
			return org.apache.subversion.javahl.types.Depth.exclude;
		default:
			return org.apache.subversion.javahl.types.Depth.unknown;
		}
		
	}
}
