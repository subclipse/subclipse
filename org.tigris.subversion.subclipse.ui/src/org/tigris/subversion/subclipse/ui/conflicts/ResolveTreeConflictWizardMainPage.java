package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class ResolveTreeConflictWizardMainPage extends WizardPage {
	private Button mergeFromRepositoryButton;
	private Button mergeFromWorkingCopyButton;
	private Button revertConflictedResourceButton;
	private Button removeUnversionedConflictedResourceButton;
	private Button deleteSelectedResourceButton;
	private Button revertSelectedResourceButton;
	private Button removeUnversionedSelectedResourceButton;
	private Button deleteConflictedResourceButton;
	private Button markResolvedButton;

	public ResolveTreeConflictWizardMainPage() {
		super("main", "Specify steps", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_RESOLVE_TREE_CONFLICT));
	}

	public void createControl(Composite parent) {
		ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
		SVNTreeConflict treeConflict = wizard.getTreeConflict();
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		outerContainer.setLayout(new GridLayout());
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	

		Group detailsGroup = new Group(outerContainer, SWT.NONE);
		detailsGroup.setText("Conflict description:");
		GridLayout detailsLayout = new GridLayout();
		detailsLayout.numColumns = 2;
		detailsGroup.setLayout(detailsLayout);
		detailsGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));		
		
		Label label = new Label(detailsGroup, SWT.NONE);
		label.setText(treeConflict.getDescription());
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		label = new Label(detailsGroup, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		label = new Label(detailsGroup, SWT.NONE);
		label.setText("Source left: ");
		label = new Label(detailsGroup, SWT.WRAP);
		label.setText(treeConflict.getConflictDescriptor().getSrcLeftVersion().toString());
		gd = new GridData();
		gd.widthHint = 500;
		label.setLayoutData(gd);
		
		label = new Label(detailsGroup, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		label = new Label(detailsGroup, SWT.NONE);
		label.setText("Source right: ");
		label = new Label(detailsGroup, SWT.WRAP);
		label.setText(treeConflict.getConflictDescriptor().getSrcRightVersion().toString());
		gd = new GridData();
		gd.widthHint = 500;
		label.setLayoutData(gd);
		
		Group resolutionGroup = new Group(outerContainer, SWT.NONE);
		resolutionGroup.setText("Resolution steps:");
		GridLayout resolutionLayout = new GridLayout();
		resolutionLayout.numColumns = 1;
		resolutionGroup.setLayout(resolutionLayout);
		resolutionGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));		
		
		SVNConflictDescriptor conflictDescriptor = treeConflict.getConflictDescriptor();
		int reason = conflictDescriptor.getReason();
		int action = conflictDescriptor.getAction();
		int operation = conflictDescriptor.getOperation();
		if ((reason == SVNConflictDescriptor.Reason.deleted || reason == SVNConflictDescriptor.Reason.missing) && action == SVNConflictDescriptor.Action.edit) {
			mergeFromRepositoryButton = new Button(resolutionGroup, SWT.CHECK);
			mergeFromRepositoryButton.setText("Merge " + treeConflict.getResource().getName() + "@" + conflictDescriptor.getSrcRightVersion().getPegRevision() + " into selected working copy resource");
			mergeFromRepositoryButton.setSelection(true);
		}
		if (reason == SVNConflictDescriptor.Reason.edited && action == SVNConflictDescriptor.Action.delete) {
			mergeFromWorkingCopyButton = new Button(resolutionGroup, SWT.CHECK);
			mergeFromWorkingCopyButton.setText("Merge " + treeConflict.getResource().getName() + " from working copy into selected working copy resource");
			mergeFromWorkingCopyButton.setSelection(true);
			if (operation == SVNConflictDescriptor.Operation._update) {
				revertConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				revertConflictedResourceButton.setText("Revert " + treeConflict.getResource().getName());
				revertConflictedResourceButton.setSelection(true);
				removeUnversionedConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				// TODO:  Only allow selection of remove button if revert button selected.
				removeUnversionedConflictedResourceButton.setText("Remove " + treeConflict.getResource().getName() + " from working copy");
				removeUnversionedConflictedResourceButton.setSelection(true);
			}
			if (operation == SVNConflictDescriptor.Operation._merge) {
				deleteConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				deleteConflictedResourceButton.setText("Delete " + treeConflict.getResource().getName());
				deleteConflictedResourceButton.setSelection(true);
			}
		}
		if (reason == SVNConflictDescriptor.Reason.deleted && action == SVNConflictDescriptor.Action.delete) {
			deleteSelectedResourceButton = new Button(resolutionGroup, SWT.CHECK);
			deleteSelectedResourceButton.setText("Delete selected resource");
			deleteSelectedResourceButton.setSelection(true);
			revertSelectedResourceButton = new Button(resolutionGroup, SWT.CHECK);
			revertSelectedResourceButton.setText("Revert selected resource");
			revertSelectedResourceButton.setSelection(true);
			removeUnversionedSelectedResourceButton = new Button(resolutionGroup, SWT.CHECK);
			removeUnversionedSelectedResourceButton.setText("Remove selected resource from working copy");
			removeUnversionedSelectedResourceButton.setSelection(true);
		}
		markResolvedButton = new Button(resolutionGroup, SWT.CHECK);
		markResolvedButton.setText("Mark conflict resolved");
		markResolvedButton.setSelection(true);
		
		setMessage("Specify the steps to take to resolve the tree conflict");
		
		setControl(outerContainer);	
	}
	
	public boolean getMergeFromRepository() {
		return mergeFromRepositoryButton != null && mergeFromRepositoryButton.getSelection();
	}
	
	public boolean getMergeFromWorkingCopy() {
		return mergeFromWorkingCopyButton != null && mergeFromWorkingCopyButton.getSelection();
	}
	
	public boolean getRevertConflictedResource() {
		return revertConflictedResourceButton != null && revertConflictedResourceButton.getSelection();
	}
	
	public boolean getRemoveUnversionedConflictedResource() {
		return removeUnversionedConflictedResourceButton != null && removeUnversionedConflictedResourceButton.getSelection();
	}
	
	public boolean getDeleteSelectedResource() {
		return deleteSelectedResourceButton != null && deleteSelectedResourceButton.getSelection();
	}
	
	public boolean getRevertSelectedResource() {
		return revertSelectedResourceButton != null && revertSelectedResourceButton.getSelection();
	}
	
	public boolean getRemoveUnversionedSelectedResource() {
		return removeUnversionedSelectedResourceButton != null && removeUnversionedSelectedResourceButton.getSelection();
	}
	
	public boolean getDeleteConflictedResource() {
		return deleteConflictedResourceButton != null && deleteConflictedResourceButton.getSelection();
	}
	
	public boolean getMarkResolved() {
		return markResolvedButton.getSelection();
	}

}
