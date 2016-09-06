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
package com.collabnet.subversion.merge.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

import com.collabnet.subversion.merge.ConflictResolution;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.Messages;

public class DialogWizard extends Wizard {
	private boolean resumed;
	private MergeOutput[] mergeOutputs;
	private boolean conflictResolved;
	private boolean unblock;
	private String errorMessage;
	private SVNConflictDescriptor conflictDescriptor;
	private String myValue;
	private String incomingValue;
	private String valueToUse;
	private IResource[] resources;
	private boolean textConflicts;

	private boolean propertyConflicts;
	private boolean treeConflicts;
	private volatile ConflictResolution conflictResolution;
	private int resolution;
	private FinishedEditingWizardPage finishedEditingWizardPage;
	private ConflictHandlingWizardPage conflictHandlingWizardPage;
	private ResolveConflictWizardPage resolveConflictWizardPage;
	private PropertyValueSelectionWizardPage propertyValueSelectionWizardPage;
	
	public int type;
	
	public final static int SUMMARY = 0;
	public final static int CONFLICT_HANDLING = 1;
	public final static int CONFLICTS_RESOLVED = 2;
	public final static int FINISHED_EDITING = 3;
	public final static int UNDO_MERGE_WARNING = 4;
	public final static int UNDO_MERGE_COMPLETED = 5;
	public final static int MERGE_ABORTED = 6;
	public final static int RESUME_MERGE = 7;
	public final static int UNRESOLVED_CONFLICTS = 8;
	public final static int MARK_RESOLVED = 9;
	public final static int PROPERTY_VALUE_SELECTION = 10;
	public final static int COMMIT_MERGEINFO_PROPERTY = 11;

	public DialogWizard(int type) {
		super();
		this.type = type;
	}

	public void addPages() {
		super.addPages();
		if (type == SUMMARY) {
			setWindowTitle(Messages.DialogWizard_summaryTitle);
			MergeSummaryWizardPage summaryPage = new MergeSummaryWizardPage("summary"); //$NON-NLS-1$
			summaryPage.setMergeOutputs(mergeOutputs); 
			summaryPage.setResumed(resumed);
			addPage(summaryPage);
		}
		if (type == FINISHED_EDITING) {
			setWindowTitle(Messages.DialogWizard_resolveTitle);
			boolean propertyConflict = conflictDescriptor != null && conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property;
			finishedEditingWizardPage = new FinishedEditingWizardPage("finishedEditing", propertyConflict); //$NON-NLS-1$
			addPage(finishedEditingWizardPage);
		}
		if (type == CONFLICT_HANDLING) {
			setWindowTitle(Messages.DialogWizard_handleTitle);
			conflictHandlingWizardPage = new ConflictHandlingWizardPage("handleConflict"); //$NON-NLS-1$
			conflictHandlingWizardPage.setConflictDescriptor(conflictDescriptor);
			conflictHandlingWizardPage.setResource(resources[0]);
			addPage(conflictHandlingWizardPage);
		}
		if (type == CONFLICTS_RESOLVED) {
			setWindowTitle(Messages.DialogWizard_allResolvedTitle);
			ConflictsResolvedWizardPage conflictsResolvedWizardPage = new ConflictsResolvedWizardPage("conflictsResolved"); //$NON-NLS-1$
			conflictsResolvedWizardPage.setMergeOutput(mergeOutputs[0]);
			addPage(conflictsResolvedWizardPage);
		}
		if (type == UNDO_MERGE_WARNING) {
			setWindowTitle(Messages.DialogWizard_undoTitle);
			QuestionWizardPage questionWizardPage = new QuestionWizardPage("question", Messages.DialogWizard_undoTitle, Messages.DialogWizard_revertWarning); //$NON-NLS-1$
			questionWizardPage.setQuestion(Messages.DialogWizard_revertWarning2);
			addPage(questionWizardPage);
		}
		if (type == UNDO_MERGE_COMPLETED) {
			setWindowTitle(Messages.DialogWizard_undoTitle);
			QuestionWizardPage questionWizardPage = new QuestionWizardPage("question", Messages.DialogWizard_undoTitle, Messages.DialogWizard_confirmRevert); //$NON-NLS-1$
			questionWizardPage.setQuestion(Messages.DialogWizard_confirmRevert2);
			addPage(questionWizardPage);		
		}
		if (type == MERGE_ABORTED) {
			setWindowTitle(Messages.DialogWizard_terminatedTitle);
			MergeAbortedWizardPage abortedPage = new MergeAbortedWizardPage("aborted"); //$NON-NLS-1$
			abortedPage.setMergeOutput(mergeOutputs[0]); 
			abortedPage.setErrorMessage(errorMessage);
			addPage(abortedPage);
		}
		if (type == RESUME_MERGE) {
			setWindowTitle(Messages.DialogWizard_resumeTitle);
			ResumeMergeWizardPage resumePage = new ResumeMergeWizardPage("resume"); //$NON-NLS-1$
			addPage(resumePage);
		}
		if (type == UNRESOLVED_CONFLICTS) {
			setWindowTitle(Messages.DialogWizard_unresolvedConflictsTitle);
			UnresolvedConflictsWizardPage unresolvedPage = new UnresolvedConflictsWizardPage("unresolved"); //$NON-NLS-1$
			unresolvedPage.setMergeOutput(mergeOutputs[0]); 
			addPage(unresolvedPage);
		}
		if (type == MARK_RESOLVED) {
			setWindowTitle(Messages.DialogWizard_resolveTitle);
			resolveConflictWizardPage = new ResolveConflictWizardPage("resolveConflict", resources); //$NON-NLS-1$
			resolveConflictWizardPage.setTextConflicts(textConflicts);
			resolveConflictWizardPage.setPropertyConflicts(propertyConflicts);
			resolveConflictWizardPage.setTreeConflicts(treeConflicts);
			addPage(resolveConflictWizardPage);
		}	
		if (type == PROPERTY_VALUE_SELECTION) {
			setWindowTitle(Messages.DialogWizard_handleConflictTitle);
			propertyValueSelectionWizardPage = new PropertyValueSelectionWizardPage("propertyValueSelection"); //$NON-NLS-1$
			propertyValueSelectionWizardPage.setConflictDescriptor(conflictDescriptor);
			propertyValueSelectionWizardPage.setMyValue(myValue);
			propertyValueSelectionWizardPage.setIncomingValue(incomingValue);
			propertyValueSelectionWizardPage.setResource(resources[0]);
			addPage(propertyValueSelectionWizardPage);
		}
		if (type == COMMIT_MERGEINFO_PROPERTY) {
			setWindowTitle(Messages.DialogWizard_mergeCompletedTitle);
			String message;
			String text;
			if (unblock) {
				message = Messages.DialogWizard_unblockedMessage;
				text = Messages.DialogWizard_unblockedMessage2;
			} else {
				message = Messages.DialogWizard_blockedMessage;
				text = Messages.DialogWizard_blockedMessage2;				
			}
			QuestionWizardPage questionWizardPage = new QuestionWizardPage("question", Messages.DialogWizard_commitMergeInfoTitle, message); //$NON-NLS-1$
			questionWizardPage.setQuestion(text);
			addPage(questionWizardPage);		
		}		
	}

