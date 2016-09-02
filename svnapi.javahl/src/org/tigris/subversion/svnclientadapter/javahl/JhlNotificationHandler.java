/*******************************************************************************
 * Copyright (c) 2003, 2006 svnClientAdapter project and others.
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

import org.apache.subversion.javahl.ClientNotifyInformation;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.types.RevisionRange;
import org.apache.subversion.javahl.callback.ClientNotifyCallback;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNConflictVersion.NodeKind;
import org.tigris.subversion.svnclientadapter.SVNNotificationHandler;
import org.tigris.subversion.svnclientadapter.utils.Messages;



/**
 * Notification handler :
 * It listens to events from javahl jni implementation and handles 
 * notifications from SVNClientAdapter.
 * It sends notifications to all listeners 
 * 
 * It mimics svn output (see subversion/clients/cmdline/notify.c)
 * 
 *
 */
public class JhlNotificationHandler extends SVNNotificationHandler implements ClientNotifyCallback {
    private boolean receivedSomeChange;
    private boolean sentFirstTxdelta;
    
    private int updates;
    private int adds;
    private int deletes;
    private int conflicts;
    private int merges;
    private int exists;
    private int propConflicts;
    private int treeConflicts;
    private int propMerges;
    private int propUpdates;
    private boolean inExternal;
    private boolean holdStats;
    private String lastUpdate;
    private String lastExternalUpdate;
    
    private boolean statsCommand = false;
    
    private static final int COMMIT_ACROSS_WC_COMPLETED = -11;
    private static final int ENDED_ABNORMAL = -1;

