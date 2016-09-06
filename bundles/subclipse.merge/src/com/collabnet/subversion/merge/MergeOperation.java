/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.actions.CommitAction;
import org.tigris.subversion.subclipse.ui.operations.SVNOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.collabnet.subversion.merge.views.MergeResultsView;
import com.collabnet.subversion.merge.wizards.DialogWizard;
import com.collabnet.subversion.merge.wizards.MergeWizardDialog;

public class MergeOperation extends SVNOperation {
	private IResource[] mergedResources;
    
    private SVNUrl[] svnUrls1;
    private SVNUrl[] svnUrls2;
    
    private SVNRevision svnRevision1;
    private SVNRevision svnRevision2;
    
    private SVNRevisionRange[] revisions;
    
    private MergeOutput mergeOutput;

    private boolean force = false;
    private boolean ignoreAncestry = false;
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean recordOnly = false;
    private boolean unblock = false;
    
    private int textConflictHandling;
    private int binaryConflictHandling;
    private int propertyConflictHandling;
    private int treeConflictHandling;

	private MergeCommand command;
    private MergeOutput abortedMergeOutput;
    private MergeOutput incompleteOutput;
    private boolean unresolvedConflicts;
    private boolean abnormalEnd = false;
    
    private boolean resumed = false;

    public MergeOperation(IWorkbenchPart part, IResource[] resources, SVNUrl[] svnUrls1, SVNRevision svnRevision1, SVNUrl[] svnUrls2, SVNRevision svnRevision2, SVNRevisionRange[] revisions, MergeOutput mergeOutput) {
    	super(part);
        mergedResources = resources;
        this.svnUrls1 = svnUrls1;
        this.svnRevision1 = svnRevision1;
        this.svnUrls2 = svnUrls2;
        this.svnRevision2 = svnRevision2;  
        this.revisions = revisions;
        this.mergeOutput = mergeOutput;
        if (mergeOutput != null) resumed = true;
    }
    
    protected String getTaskName() {
        return Messages.MergeOperation_title;
    }

    protected String getTaskName(SVNTeamProvider provider) {
    	return Messages.MergeOperation_merging + provider.getProject().getName();             
    }
    
    public void runWithNoMonitor() throws InvocationTargetException, InterruptedException {
    	run();
    }

    protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(getTaskName(), mergedResources.length);
        ArrayList mergeOutputs = new ArrayList();
        for (int i = 0; i < mergedResources.length; i++) {
			try {	
				monitor.subTask(mergedResources[i].getName());
				incompleteOutput = null;
				if (mergeOutput == null) {
					incompleteOutput = MergeOutput.getIncompleteMerge(mergedResources[i], svnUrls1[i].toString(), svnUrls2[i].toString());
				}
				if (incompleteOutput == null)
					command = new MergeCommand(mergedResources[i], svnUrls1[i], svnRevision1, svnUrls2[i], svnRevision2, revisions, mergeOutput);
				else { 
					if (incompleteOutput.hasUnresolvedConflicts()) {
						unresolvedConflicts = true;
						break;
					}
					command = new MergeCommand(mergedResources[i], svnUrls1[i], svnRevision1, svnUrls2[i], svnRevision2, revisions, incompleteOutput);
				}
				command.setPart(getPart());
		    	command.setForce(force);
		    	command.setIgnoreAncestry(ignoreAncestry);
		    	command.setDepth(depth);
		    	command.setTextConflictHandling(textConflictHandling);
		    	command.setBinaryConflictHandling(binaryConflictHandling);
		    	command.setPropertyConflictHandling(propertyConflictHandling);
		    	command.setTreeConflictHandling(treeConflictHandling);
		    	command.setRecordOnly(recordOnly);
		    	command.run(Policy.subMonitorFor(monitor,1000));
		    	monitor.worked(1);
		    	if (recordOnly) {
		    		MergeOutput mergeOutput = command.getMergeOutput();
		    		MergeSummaryResult[] summaryResults = { new MergeSummaryResult(MergeSummaryResult.PROPERTY, "Updated", "1") }; //$NON-NLS-1$ //$NON-NLS-2$
		    		mergeOutput.setMergeSummaryResults(summaryResults);
		    		MergeResult[] mergeResults = { new AdaptableMergeResult(" ", "U", " ", mergedResources[i].getLocation().toString(), false) }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		    		mergeOutput.setMergeResults(mergeResults);
					ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(mergedResources[i]);
					if (svnResource != null) {
						svnResource.refreshStatus();
					}
		    	}
			} catch (SVNException e) {
				if (e.operationInterrupted()) {
					showCancelledMessage();
				} else {
					collectStatus(e.getStatus());
				}
			    abnormalEnd = true;
			}
			MergeOutput mergeOutput = command.getMergeOutput();
			mergeOutputs.add(mergeOutput);
			if (command.isMergeAborted()) {
				mergeOutput.setIncomplete(true);
				mergeOutput.setNormalEnd(true);
				mergeOutput.store();
				abortedMergeOutput = mergeOutput;
				MergeResultsView view = MergeResultsView.getView();
				if (view != null) view.refreshAsync(mergeOutput);
				break;
			} else {
				mergeOutput.setIncomplete(false);
				if (!recordOnly && !abnormalEnd) {
					mergeOutput.setNormalEnd(true);
					mergeOutput.store();
					MergeResultsView view = MergeResultsView.getView();
					if (view != null) view.refreshAsync(mergeOutput);	
				}
			}
        }
        if (recordOnly) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					DialogWizard dialogWizard = new DialogWizard(DialogWizard.COMMIT_MERGEINFO_PROPERTY);
					dialogWizard.setUnblock(unblock);
					MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard, true);
					if (dialog.open() == MergeWizardDialog.CANCEL) return;
		        	CommitAction commitAction = new CommitAction();
		        	commitAction.setSelectedResources(mergedResources);
		        	commitAction.run(null);					
				}
			});
        } else {
	        final MergeOutput[] mergeOutputArray = new MergeOutput[mergeOutputs.size()];
	        mergeOutputs.toArray(mergeOutputArray);
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (unresolvedConflicts) {
						DialogWizard dialogWizard = new DialogWizard(DialogWizard.UNRESOLVED_CONFLICTS);
						dialogWizard.setMergeOutput(incompleteOutput);
						MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
						dialog.open();
					} else {
						if (command.isMergeAborted()) {
							DialogWizard dialogWizard = new DialogWizard(DialogWizard.MERGE_ABORTED);
							dialogWizard.setMergeOutput(abortedMergeOutput);
							dialogWizard.setErrorMessage(command.getErrorMessage());
							MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
							dialog.open();
						}
					}
					if (mergeOutputArray.length > 0 && !abnormalEnd) {
						DialogWizard dialogWizard = new DialogWizard(DialogWizard.SUMMARY);
						dialogWizard.setMergeOutputs(mergeOutputArray);
						dialogWizard.setResumed(resumed);
						MergeWizardDialog dialog = new MergeWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
						dialog.open();
					}
				}			
			});	     
        }
        monitor.done();
    }
    
	public void setForce(boolean force) {
		this.force = force;
	}

	public void setIgnoreAncestry(boolean ignoreAncestry) {
		this.ignoreAncestry = ignoreAncestry;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setTextConflictHandling(int textConflictHandling) {
		this.textConflictHandling = textConflictHandling;
	}

	public void setBinaryConflictHandling(int binaryConflictHandling) {
		this.binaryConflictHandling = binaryConflictHandling;
	}
	
    public void setPropertyConflictHandling(int propertyConflictHandling) {
		this.propertyConflictHandling = propertyConflictHandling;
	}
    
    public void setTreeConflictHandling(int treeConflictHandling) {
		this.treeConflictHandling = treeConflictHandling;
	}

	public void setRecordOnly(boolean recordOnly) {
		this.recordOnly = recordOnly;
	}
	
	public void setUnblock(boolean unblock) {
		this.unblock = unblock;
	}

}
