package org.tigris.subversion.subclipse.ui.conflicts;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
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
	
	private Text mergeTargetText;
	
	private boolean markResolvedEnabled = true;
	
	private ISVNStatus copiedTo;
	private ISVNStatus remoteCopiedTo;
	private IResource mergeTarget;

	public ResolveTreeConflictWizardMainPage() {
		super("main", "Specify steps", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_RESOLVE_TREE_CONFLICT));
	}

	public void createControl(Composite parent) {
		ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
		final SVNTreeConflict treeConflict = wizard.getTreeConflict();
		
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
			copiedTo = getCopiedTo();
			mergeFromRepositoryButton = new Button(resolutionGroup, SWT.CHECK);
			mergeFromRepositoryButton.setText("Merge " + treeConflict.getResource().getName() + " r" + conflictDescriptor.getSrcLeftVersion().getPegRevision() + ":" + conflictDescriptor.getSrcRightVersion().getPegRevision() + " into:");
			Composite mergeTargetGroup = new Composite(resolutionGroup, SWT.NONE);
			GridLayout mergeTargetLayout = new GridLayout();
			mergeTargetLayout.numColumns = 2;
			mergeTargetGroup.setLayout(mergeTargetLayout);
			mergeTargetGroup.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
			mergeTargetText = new Text(mergeTargetGroup, SWT.BORDER | SWT.READ_ONLY);
			gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			mergeTargetText.setLayoutData(gd);
			if (copiedTo != null) {
				mergeTarget = File2Resource.getResource(copiedTo.getFile());
				mergeTargetText.setText(copiedTo.getPath());
			} else setPageComplete(false);
			Button selectMergeTargetButton = new Button(mergeTargetGroup, SWT.PUSH);
			selectMergeTargetButton.setText("Browse...");
			selectMergeTargetButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					ResourceSelectionDialog dialog = new ResourceSelectionDialog(getShell(), treeConflict.getResource().getProject(), "Select merge target");
					if (dialog.open() == ResourceSelectionDialog.OK) {
						Object[] selectedResources = dialog.getResult();
						if (selectedResources != null && selectedResources.length > 0 && (selectedResources[0] instanceof IResource)) {
							mergeTarget = (IResource)selectedResources[0];
							mergeTargetText.setText(mergeTarget.getLocation().toString());
							setPageComplete(true);
						}
					}
				}				
			});
			mergeFromRepositoryButton.setSelection(true);
		}
		if (reason == SVNConflictDescriptor.Reason.edited && action == SVNConflictDescriptor.Action.delete) {
			remoteCopiedTo = getRemoteCopiedTo();
			mergeFromWorkingCopyButton = new Button(resolutionGroup, SWT.CHECK);
			mergeFromWorkingCopyButton.setText("Merge " + treeConflict.getResource().getName() + " working copy changes into:");
			mergeFromWorkingCopyButton.setSelection(true);
			Composite mergeTargetGroup = new Composite(resolutionGroup, SWT.NONE);
			GridLayout mergeTargetLayout = new GridLayout();
			mergeTargetLayout.numColumns = 2;
			mergeTargetGroup.setLayout(mergeTargetLayout);
			mergeTargetGroup.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
			mergeTargetText = new Text(mergeTargetGroup, SWT.BORDER | SWT.READ_ONLY);
			gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			mergeTargetText.setLayoutData(gd);
			if (remoteCopiedTo != null) {
				mergeTarget = File2Resource.getResource(remoteCopiedTo.getFile());
				mergeTargetText.setText(remoteCopiedTo.getPath());
			} else setPageComplete(false);
			Button selectMergeTargetButton = new Button(mergeTargetGroup, SWT.PUSH);
			selectMergeTargetButton.setText("Browse...");
			selectMergeTargetButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					ResourceSelectionDialog dialog = new ResourceSelectionDialog(getShell(), treeConflict.getResource().getProject(), "Select merge target");
					if (dialog.open() == ResourceSelectionDialog.OK) {
						Object[] selectedResources = dialog.getResult();
						if (selectedResources != null && selectedResources.length > 0 && (selectedResources[0] instanceof IResource)) {
							mergeTarget = (IResource)selectedResources[0];
							mergeTargetText.setText(mergeTarget.getLocation().toString());
							setPageComplete(true);
						}
					}
				}				
			});
			
			if (operation == SVNConflictDescriptor.Operation._update) {
				revertConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				revertConflictedResourceButton.setText("Revert " + treeConflict.getResource().getName() + " (conflict will be resolved)");
				revertConflictedResourceButton.setSelection(true);
				removeUnversionedConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				removeUnversionedConflictedResourceButton.setText("Remove " + treeConflict.getResource().getName() + " from working copy");
				removeUnversionedConflictedResourceButton.setSelection(true);
				markResolvedEnabled = false;
				SelectionListener choiceListener = new SelectionAdapter() {
					public void widgetSelected(SelectionEvent arg0) {
						if (revertConflictedResourceButton.getSelection()) {						
							removeUnversionedConflictedResourceButton.setEnabled(true);
							markResolvedButton.setEnabled(false);							
						} else {
							removeUnversionedConflictedResourceButton.setSelection(false);
							removeUnversionedConflictedResourceButton.setEnabled(false);
							markResolvedButton.setEnabled(true);	
						}
					}				
				};
				revertConflictedResourceButton.addSelectionListener(choiceListener);
				removeUnversionedConflictedResourceButton.addSelectionListener(choiceListener);
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
		if (markResolvedEnabled) markResolvedButton.setSelection(true);
		else markResolvedButton.setEnabled(false);
		
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
	
	public IResource getMergeTarget() {
		return mergeTarget;
	}
	
	private ISVNStatus getCopiedTo() {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.setTaskName("Looking for copied-to URL");
				monitor.beginTask("Looking for copied-to URL", IProgressMonitor.UNKNOWN);
				ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
				try {
					copiedTo = wizard.getLocalCopiedTo();
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				monitor.done();
			}			
		};
		try {
			getContainer().run(false, false, runnable);
		} catch (Exception e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
		return copiedTo;
	}
	
//	private ISVNLogMessageChangePath getRemoteCopiedTo() {
	private ISVNStatus getRemoteCopiedTo() {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.setTaskName("Looking for copied-to URL");
				monitor.beginTask("Looking for copied-to URL", IProgressMonitor.UNKNOWN);
				ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
				try {
					remoteCopiedTo = wizard.getRemoteCopiedTo();
				} catch (Exception e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				monitor.done();
			}			
		};
		try {
			getContainer().run(false, false, runnable);
		} catch (Exception e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
		return remoteCopiedTo;		
	}

}