    public void onNotify(ClientNotifyInformation info) {
        // for some actions, we don't want to call notifyListenersOfChange :
        // when the status of the target has not been modified 
        boolean notify = true;

        switch (info.getAction()) {
         	case foreign_merge_begin :
        		if (info.getMergeRange() != null) {
	        		if (info.getMergeRange().getFromRevision().equals(info.getMergeRange().getToRevision()))
	        			logMessage("--- Merging (from foreign repository) r" + info.getMergeRange().getFromRevision().toString() + " into " + info.getPath());
	        		else
	        			if (info.getMergeRange().getToRevision().equals(Revision.HEAD) || 
	        					RevisionRange.getRevisionAsLong(info.getMergeRange().getToRevision()).longValue() > RevisionRange.getRevisionAsLong(info.getMergeRange().getFromRevision()).longValue())
	        				logMessage("--- Merging (from foreign repository) r" + info.getMergeRange().getFromRevision().toString() + " through r" + info.getMergeRange().getToRevision().toString() + " into " + info.getPath());
	        			else
	        				logMessage("--- Reverse-merging (from foreign repository) r" + info.getMergeRange().getFromRevision().toString() + " through r" + info.getMergeRange().getToRevision().toString() + " into " + info.getPath());
		        } else {
	        		logMessage("--- Merging differences between foreign repository URLs into " + info.getPath());
	        	}
        		notify = false;
        		break;
        	case merge_begin :
        		if (info.getMergeRange() != null) {
	        		if (info.getMergeRange().getFromRevision().equals(info.getMergeRange().getToRevision()))
	        			logMessage("--- Merging r" + info.getMergeRange().getFromRevision().toString() + " into " + info.getPath());
	        		else
	        			if (info.getMergeRange().getToRevision().equals(Revision.HEAD) || 
	        					RevisionRange.getRevisionAsLong(info.getMergeRange().getToRevision()).longValue() > RevisionRange.getRevisionAsLong(info.getMergeRange().getFromRevision()).longValue())
	        				logMessage("--- Merging r" + info.getMergeRange().getFromRevision().toString() + " through r" + info.getMergeRange().getToRevision().toString() + " into " + info.getPath());
	        			else
	        				logMessage("--- Reverse-merging r" + info.getMergeRange().getFromRevision().toString() + " through r" + info.getMergeRange().getToRevision().toString() + " into " + info.getPath());
		        } else {
	        		logMessage("--- Merging differences between repository URLs into " + info.getPath());
	        	}
        		notify = false;
        		break;
            case skip :
            	notify = logSkipped(info, Messages.bind("notify.skipped", info.getPath())); //$NON-NLS-1$
                break;
            case skip_conflicted :
            	notify = logSkipped(info, Messages.bind("notify.skipped.conflicted", info.getPath())); //$NON-NLS-1$
                break;                
            case update_skip_obstruction :
            	notify = logSkipped(info, Messages.bind("notify.update.skip.obstruction", info.getPath())); //$NON-NLS-1$
                break;
            case update_skip_working_only :
            	notify = logSkipped(info, Messages.bind("notify.update.skip.working.only", info.getPath())); //$NON-NLS-1$
                break;  
            case update_skip_access_denied :
            	notify = logSkipped(info, Messages.bind("notify.update.skip.access.denied", info.getPath())); //$NON-NLS-1$
                break;                  
            case failed_lock: 
            	notify = logFailedOperation(info, Messages.bind("notify.lock.failed", info.getPath())); //$NON-NLS-1$
                break;
            case failed_unlock:
            	notify = logFailedOperation(info, Messages.bind("notify.unlock.failed", info.getPath())); //$NON-NLS-1$
            	break;
            case locked:
                if (info.getLock() != null && info.getLock().getOwner() != null)
                    logMessage(Messages.bind("notify.lock.other", info.getLock().getPath(), info.getLock().getOwner())); //$NON-NLS-1$
                else
                    logMessage(Messages.bind("notify.lock", info.getPath())); //$NON-NLS-1$
        	    notify = false; // for JavaHL bug
            	break;
            case unlocked:
                logMessage(Messages.bind("notify.unlock", info.getPath())); //$NON-NLS-1$
            	notify = false; // for JavaHL bug
            	break;
            case update_delete :
            case update_shadowed_delete :
                logMessage("D   " + info.getPath()); //$NON-NLS-1$
                receivedSomeChange = true;
                deletes += 1;
                break;
            case update_replaced :
                logMessage("R   " + info.getPath()); //$NON-NLS-1$
                receivedSomeChange = true;
                adds += 1;
                deletes += 1;
                break;
            case update_add :
            case update_shadowed_add :
                logMessage("A   " + info.getPath()); //$NON-NLS-1$
                receivedSomeChange = true;
                adds += 1;
                break;
            case exists :
                logMessage("E   " + info.getPath()); //$NON-NLS-1$
                receivedSomeChange = true;
                exists += 1;
                break;
            case changelist_set :
                logMessage(Messages.bind("notify.changelist.set", info.getPath())); //$NON-NLS-1$
                notify = false;
                break; 
            case changelist_clear :
                logMessage(Messages.bind("notify.changelist.clear", info.getPath())); //$NON-NLS-1$
                notify = false;
                break;  
            case changelist_moved :
                logMessage(Messages.bind("notify.changelist.moved", info.getPath())); //$NON-NLS-1$
                notify = false;
                break;                        
            case restore :
                logMessage(Messages.bind("notify.restored", info.getPath())); //$NON-NLS-1$
                break;
            case revert :
                logMessage(Messages.bind("notify.reverted", info.getPath())); //$NON-NLS-1$
                break;
            case failed_revert :
                logError(Messages.bind("notify.revert.failed", info.getPath())); //$NON-NLS-1$
                notify = false;
                break;
            case resolved :
                logMessage(Messages.bind("notify.resolved", info.getPath())); //$NON-NLS-1$
                break;
            case add :
                logMessage("A         " + info.getPath()); //$NON-NLS-1$
                break;
            case copy :
                logMessage(Messages.bind("notify.copy", info.getPath())); //$NON-NLS-1$
                notify = false;
                break;                
            case delete :
                logMessage("D         " + info.getPath()); //$NON-NLS-1$
                receivedSomeChange = true;
                break;
            case tree_conflict :
                logError("  C " + info.getPath()); //$NON-NLS-1$
                receivedSomeChange = true;
                treeConflicts += 1;
            	break;
            case update_update :
            case update_shadowed_update :
                boolean error = false;
                if (!((info.getKind().ordinal() == NodeKind.directory)
                    && ((info.getPropState() == ClientNotifyInformation.Status.inapplicable)
                        || (info.getPropState() == ClientNotifyInformation.Status.unknown)
                        || (info.getPropState() == ClientNotifyInformation.Status.unchanged)))) {
                    receivedSomeChange = true;
                    char[] statecharBuf = new char[] { ' ', ' ' };
                    if (info.getKind().ordinal() == NodeKind.file) {
                        if (info.getContentState() == ClientNotifyInformation.Status.conflicted) {
                            statecharBuf[0] = 'C';
                            conflicts += 1;
                            error = true;
                        }
                        else if (info.getContentState() == ClientNotifyInformation.Status.merged) {
                            statecharBuf[0] = 'G';
                            merges += 1;
                            error = true;
                        }
                        else if (info.getContentState() == ClientNotifyInformation.Status.changed) {
                            statecharBuf[0] = 'U';
                            updates += 1;
                        }
                        else if (info.getContentState() == ClientNotifyInformation.Status.unchanged && info.getPropState().ordinal() < ClientNotifyInformation.Status.obstructed.ordinal())
                            break;
                    }
                    if (info.getPropState() == ClientNotifyInformation.Status.conflicted) {
                        statecharBuf[1] = 'C';
                        propConflicts += 1;
                        error = true;
                    }
                    else if (info.getPropState() == ClientNotifyInformation.Status.merged) {
                        statecharBuf[1] = 'G';
                        propMerges += 1;
                        error = true;
                    }
                    else if (info.getPropState() == ClientNotifyInformation.Status.changed) {
                        statecharBuf[1] = 'U';
                        propUpdates += 1;
                    }
                    if (info.getContentState() == ClientNotifyInformation.Status.unknown && info.getPropState() == ClientNotifyInformation.Status.unknown)
                    	break;
                    if (error)
                        logError("" + statecharBuf[0] + statecharBuf[1] + "  " + info.getPath());                       //$NON-NLS-1$ //$NON-NLS-2$
                    else
                        logMessage("" + statecharBuf[0] + statecharBuf[1] + "  " + info.getPath());                       //$NON-NLS-1$ //$NON-NLS-2$
                }
                break;
            case update_external :
                logMessage(Messages.bind("notify.update.external", info.getPath())); //$NON-NLS-1$
            	inExternal = true;
                break;
            case update_external_removed :
                logMessage(Messages.bind("notify.update.external.removed", info.getPath())); //$NON-NLS-1$
                break;                
            case update_completed :
                notify = false;
                if (info.getRevision() >= 0) {
                    logRevision( info.getRevision(), info.getPath() );

                    if (command == ISVNNotifyListener.Command.EXPORT) {
                        logCompleted(Messages.bind("notify.export", Long.toString(info.getRevision()))); //$NON-NLS-1$
                    }                       
                    else 
                    if (command == ISVNNotifyListener.Command.CHECKOUT) {
                        logCompleted(Messages.bind("notify.checkout", Long.toString(info.getRevision()))); //$NON-NLS-1$
                    }                       
                    else
                    if (receivedSomeChange) {
                        if (holdStats) {
                        // Hold off until the releaseStats() method
                        // is executed.  Keeps noise out of the log.
                            if (inExternal)
                                lastExternalUpdate = Messages.bind("notify.update", Long.toString(info.getRevision())); //$NON-NLS-1$
                            else
                                lastUpdate = Messages.bind("notify.update", Long.toString(info.getRevision())); //$NON-NLS-1$
                            
                        } else
                            logCompleted(Messages.bind("notify.update", Long.toString(info.getRevision()))); //$NON-NLS-1$
                    }
                    else {
                        logCompleted(Messages.bind("notify.at", Long.toString(info.getRevision()))); //$NON-NLS-1$
                    }
                } else
                {
                    if (command == ISVNNotifyListener.Command.EXPORT) {
                        logCompleted(Messages.bind("notify.export.complete")); //$NON-NLS-1$
                    }
                    else
                    if (command == ISVNNotifyListener.Command.CHECKOUT) {
                        logCompleted(Messages.bind("notify.checkout.complete")); //$NON-NLS-1$
                    }
                    else {
                        logCompleted(Messages.bind("notify.update.complete")); //$NON-NLS-1$
                    }  
                }
                break;
            case status_external :
              if (!skipCommand())
                logMessage(Messages.bind("notify.status.external", info.getPath())); //$NON-NLS-1$
              notify = false;
              break;
            case status_completed :
              notify = false;
              if (info.getRevision() >= 0) {
                logRevision(info.getRevision(), info.getPath());
                if (!skipCommand())
                    logMessage(Messages.bind("notify.status.revision", Long.toString(info.getRevision()))); //$NON-NLS-1$
              }
              break;                
            case commit_modified :
                logMessage(Messages.bind("notify.commit.modified", info.getPath())); //$NON-NLS-1$
                break;
            case commit_added :
            case commit_copied :
                logMessage(Messages.bind("notify.commit.add", info.getPath())); //$NON-NLS-1$
                break;
            case commit_deleted :
                logMessage(Messages.bind("notify.commit.delete", info.getPath())); //$NON-NLS-1$
                break;
            case commit_replaced :
            case commit_copied_replaced :
                logMessage(Messages.bind("notify.commit.replace", info.getPath())); //$NON-NLS-1$
                break;
            case commit_postfix_txdelta :
                notify = false;
                if (!sentFirstTxdelta) {
                    logMessage(Messages.bind("notify.commit.transmit")); //$NON-NLS-1$
                    sentFirstTxdelta = true;
                }
                break;  
            case url_redirect :
            	break;
            case property_added:
            	logMessage(Messages.bind("notify.property.set", info.getPath())); //$NON-NLS-1$
            	break;
            case property_modified:
            	logMessage(Messages.bind("notify.property.set", info.getPath())); //$NON-NLS-1$
            	break; 
            case property_deleted:
            	logMessage(Messages.bind("notify.property.deleted", info.getPath())); //$NON-NLS-1$
            	break;
            case property_deleted_nonexistent:
            	notify = false;
            	logMessage(Messages.bind("notify.property.deleted.nonexistent")); //$NON-NLS-1$
            	break; 
            case revprop_set:
            	notify = false;
            	logMessage(Messages.bind("notify.revision.property.set")); //$NON-NLS-1$
            	break;  
            case revprop_deleted:
            	notify = false;
            	logMessage(Messages.bind("notify.revision.property.deleted")); //$NON-NLS-1$
            	break;    
            case merge_completed:
            	break;                   	
            case blame_revision:
            	break;
            case update_started:
            	break;
            case merge_record_info:
            	logMessage(Messages.bind("notify.merge.record.info", info.getPath())); //$NON-NLS-1$
            	propUpdates += 1;
            	break;
            case merge_record_info_begin:
            	break;
            case merge_elide_info:
            	break;
            case patch:
            	notify = false;
            	logMessage(Messages.bind("notify.patch")); //$NON-NLS-1$
            	break;
            case patch_applied_hunk:
            	logMessage(Messages.bind("notify.patch.applied.hunk", info.getPath())); //$NON-NLS-1$
            	break;  
            case patch_rejected_hunk:
            	notify = logFailedOperation(info, Messages.bind("notify.patch.rejected.hunk", info.getPath()));
            	break;
            case patch_hunk_already_applied:
            	notify = logFailedOperation(info, Messages.bind("notify.patch.hunk.already.applied", info.getPath()));
            	break;                       	
            case upgraded_path:
            	logMessage(Messages.bind("notify.upgraded.path", info.getPath())); //$NON-NLS-1$
            	break;
            case failed_external: 
            	notify = logFailedOperation(info, Messages.bind("notify.external", info.getPath())); //$NON-NLS-1$
                break;     
            case failed_conflict: 
            	notify = logFailedOperation(info, Messages.bind("notify.conflict", info.getPath())); //$NON-NLS-1$
                break;  
            case failed_missing: 
            	notify = logFailedOperation(info, Messages.bind("notify.missing", info.getPath())); //$NON-NLS-1$
                break;                      
            case failed_out_of_date: 
            	notify = logFailedOperation(info, Messages.bind("notify.out.of.date", info.getPath())); //$NON-NLS-1$
                break; 
            case failed_no_parent:
            	notify = logFailedOperation(info, Messages.bind("notify.no.parent")); //$NON-NLS-1$
                break;                
            case failed_locked:
            	notify = logFailedOperation(info, Messages.bind("notify.locked", info.getPath())); //$NON-NLS-1$
                break;
            case failed_forbidden_by_server:
            	notify = logFailedOperation(info, Messages.bind("notify.forbidden.by.server")); //$NON-NLS-1$
                break;
            case failed_obstructed:
            	notify = logFailedOperation(info, Messages.bind("notify.obstructed")); //$NON-NLS-1$
                break;                 
            case path_nonexistent:
            	notify = logFailedOperation(info, Messages.bind("notify.path.nonexistent", info.getPath())); //$NON-NLS-1$
                break;   
            case exclude:
            	logMessage(Messages.bind("notify.exclude", info.getPath())); //$NON-NLS-1$
            	break;
            case conflict_resolver_starting:
            	break;
            case conflict_resolver_done:
            	break; 
            case left_local_modifications:
            	logMessage(Messages.bind("notify.left.local.modifications", info.getPath())); //$NON-NLS-1$
            	break;            	
            case foreign_copy_begin:
            	break;
            case update_broken_lock:
            	logError(Messages.bind("notify.lock.broken", info.getPath())); //$NON-NLS-1$
            	break; 
            case move_broken:
            	logError(Messages.bind("notify.move.broken", info.getPath())); //$NON-NLS-1$
            	break;         
            default:
            	if (info.getAction().ordinal() == ENDED_ABNORMAL) {
            		if (command == ISVNNotifyListener.Command.COMMIT)
            			logError(Messages.bind("notify.commit.abnormal")); //$NON-NLS-1$
            		else
            			logError(Messages.bind("notify.end.abnormal")); //$NON-NLS-1$
            		if (info.getErrMsg() != null)
            			logError(info.getErrMsg()); 
            		notify = false;                                
            		break;
            	}
            	if (info.getAction().ordinal() == COMMIT_ACROSS_WC_COMPLETED) {
                    notify = false;
                    logCompleted(Messages.bind("notify.commit", Long.toString(info.getRevision()))); //$NON-NLS-1$
                    break;
            	}
            	logMessage("Unknown action received: " + info.getAction());
                	
        }

        if (notify) {
            // only when the status changed
            notifyListenersOfChange(info.getPath(), JhlConverter.convertNodeKind(info.getKind()));                
        }
    }
    
