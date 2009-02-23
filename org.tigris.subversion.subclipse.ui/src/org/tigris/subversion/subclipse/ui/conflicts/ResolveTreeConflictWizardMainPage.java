package org.tigris.subversion.subclipse.ui.conflicts;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ResolveTreeConflictWizardMainPage extends WizardPage {
	private Button mergeFromRepositoryButton;
	private Button compareButton;
	
	private Button revertButton;
	private Button deleteButton1;
	private Button deleteButton2;
	private Button markResolvedButton;
	
	private Button option1Button;
	private Button option2Button;
	private Button option3Button;
	private Group option1Group;
	private Group option2Group;
	
	private Text mergeTargetText;
	private Combo mergeTargetCombo;
	private Label compareLabel;
	
	private Combo revertCombo;
	
	private boolean markResolvedEnabled = true;
	
	private ISVNStatus copiedTo;
	private ISVNStatus remoteCopiedTo;
	private ISVNStatus[] adds;
	private IResource mergeTarget;
	private IResource theirs;
	private IResource mine;

	private IResource revertResource;
	private IResource deleteResource1;
	private IResource deleteResource2;
	private ISVNLocalResource svnCompareResource;

	private IResource compareResource1;
	private IResource compareResource2;
	private ISVNRemoteResource remoteResource;
	private String mergeFromUrl;
	
	private SVNTreeConflict treeConflict;
	
	public ResolveTreeConflictWizardMainPage() {
		super("main", "Specify steps", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_RESOLVE_TREE_CONFLICT));
	}

	public void createControl(Composite parent) {
		ResolveTreeConflictWizard wizard = (ResolveTreeConflictWizard)getWizard();
		treeConflict = wizard.getTreeConflict();
		
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
			getRemoteResource(wizard, treeConflict);
			compareButton = new Button(resolutionGroup, SWT.CHECK);
			compareButton.setText("Compare " + conflictDescriptor.getSrcRightVersion().getPathInRepos() + " to Compare/Merge target:");			
			compareLabel = new Label(resolutionGroup, SWT.NONE);
			compareLabel.setText("You will be prompted with the following options when the compare editor is closed:");
			compareLabel.setVisible(false);			
			mergeFromRepositoryButton = new Button(resolutionGroup, SWT.CHECK);
			mergeFromRepositoryButton.setText("Merge " + conflictDescriptor.getSrcRightVersion().getPathInRepos() + " into Compare/Merge target:");
			Group mergeTargetGroup = new Group(resolutionGroup, SWT.NONE);
			mergeTargetGroup.setText("Compare/Merge Target:");
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
				svnCompareResource =  SVNWorkspaceRoot.getSVNResourceFor(mergeTarget);
				mergeTargetText.setText(mergeTarget.getFullPath().toString());
			} else if (remoteCopiedTo != null) {
				mergeTarget = File2Resource.getResource(remoteCopiedTo.getFile());
				svnCompareResource =  SVNWorkspaceRoot.getSVNResourceFor(mergeTarget);
				mergeTargetText.setText(mergeTarget.getFullPath().toString());
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
							svnCompareResource =  SVNWorkspaceRoot.getSVNResourceFor(mergeTarget);
							mergeTargetText.setText(mergeTarget.getFullPath().toString());							
							setPageComplete(true);
						}
					}
				}				
			});
			mergeFromRepositoryButton.setSelection(true);
			SelectionListener choiceListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					if (compareButton.getSelection() || (mergeFromRepositoryButton != null && mergeFromRepositoryButton.getSelection()))
						setPageComplete((mergeTargetText != null && mergeTargetText.getText().length() > 0) || (mergeTargetCombo != null && mergeTargetCombo.getText().length() > 0));
					else
						setPageComplete(true);
					if (compareButton.getSelection()) {
						compareLabel.setVisible(true);
						mergeFromRepositoryButton.setEnabled(false);
						markResolvedButton.setEnabled(false);
					} else {
						compareLabel.setVisible(false);
						mergeFromRepositoryButton.setEnabled(true);
						markResolvedButton.setEnabled(true);
					}
				}				
			};
			compareButton.addSelectionListener(choiceListener);
			if (mergeFromRepositoryButton != null) mergeFromRepositoryButton.addSelectionListener(choiceListener);
		}
		if (reason == SVNConflictDescriptor.Reason.edited && action == SVNConflictDescriptor.Action.delete) {					
			compareButton = new Button(resolutionGroup, SWT.CHECK);
			compareButton.setText("Compare " + treeConflict.getResource().getName() + " to:");
			compareButton.setSelection(false);
			if (operation != SVNConflictDescriptor.Operation._switch) {
				compareLabel = new Label(resolutionGroup, SWT.NONE);
				compareLabel.setText("You will be prompted with the following options when the compare editor is closed:");
				compareLabel.setVisible(false);
			}
			compareResource2 = treeConflict.getResource();			
			if (operation == SVNConflictDescriptor.Operation._merge) {
				try {
					adds = wizard.getAdds();
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}				
				mergeFromRepositoryButton = new Button(resolutionGroup, SWT.CHECK);
				mergeFromRepositoryButton.setText("Merge " + treeConflict.getResource().getName() + " into:");			
				mergeFromRepositoryButton.setSelection(true);
				mergeFromUrl = wizard.getSvnResource().getUrl().toString();
			} else {
				remoteCopiedTo = getRemoteCopiedTo(false);
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
				mergeTargetCombo = new Combo(mergeTargetGroup, SWT.BORDER | SWT.READ_ONLY);
				gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
				mergeTargetCombo.setLayoutData(gd);
				mergeTargetCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						ISVNStatus selectedAdd = adds[mergeTargetCombo.getSelectionIndex()];
						mergeTarget = File2Resource.getResource(selectedAdd.getFile());
						compareResource1 = mergeTarget;
					}				
				});
			}
			if (adds != null && adds.length > 0) {
				for (int i = 0; i < adds.length; i++) {
					IResource mergeTargetResource = File2Resource.getResource(adds[i].getFile());
					mergeTargetCombo.add(mergeTargetResource.getFullPath().toString());
				}
				mergeTargetCombo.select(0);
				mergeTarget = File2Resource.getResource(adds[0].getFile());
				compareResource1 = mergeTarget;
			} else if (remoteCopiedTo != null) {
				mergeTarget = File2Resource.getResource(remoteCopiedTo.getFile());
				mergeTargetText.setText(mergeTarget.getFullPath().toString());
				compareResource1 = mergeTarget;
			} // else setPageComplete(false);
			Button selectMergeTargetButton = new Button(mergeTargetGroup, SWT.PUSH);
			selectMergeTargetButton.setText("Browse...");
			selectMergeTargetButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					ResourceSelectionDialog dialog = new ResourceSelectionDialog(getShell(), treeConflict.getResource().getProject(), "Select merge target");
					if (dialog.open() == ResourceSelectionDialog.OK) {
						Object[] selectedResources = dialog.getResult();
						if (selectedResources != null && selectedResources.length > 0 && (selectedResources[0] instanceof IResource)) {
							mergeTarget = (IResource)selectedResources[0];
							compareResource1 = mergeTarget;
							if (mergeTargetText == null)
								mergeTargetCombo.setText(mergeTarget.getFullPath().toString());
							else
								mergeTargetText.setText(mergeTarget.getFullPath().toString());
							setPageComplete(true);
						}
					}
				}				
			});
			if (operation == SVNConflictDescriptor.Operation._switch) {
				compareLabel = new Label(resolutionGroup, SWT.NONE);
				compareLabel.setText("You will be prompted with the following options when the compare editor is closed:");
				compareLabel.setVisible(false);
			}
			if (operation != SVNConflictDescriptor.Operation._merge) {
				revertResource = treeConflict.getResource();
				if (wizard.isAdded()) {
					revertButton = new Button(resolutionGroup, SWT.CHECK);
					revertButton.setText("Revert " + revertResource.getName() + " (conflict will be resolved)");
					revertButton.setSelection(true);
				}
				deleteResource1 = treeConflict.getResource();
				deleteButton1 = new Button(resolutionGroup, SWT.CHECK);
				if (wizard.isAdded())
					deleteButton1.setText("Remove " + deleteResource1.getName() + " from working copy");
				else
					deleteButton1.setText("Delete " + deleteResource1.getName());
				deleteButton1.setSelection(true);
				if (wizard.isAdded()) markResolvedEnabled = false;
				SelectionListener choiceListener = new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						if (compareButton.getSelection() || (mergeFromRepositoryButton != null && mergeFromRepositoryButton.getSelection()))
							setPageComplete((mergeTargetText != null && mergeTargetText.getText().length() > 0) || (mergeTargetCombo != null && mergeTargetCombo.getText().length() > 0));
						else
							setPageComplete(true);
						if (compareButton.getSelection()) {
							compareLabel.setVisible(true);
							if (revertButton != null) revertButton.setEnabled(false);
							deleteButton1.setEnabled(false);
							markResolvedButton.setEnabled(false);
						} else {
							compareLabel.setVisible(false);
							if (revertButton != null) {
								revertButton.setEnabled(true);
								deleteButton1.setEnabled(true);
								markResolvedButton.setEnabled(true);
								if (revertButton.getSelection()) {						
									deleteButton1.setEnabled(true);
									markResolvedButton.setSelection(false);
									markResolvedButton.setEnabled(false);							
								} else {
									deleteButton1.setSelection(false);
									deleteButton1.setEnabled(false);
									markResolvedButton.setEnabled(true);	
								}
							}
						}
					}				
				};
				compareButton.addSelectionListener(choiceListener);
				if (mergeFromRepositoryButton != null) mergeFromRepositoryButton.addSelectionListener(choiceListener);
				if (revertButton != null) revertButton.addSelectionListener(choiceListener);
				deleteButton1.addSelectionListener(choiceListener);
			}
			if (operation == SVNConflictDescriptor.Operation._merge) {
				if (treeConflict.getResource().exists()) {
					deleteResource1 = treeConflict.getResource();
					deleteButton1 = new Button(resolutionGroup, SWT.CHECK);
					deleteButton1.setText("Delete " + deleteResource1.getName());
					deleteButton1.setSelection(true);
				}
				SelectionListener choiceListener = new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						if (!compareButton.getSelection() && !mergeFromRepositoryButton.getSelection()) setPageComplete(true);
						else setPageComplete((mergeTargetText != null && mergeTargetText.getText().length() > 0) || (mergeTargetCombo != null && mergeTargetCombo.getText().length() > 0));
						if (compareButton.getSelection()) {
							compareLabel.setVisible(true);
							if (mergeFromRepositoryButton != null) mergeFromRepositoryButton.setEnabled(false);
							if (deleteButton1 != null) deleteButton1.setEnabled(false);
							if (markResolvedButton != null) markResolvedButton.setEnabled(false);
						} else {
							compareLabel.setVisible(false);
							if (mergeFromRepositoryButton != null) mergeFromRepositoryButton.setEnabled(true);
							if (deleteButton1 != null) deleteButton1.setEnabled(true);
							if (markResolvedButton != null) markResolvedButton.setEnabled(true);							
						}
					}				
				};				
				compareButton.addSelectionListener(choiceListener);
				mergeFromRepositoryButton.addSelectionListener(choiceListener);
			}
		}
		if (reason == SVNConflictDescriptor.Reason.deleted && action == SVNConflictDescriptor.Action.delete && operation != SVNConflictDescriptor.Operation._merge) {
			copiedTo = getCopiedTo(true);
			remoteCopiedTo = getRemoteCopiedTo(true);
			
			theirs = null;
			mine = null;
			if (remoteCopiedTo != null) theirs = File2Resource.getResource(remoteCopiedTo.getFile());
			if (copiedTo != null) mine = File2Resource.getResource(copiedTo.getFile());
			
			if (mine != null && mine.exists() && theirs != null && theirs.exists()) {
				compareButton = new Button(resolutionGroup, SWT.CHECK);
				compareButton.setText("Compare " + mine.getName() + " to " + theirs.getName());
				compareButton.setSelection(false);
				compareResource1 = mine;
				compareResource2 = theirs;
				compareLabel = new Label(resolutionGroup, SWT.NONE);
				compareLabel.setText("You will be prompted with the following options when the compare editor is closed:");
				compareLabel.setVisible(false);
				compareButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						compareLabel.setVisible(compareButton.getSelection());
						if (option1Button != null) option1Button.setEnabled(!compareButton.getSelection());
						if (option1Group != null) option1Group.setEnabled(!compareButton.getSelection());
						if (option2Button != null) option2Button.setEnabled(!compareButton.getSelection());
						if (option2Group != null) option2Group.setEnabled(!compareButton.getSelection());
						if (option3Button != null) option3Button.setEnabled(!compareButton.getSelection());
						markResolvedButton.setEnabled(!compareButton.getSelection());
					}				
				});
			}
			
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
					deleteResource1 = theirs;
					deleteButton1 = new Button(option1Group, SWT.CHECK);
					deleteButton1.setText("Delete " + deleteResource1.getName());
					deleteButton1.setSelection(true);
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
							revertResource = mine;
							revertButton = new Button(option2Group, SWT.CHECK);
							revertButton.setText("Revert " + mine.getName());
							revertButton.setSelection(true);
						}
					} catch (SVNException e) {
						SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
					}
					deleteResource2 = mine;
					deleteButton2 = new Button(option2Group, SWT.CHECK);
					deleteButton2.setText("Remove " + deleteResource2.getName() + " from working copy");
					deleteButton2.setSelection(true);
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
					if (revertButton != null) {
						if (revertButton.getSelection()) {
							deleteButton2.setEnabled(true);
						} else {
							deleteButton2.setEnabled(false);
							deleteButton2.setSelection(false);
						}
					}
				}				
			};
			if (option1Button != null) option1Button.addSelectionListener(optionListener);
			if (option2Button != null) option2Button.addSelectionListener(optionListener);
			if (option3Button != null) option3Button.addSelectionListener(optionListener);
			if (revertButton != null) revertButton.addSelectionListener(optionListener);
		}
		if (reason == SVNConflictDescriptor.Reason.deleted && action == SVNConflictDescriptor.Action.delete && operation == SVNConflictDescriptor.Operation._merge) {
			remoteCopiedTo = getRemoteCopiedTo(true);
			if (remoteCopiedTo != null) mine = File2Resource.getResource(remoteCopiedTo.getFile());
			if (mine != null && mine.exists()) {
				try {
					adds = wizard.getAdds();
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				option1Button = new Button(resolutionGroup, SWT.RADIO);
				option1Button.setText("Choose " + mine.getFullPath());
				option1Button.setSelection(true);
				
				
				if (adds != null && adds.length > 0) {
					option1Group = new Group(resolutionGroup, SWT.NONE);
					option1Group.setText(mine.getName());
					GridLayout option1Layout = new GridLayout();
					option1Layout.numColumns = 1;
					option1Group.setLayout(option1Layout);
					option1Group.setLayoutData(
					new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));						
					if (adds.length == 1) {
						revertResource = File2Resource.getResource(adds[0].getFile());
						revertButton = new Button(option1Group, SWT.CHECK);
						revertButton.setText("Revert " + revertResource.getName());
						revertButton.setSelection(true);
						deleteResource1 = File2Resource.getResource(adds[0].getFile());
						deleteButton1 = new Button(option1Group, SWT.CHECK);
						deleteButton1.setText("Remove " + deleteResource1.getName() + " from working copy");
						deleteButton1.setSelection(true);
					} else {
						revertButton = new Button(option1Group, SWT.CHECK);
						revertButton.setText("Revert the selected resource:");
						revertCombo = new Combo(option1Group, SWT.BORDER | SWT.READ_ONLY);
						gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
						revertCombo.setLayoutData(gd);
						revertCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								ISVNStatus selectedRevert = adds[revertCombo.getSelectionIndex()];
								revertResource = File2Resource.getResource(selectedRevert.getFile());
								deleteResource1 = revertResource;
							}				
						});
						for (int i = 0; i < adds.length; i++) {
							IResource revertResource = File2Resource.getResource(adds[i].getFile());
							revertCombo.add(revertResource.getFullPath().toString());
						}
						revertCombo.select(0);
						revertResource = File2Resource.getResource(adds[0].getFile());
						deleteResource1 = revertResource;
						deleteButton1 = new Button(option1Group, SWT.CHECK);
						deleteButton1.setText("Remove the selected resource from working copy");
						deleteButton1.setEnabled(false);
						setPageComplete(false);
					}
				}
				
				option2Button = new Button(resolutionGroup, SWT.RADIO);
				option2Group = new Group(resolutionGroup, SWT.NONE);
				GridLayout option2Layout = new GridLayout();
				option2Layout.numColumns = 1;
				option2Group.setLayout(option2Layout);
				option2Group.setLayoutData(
				new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));						
				if (adds == null || adds.length != 1) {
					option2Button.setText("Choose incoming");
					option2Group.setText("Incoming");
				} else {
					IResource addResource = File2Resource.getResource(adds[0].getFile());
					option2Button.setText("Choose " + addResource.getFullPath());
					option2Group.setText(addResource.getName());
				}
				deleteResource2 = mine;
				deleteButton2 = new Button(option2Group, SWT.CHECK);
				deleteButton2.setText("Delete " + deleteResource2.getName());
				deleteButton2.setSelection(true);
				option2Group.setEnabled(false);
				option3Button = new Button(resolutionGroup, SWT.RADIO);
				option3Button.setText("Choose both (just mark conflict resolved)");
				SelectionListener choiceListener = new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						if (option1Group != null) option1Group.setEnabled(option1Button.getSelection());
						if (option2Group != null) option2Group.setEnabled(option2Button.getSelection());
						if (revertButton != null && deleteButton1 != null) {
							if (revertButton.getSelection()) {
								deleteButton1.setEnabled(true);
							} else {
								deleteButton1.setEnabled(false);
								deleteButton1.setSelection(false);
							}
						}	
						if (option1Button.getSelection()) {
							setPageComplete(revertButton == null || revertButton.getSelection());
						}
						if (option2Button.getSelection()) {
							setPageComplete(deleteButton2 == null || deleteButton2.getSelection());
						}
						if (option3Button.getSelection()) setPageComplete(true);
					}				
				};
				option1Button.addSelectionListener(choiceListener);
				option2Button.addSelectionListener(choiceListener);
				option3Button.addSelectionListener(choiceListener);
				if (revertButton != null) revertButton.addSelectionListener(choiceListener);
				if (deleteButton2 != null) deleteButton2.addSelectionListener(choiceListener);
				if (revertButton != null) revertButton.addSelectionListener(choiceListener);
			}
		}
		if ((reason == SVNConflictDescriptor.Reason.added && action == SVNConflictDescriptor.Action.add && operation == SVNConflictDescriptor.Operation._update) || (reason == SVNConflictDescriptor.Reason.obstructed && action == SVNConflictDescriptor.Action.add && operation == SVNConflictDescriptor.Operation._merge)) {
			compareButton = new Button(resolutionGroup, SWT.CHECK);
			compareButton.setText("Compare " + treeConflict.getResource().getName() + " to " + treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos() + " in repository.");
			compareButton.setSelection(false);
			compareResource1 = treeConflict.getResource();
			
			getRemoteResource(wizard, treeConflict);
			
			compareLabel = new Label(resolutionGroup, SWT.NONE);
			compareLabel.setText("You will be prompted with the following options when the compare editor is closed:");
			compareLabel.setVisible(false);			
			SelectionListener choiceListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					if (compareButton.getSelection()) {
						compareLabel.setVisible(true);
						markResolvedButton.setEnabled(false);
					} else {
						compareLabel.setVisible(false);
						markResolvedButton.setEnabled(true);
					}
				}				
			};
			compareButton.addSelectionListener(choiceListener);			
			
		}
		markResolvedButton = new Button(resolutionGroup, SWT.CHECK);
		markResolvedButton.setText("Mark conflict resolved");
		if (markResolvedEnabled) markResolvedButton.setSelection(true);
		else markResolvedButton.setEnabled(false);
		
		setMessage("Specify the steps to take to resolve the tree conflict");
		
		setControl(outerContainer);	
	}

	private void getRemoteResource(ResolveTreeConflictWizard wizard,
			final SVNTreeConflict treeConflict) {
		ISVNRepositoryLocation repository = wizard.getSvnResource().getRepository();
		SVNRevision revision = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
		try {
			SVNUrl url = new SVNUrl(treeConflict.getConflictDescriptor().getSrcRightVersion().getReposURL() + "/" + treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos());
			GetRemoteResourceCommand command = new GetRemoteResourceCommand(repository, url, revision);
			command.run(new NullProgressMonitor());
			remoteResource = command.getRemoteResource();
		} catch (Exception e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
	}
	
	public boolean getMergeFromRepository() {
		if (compareButton != null && compareButton.getSelection()) return false;
		return mergeFromRepositoryButton != null && mergeFromRepositoryButton.getSelection();
	}
	
	public boolean getCompare() {
		return compareButton != null && compareButton.getSelection();
	}

	public boolean getMarkResolved() {
		return !getCompare() && markResolvedButton.getSelection();
	}
	
	public IResource getMergeTarget() {
		return mergeTarget;
	}

	public IResource getRevertResource() {
		if (getCompare()) return null;
		if (revertButton != null && revertButton.isEnabled() && revertButton.getSelection())
			return revertResource;
		else
			return null;
	}
	
	public IResource getDeleteResource() {
		if (getCompare()) return null;
		if (deleteButton1 != null && deleteButton1.isEnabled() && deleteButton1.getSelection())
			return deleteResource1;
		if (deleteButton2 != null && deleteButton2.isEnabled() && deleteButton2.getSelection())
			return deleteResource2;		
		return null;
	}
	
	public IResource getCompareResource1() {
		return compareResource1;
	}

	public IResource getCompareResource2() {
		return compareResource2;
	}
	
	public ISVNRemoteResource getRemoteResource() {
		return remoteResource;
	}
	
	public ISVNLocalResource getSvnCompareResource() {
		return svnCompareResource;
	}
	
	public String getMergeFromUrl() {
		if (mergeFromUrl == null)
			return treeConflict.getConflictDescriptor().getSrcRightVersion().getReposURL() + "/" + treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos();
		else
			return mergeFromUrl;
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
