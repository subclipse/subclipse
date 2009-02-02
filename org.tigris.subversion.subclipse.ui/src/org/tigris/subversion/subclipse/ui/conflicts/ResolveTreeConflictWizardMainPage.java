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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
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
	
	private Button option1Button;
	private Button option2Button;
	private Button option3Button;
	private Group option1Group;
	private Group option2Group;
	
	private Text mergeTargetText;
	private Combo mergeTargetCombo;
	private Label compareLabel;
	
	private boolean markResolvedEnabled = true;
	
	private ISVNStatus copiedTo;
	private ISVNStatus remoteCopiedTo;
	private ISVNStatus[] adds;
	private IResource mergeTarget;
	private IResource theirs;
	private IResource mine;

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
			if (operation == SVNConflictDescriptor.Operation._merge) 
				remoteCopiedTo = getRemoteCopiedTo(true);
			else
				copiedTo = getCopiedTo(false);
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
			} else if (remoteCopiedTo != null) {
				mergeTarget = File2Resource.getResource(remoteCopiedTo.getFile());
				mergeTargetText.setText(remoteCopiedTo.getPath());				
			}
			else {
				setPageComplete(false);
			}
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
			if (operation == SVNConflictDescriptor.Operation._merge) {
				try {
					adds = wizard.getAdds();
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				
				mergeFromRepositoryButton = new Button(resolutionGroup, SWT.CHECK);
				mergeFromRepositoryButton.setText("Merge " + treeConflict.getResource().getName() + " r" + conflictDescriptor.getSrcLeftVersion().getPegRevision() + ":" + conflictDescriptor.getSrcRightVersion().getPegRevision() + " into:");			
				mergeFromRepositoryButton.setSelection(true);
			} else {
				remoteCopiedTo = getRemoteCopiedTo(false);
				mergeFromWorkingCopyButton = new Button(resolutionGroup, SWT.CHECK);
				mergeFromWorkingCopyButton.setText("Compare " + treeConflict.getResource().getName() + " to:");
				mergeFromWorkingCopyButton.setSelection(false);
			}
			Composite mergeTargetGroup = new Composite(resolutionGroup, SWT.NONE);
			GridLayout mergeTargetLayout = new GridLayout();
			mergeTargetLayout.numColumns = 2;
			mergeTargetGroup.setLayout(mergeTargetLayout);
			mergeTargetGroup.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
			if (adds == null || adds.length == 0) {
				mergeTargetText = new Text(mergeTargetGroup, SWT.BORDER | SWT.READ_ONLY);
				gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
				mergeTargetText.setLayoutData(gd);
			} else {
				mergeTargetCombo = new Combo(mergeTargetGroup, SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
				mergeTargetCombo.setLayoutData(gd);
				mergeTargetCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						ISVNStatus selectedAdd = adds[mergeTargetCombo.getSelectionIndex()];
						mergeTarget = File2Resource.getResource(selectedAdd.getFile());
						System.out.println(mergeTarget.getName());
					}				
				});
			}
			if (adds != null && adds.length > 0) {
				for (int i = 0; i < adds.length; i++) {
					mergeTargetCombo.add(adds[i].getPath());
				}
				mergeTargetCombo.select(0);
				mergeTarget = File2Resource.getResource(adds[0].getFile());
			} else if (remoteCopiedTo != null) {
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
							if (mergeTargetText == null)
								mergeTargetCombo.setText(mergeTarget.getLocation().toString());
							else
								mergeTargetText.setText(mergeTarget.getLocation().toString());
							setPageComplete(true);
						}
					}
				}				
			});
			
			if (operation == SVNConflictDescriptor.Operation._update) {
				compareLabel = new Label(resolutionGroup, SWT.NONE);
				compareLabel.setText("You will be prompted with the following options when the compare editor is closed:");
				compareLabel.setVisible(false);
				revertConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				revertConflictedResourceButton.setText("Revert " + treeConflict.getResource().getName() + " (conflict will be resolved)");
				revertConflictedResourceButton.setSelection(true);
				removeUnversionedConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
				removeUnversionedConflictedResourceButton.setText("Remove " + treeConflict.getResource().getName() + " from working copy");
				removeUnversionedConflictedResourceButton.setSelection(true);
				markResolvedEnabled = false;
				SelectionListener choiceListener = new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						if (mergeFromWorkingCopyButton.getSelection()) {
							compareLabel.setVisible(true);
							revertConflictedResourceButton.setEnabled(false);
							removeUnversionedConflictedResourceButton.setEnabled(false);
							markResolvedButton.setEnabled(false);
							setPageComplete(mergeTargetText.getText().length() > 0);
						} else {
							setPageComplete(true);
							compareLabel.setVisible(false);
							revertConflictedResourceButton.setEnabled(true);
							removeUnversionedConflictedResourceButton.setEnabled(true);
							markResolvedButton.setEnabled(true);
							if (revertConflictedResourceButton.getSelection()) {						
								removeUnversionedConflictedResourceButton.setEnabled(true);
								markResolvedButton.setSelection(false);
								markResolvedButton.setEnabled(false);							
							} else {
								removeUnversionedConflictedResourceButton.setSelection(false);
								removeUnversionedConflictedResourceButton.setEnabled(false);
								markResolvedButton.setEnabled(true);	
							}
						}
					}				
				};
				mergeFromWorkingCopyButton.addSelectionListener(choiceListener);
				revertConflictedResourceButton.addSelectionListener(choiceListener);
				removeUnversionedConflictedResourceButton.addSelectionListener(choiceListener);
			}
			if (operation == SVNConflictDescriptor.Operation._merge) {
				if (treeConflict.getResource().exists()) {
					deleteConflictedResourceButton = new Button(resolutionGroup, SWT.CHECK);
					deleteConflictedResourceButton.setText("Delete " + treeConflict.getResource().getName());
					deleteConflictedResourceButton.setSelection(true);
				}
				SelectionListener choiceListener = new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						if (!mergeFromRepositoryButton.getSelection()) setPageComplete(true);
						else setPageComplete(mergeTargetText.getText().length() > 0);
					}				
				};
				mergeFromRepositoryButton.addSelectionListener(choiceListener);
			}
		}
		if (reason == SVNConflictDescriptor.Reason.deleted && action == SVNConflictDescriptor.Action.delete) {
			copiedTo = getCopiedTo(true);
			remoteCopiedTo = getRemoteCopiedTo(true);
			
			theirs = null;
			mine = null;
			if (remoteCopiedTo != null) theirs = File2Resource.getResource(remoteCopiedTo.getFile());
			if (copiedTo != null) mine = File2Resource.getResource(copiedTo.getFile());
			
			if (mine != null && mine.exists()) {
				option1Button = new Button(resolutionGroup, SWT.RADIO);
				option1Button.setText("Choose " + mine.getFullPath());
				option1Button.setSelection(true);
				option1Group = new Group(resolutionGroup, SWT.NONE);
				option1Group.setText(mine.getName());
				GridLayout option1Layout = new GridLayout();
				option1Layout.numColumns = 1;
				option1Group.setLayout(option1Layout);
				option1Group.setLayoutData(
				new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));		
				if (theirs != null && theirs.exists()) {
					deleteSelectedResourceButton = new Button(option1Group, SWT.CHECK);
					deleteSelectedResourceButton.setText("Delete " + theirs.getName());
					deleteSelectedResourceButton.setSelection(true);
				} else option1Group.setVisible(false);		
			}
			
			if (theirs != null && theirs.exists()) {
				option2Button = new Button(resolutionGroup, SWT.RADIO);
				option2Button.setText("Choose " + theirs.getFullPath());
				if (option1Button == null) option2Button.setSelection(true);
				option2Group = new Group(resolutionGroup, SWT.NONE);
				option2Group.setText(theirs.getName());
				GridLayout option2Layout = new GridLayout();
				option2Layout.numColumns = 1;
				option2Group.setLayout(option2Layout);
				option2Group.setLayoutData(
				new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));		
				if (mine != null && mine.exists()) {
					ISVNLocalResource myResource = SVNWorkspaceRoot.getSVNResourceFor(mine);
					try {
						if (myResource.getStatus().isAdded()) {
							revertSelectedResourceButton = new Button(option2Group, SWT.CHECK);
							revertSelectedResourceButton.setText("Revert " + mine.getName());
							revertSelectedResourceButton.setSelection(true);
						}
					} catch (SVNException e) {
						SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
					}
					removeUnversionedSelectedResourceButton = new Button(option2Group, SWT.CHECK);
					removeUnversionedSelectedResourceButton.setText("Remove " + mine.getName() + " from working copy");
					removeUnversionedSelectedResourceButton.setSelection(true);
					option2Group.setEnabled(option1Button == null);
				} else option2Group.setVisible(false);
			}
			
			if (mine != null && mine.exists() && theirs != null && theirs.exists()) {
				option3Button = new Button(resolutionGroup, SWT.RADIO);
				option3Button.setText("Choose both (just mark conflict resolved)");
			}

			SelectionListener optionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					if (option1Button != null && option1Button.getSelection()) {
						option1Group.setEnabled(true);
						if (option2Group != null) option2Group.setEnabled(false);
					}
					if (option2Button != null && option2Button.getSelection()) {
						option2Group.setEnabled(true);
						if (option1Group != null) option1Group.setEnabled(false);
					}	
					if (option3Button.getSelection()) {
						option1Group.setEnabled(false);
						option2Group.setEnabled(false);
					}
					if (revertSelectedResourceButton != null) {
						if (revertSelectedResourceButton.getSelection()) {
							removeUnversionedSelectedResourceButton.setEnabled(true);
						} else {
							removeUnversionedSelectedResourceButton.setEnabled(false);
							removeUnversionedSelectedResourceButton.setSelection(false);
						}
					}
				}				
			};
			if (option1Button != null) option1Button.addSelectionListener(optionListener);
			if (option2Button != null) option2Button.addSelectionListener(optionListener);
			if (option3Button != null) option3Button.addSelectionListener(optionListener);
			if (revertSelectedResourceButton != null) revertSelectedResourceButton.addSelectionListener(optionListener);
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
		return deleteSelectedResourceButton != null && deleteSelectedResourceButton.isEnabled() && deleteSelectedResourceButton.getSelection();
	}
	
	public boolean getRevertSelectedResource() {
		return revertSelectedResourceButton != null && revertSelectedResourceButton.isEnabled() && revertSelectedResourceButton.getSelection();
	}
	
	public boolean getRemoveUnversionedSelectedResource() {
		return removeUnversionedSelectedResourceButton != null && removeUnversionedSelectedResourceButton.isEnabled() && removeUnversionedSelectedResourceButton.getSelection();
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
	
	public IResource getTheirs() {
		return theirs;
	}

	public IResource getMine() {
		return mine;
	}
	
	private ISVNStatus getCopiedTo(final boolean getAll) {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.setTaskName("Looking for copied-to URL");
				monitor.beginTask("Looking for copied-to URL", IProgressMonitor.UNKNOWN);
				ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
				try {
					copiedTo = wizard.getLocalCopiedTo(getAll);
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

	private ISVNStatus getRemoteCopiedTo(final boolean getAll) {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.setTaskName("Looking for copied-to URL");
				monitor.beginTask("Looking for copied-to URL", IProgressMonitor.UNKNOWN);
				ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
				try {
					remoteCopiedTo = wizard.getRemoteCopiedTo(getAll);
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