	public boolean performFinish() {
		if (type == FINISHED_EDITING) {
//			conflictResolved = finishedEditingWizardPage.yesButton.getSelection();
			resolution = finishedEditingWizardPage.getResolution();
			conflictResolved = resolution != ISVNConflictResolver.Choice.postpone;
		}
		if (type == CONFLICT_HANDLING)
			conflictResolution = conflictHandlingWizardPage.getConflictResolution();
		if (type == CONFLICTS_RESOLVED) {
			if (mergeOutputs[0].isIncomplete())
				mergeOutputs[0].resume();
			else
				mergeOutputs[0].delete();
		}
		if (type == PROPERTY_VALUE_SELECTION) {
			conflictResolved = true;
			valueToUse = propertyValueSelectionWizardPage.getValue();
		}
		if (type == MARK_RESOLVED)
			resolution = resolveConflictWizardPage.getConflictResolution();
		return true;
	}

	public boolean performCancel() {
		if (type == CONFLICT_HANDLING)
			conflictResolution = conflictHandlingWizardPage.getConflictResolution();
		if (type == PROPERTY_VALUE_SELECTION) {
			valueToUse = propertyValueSelectionWizardPage.getValue();
			conflictResolved = false;
		}
		return super.performCancel();
	}

	public void setMergeOutput(MergeOutput mergeOutput) {
		mergeOutputs = new MergeOutput[1];
		mergeOutputs[0] = mergeOutput;
	}
	
	public void setMergeOutputs(MergeOutput[] mergeOutputs) {
		this.mergeOutputs = mergeOutputs;
	}	

	public boolean isConflictResolved() {
		return conflictResolved;
	}

	public ConflictResolution getConflictResolution() {
		return conflictResolution;
	}

	public void setConflictDescriptor(SVNConflictDescriptor conflictDescriptor) {
		this.conflictDescriptor = conflictDescriptor;
	}

	public void setResumed(boolean resumed) {
		this.resumed = resumed;
	}
	
	public void setUnblock(boolean unblock) {
		this.unblock = unblock;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setResources(IResource[] resources) {
		this.resources = resources;
	}

	public int getResolution() {
		return resolution;
	}

	public void setMyValue(String myValue) {
		this.myValue = myValue;
	}

	public void setIncomingValue(String incomingValue) {
		this.incomingValue = incomingValue;
	}

	public String getValueToUse() {
		return valueToUse;
	}
	
	public void setTextConflicts(boolean textConflicts) {
		this.textConflicts = textConflicts;
	}

	public void setPropertyConflicts(boolean propertyConflicts) {
		this.propertyConflicts = propertyConflicts;
	}
	
	public void setTreeConflicts(boolean treeConflicts) {
		this.treeConflicts = treeConflicts;
	}

}