    private boolean logFailedOperation(ClientNotifyInformation info, String defaultErrorMessage) {
    	if (info.getErrMsg() == null)
    		logError(defaultErrorMessage);
    	else
    		logError(info.getErrMsg());
    	return false;
    }
    
    private boolean logSkipped(ClientNotifyInformation info, String defaultErrorMessage)	 {
    	if (info.getErrMsg() == null)
    		logMessage(defaultErrorMessage);
    	else
    		logError(info.getErrMsg());
    	return false;    	
    }

    public void setCommand(int command) {
        receivedSomeChange = false;
        sentFirstTxdelta = false;
        if (command == ISVNNotifyListener.Command.UPDATE
                || command == ISVNNotifyListener.Command.MERGE
                || command == ISVNNotifyListener.Command.SWITCH) {
        	clearStats();
        	statsCommand = true;
        }
        super.setCommand(command);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.SVNNotificationHandler#logCompleted(java.lang.String)
     */
    public void logCompleted(String message) {
        super.logCompleted(message);
        if (inExternal)
            inExternal = false;
        else
            logStats();
    }

    private void clearStats(){
        adds = 0;
        updates = 0;
        deletes = 0;
        conflicts = 0;
        merges = 0;
        exists = 0;
        propConflicts = 0;
        treeConflicts = 0;
        propMerges = 0;
        propUpdates = 0;
        inExternal = false;
        holdStats = false;
        lastUpdate = null;
        lastExternalUpdate = null;
    }
    
    private void logStats() {
        if (holdStats)
            return;
        if (statsCommand) {
	        if (fileStats()) {
	            logMessage(Messages.bind("notify.stats.file.head")); //$NON-NLS-1$
		        if (merges > 0)
		            logMessage(Messages.bind("notify.stats.merge", Integer.toString(merges))); //$NON-NLS-1$
		        if (deletes > 0)
		            logMessage(Messages.bind("notify.stats.delete", Integer.toString(deletes))); //$NON-NLS-1$
		        if (adds > 0)
		            logMessage(Messages.bind("notify.stats.add", Integer.toString(adds))); //$NON-NLS-1$
		        if (updates > 0)
		            logMessage(Messages.bind("notify.stats.update", Integer.toString(updates))); //$NON-NLS-1$
		        if (exists > 0)
		            logMessage(Messages.bind("notify.stats.exists", Integer.toString(exists))); //$NON-NLS-1$
	        }
	        if (propStats()){
	            logMessage(Messages.bind("notify.stats.prop.head")); //$NON-NLS-1$
		        if (propMerges > 0)
		            logMessage(Messages.bind("notify.stats.merge", Integer.toString(propMerges))); //$NON-NLS-1$
		        if (propUpdates > 0)
		            logMessage(Messages.bind("notify.stats.update", Integer.toString(propUpdates))); //$NON-NLS-1$
	        }
	        if (conflictStats()) {
	            logMessage(Messages.bind("notify.stats.conflict.head")); //$NON-NLS-1$
		        if (conflicts > 0)
		            logMessage(Messages.bind("notify.stats.conflict", Integer.toString(conflicts))); //$NON-NLS-1$
		        if (propConflicts > 0)
		            logMessage(Messages.bind("notify.stats.prop.conflicts", Integer.toString(propConflicts))); //$NON-NLS-1$
		        if (treeConflicts > 0) {
		            logMessage(Messages.bind("notify.stats.tree.conflicts", Integer.toString(treeConflicts))); //$NON-NLS-1$
		        }
	        }
	        statsCommand = false;
	        clearStats();
        }
    }
    
    private boolean fileStats() {
        if (updates > 0 || adds > 0 || deletes > 0 
                || merges > 0 || exists > 0)
            return true;
        return false;
    }

    
    private boolean conflictStats() {
        if (treeConflicts > 0 || propConflicts > 0
                || conflicts > 0)
            return true;
        return false;
    }

    private boolean propStats() {
        if (propUpdates > 0               
                || propMerges > 0)
            return true;
        return false;
    }

   
    /**
     * Put a hold on the logging of stats.  This method allows
     * the update method to hold off logging stats until all of
     * a set of updates are completed.
     */
    public void holdStats() {
        this.holdStats = true;
    }
    
    
    /**
     * Perform the logging of any accumulated stats.
     * The update method will call this after the command completes
     * so that the stats logging can wait until the very end.
     */
    public void releaseStats() {
        this.holdStats = false;
        if (command == ISVNNotifyListener.Command.UPDATE) {
            // In addition to the stats, need to send the 
            // Updated to revision N. messages that normally
            // appear in the log.
            if (lastExternalUpdate != null)
                logCompleted(lastExternalUpdate);
            if (lastUpdate != null)
                logCompleted(lastUpdate);
        }
        logStats();
    }
}