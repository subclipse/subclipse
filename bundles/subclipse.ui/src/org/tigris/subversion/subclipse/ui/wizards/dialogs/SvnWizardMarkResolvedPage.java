package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class SvnWizardMarkResolvedPage extends SvnWizardDialogPage {
	private IResource[] resources;
	private Button markResolvedButton;
	private Button chooseUserVersionButton;
	private Button chooseUserVersionForConflictsButton;
	private Button chooseIncomingVersionButton;
	private Button chooseIncomingVersionForConflictsButton;
	private Button chooseBaseVersionButton;
	private IDialogSettings settings;
	private int resolution;
	private boolean propertyConflicts;
	private boolean treeConflicts;

	private final static String LAST_CHOICE = "ResolveConflictDialog.lastChoice"; //$NON-NLS-1$
	
	public SvnWizardMarkResolvedPage(IResource[] resources) {
		super("MarkResolvedDialog", Policy.bind("ResolveOperation.taskName")); //$NON-NLS-1$ //$NON-NLS-2$
		this.resources = resources;	
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {}

	public void createControls(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);	
		
		Label label = new Label(composite, SWT.WRAP);
		if (resources.length == 1)
			label.setText(Policy.bind("ResolveDialog.file", resources[0].getFullPath().makeRelative().toOSString())); //$NON-NLS-1$
		else
			label.setText(Policy.bind("ResolveDialog.multipleFiles")); //$NON-NLS-1$
		data = new GridData();
		data.widthHint = 500;
		label.setLayoutData(data);
		
		if (treeConflicts) {
			new Label(composite, SWT.NONE);
			Label propertyLabel1 = new Label(composite, SWT.WRAP);
			if (resources.length > 1) propertyLabel1.setText(Policy.bind("ResolveDialog.treeConflictMultiple")); //$NON-NLS-1$
			else propertyLabel1.setText(Policy.bind("ResolveDialog.treeConflict")); //$NON-NLS-1$
			data = new GridData();
			data.widthHint = 500;
			propertyLabel1.setLayoutData(data);			
		}
		else if (propertyConflicts) {
			new Label(composite, SWT.NONE);
			Label propertyLabel1 = new Label(composite, SWT.WRAP);
			if (resources.length > 1) propertyLabel1.setText(Policy.bind("ResolveDialog.propertyConflictMultiple")); //$NON-NLS-1$
			else propertyLabel1.setText(Policy.bind("ResolveDialog.propertyConflict")); //$NON-NLS-1$
			data = new GridData();
			data.widthHint = 500;
			propertyLabel1.setLayoutData(data);
		}
		
		new Label(composite, SWT.NONE);
		
		Group conflictGroup = new Group(composite, SWT.NULL);
		
		conflictGroup.setText(Policy.bind("ResolveDialog.question")); //$NON-NLS-1$
		GridLayout conflictLayout = new GridLayout();
		conflictLayout.numColumns = 1;
		conflictGroup.setLayout(conflictLayout);
		data = new GridData(GridData.FILL_BOTH);
		conflictGroup.setLayoutData(data);	

		markResolvedButton = new Button(conflictGroup, SWT.RADIO);
		markResolvedButton.setText(Policy.bind("ResolveDialog.resolved")); //$NON-NLS-1$	
		if (treeConflicts) {
			Label propertyLabel2 = new Label(conflictGroup, SWT.NONE);
			propertyLabel2.setText(Policy.bind("ResolveDialog.nonTreeOnly")); //$NON-NLS-1$				
		}
		else if (propertyConflicts) {
			Label propertyLabel2 = new Label(conflictGroup, SWT.NONE);
			propertyLabel2.setText(Policy.bind("ResolveDialog.nonPropertyOnly")); //$NON-NLS-1$	
		}		
		chooseUserVersionButton = new Button(conflictGroup, SWT.RADIO);
		chooseUserVersionButton.setText(Policy.bind("ResolveDialog.useMine")); //$NON-NLS-1$
		if (!propertyConflicts) {
			chooseUserVersionForConflictsButton = new Button(conflictGroup, SWT.RADIO);
			chooseUserVersionForConflictsButton.setText("Resolve conflicts in local file with my changes.");			
		}
		chooseIncomingVersionButton = new Button(conflictGroup, SWT.RADIO);
		chooseIncomingVersionButton.setText(Policy.bind("ResolveDialog.useTheirs")); //$NON-NLS-1$	
		if (!propertyConflicts) {
			chooseIncomingVersionForConflictsButton = new Button(conflictGroup, SWT.RADIO);
			chooseIncomingVersionForConflictsButton.setText("Resolve conflicts in local file with changes from incoming file.");
		}
		chooseBaseVersionButton = new Button(conflictGroup, SWT.RADIO);
		chooseBaseVersionButton.setText(Policy.bind("ResolveDialog.useBase")); //$NON-NLS-1$

		int lastChoice = ISVNConflictResolver.Choice.chooseMerged;
		try {
			lastChoice = settings.getInt(LAST_CHOICE);
		} catch (Exception e) {}
		if (lastChoice == ISVNConflictResolver.Choice.chooseMerged) markResolvedButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseMine && chooseUserVersionForConflictsButton != null) chooseUserVersionForConflictsButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseMineFull && chooseUserVersionButton != null) chooseUserVersionButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseTheirs && chooseIncomingVersionForConflictsButton != null) chooseIncomingVersionForConflictsButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseTheirsFull && chooseIncomingVersionButton != null) chooseIncomingVersionButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseBase && chooseBaseVersionButton != null) chooseBaseVersionButton.setSelection(true);	

		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (markResolvedButton.getSelection()) settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
				else if (chooseUserVersionButton.getSelection()) settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseMineFull);
				else if (chooseUserVersionForConflictsButton != null && chooseUserVersionForConflictsButton.getSelection()) settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseMine);
				else if (chooseIncomingVersionButton.getSelection()) settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseTheirsFull);
				else if (chooseIncomingVersionForConflictsButton != null && chooseIncomingVersionForConflictsButton.getSelection()) settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseTheirs);
				else if (chooseBaseVersionButton.getSelection()) settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseBase);
			}			
		};	
		markResolvedButton.addSelectionListener(selectionListener);
		chooseUserVersionButton.addSelectionListener(selectionListener);
		if (chooseUserVersionForConflictsButton != null) chooseUserVersionForConflictsButton.addSelectionListener(selectionListener);
		chooseIncomingVersionButton.addSelectionListener(selectionListener);
		if (chooseIncomingVersionForConflictsButton != null) chooseIncomingVersionForConflictsButton.addSelectionListener(selectionListener);
		chooseBaseVersionButton.addSelectionListener(selectionListener);
		
		setControl(outerContainer);		
	}

	public String getWindowTitle() {
		return Policy.bind("ResolveOperation.taskName"); //$NON-NLS-1$
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		resolution = ISVNConflictResolver.Choice.postpone;
		if (markResolvedButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseMerged;
		else if (chooseIncomingVersionButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseTheirsFull;
		else if (chooseUserVersionButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseMineFull;
		else if (chooseBaseVersionButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseBase;
		else if (chooseUserVersionForConflictsButton != null && chooseUserVersionForConflictsButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseMine;
		else if (chooseIncomingVersionForConflictsButton != null && chooseIncomingVersionForConflictsButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseTheirs;
		return true;
	}

	public void saveSettings() {}

	public void setMessage() {
		if (resources.length == 1)
			setMessage(Policy.bind("ResolveDialog.message", resources[0].getName())); //$NON-NLS-1$
		else 
			setMessage(Policy.bind("ResolveDialog.messageMultiple")); //$NON-NLS-1$
	}
	
	public int getResolution() {
		return resolution;
	}

	public void setPropertyConflicts(boolean propertyConflicts) {
		this.propertyConflicts = propertyConflicts;
	}
	
	public void setTreeConflicts(boolean treeConflicts) {
		this.treeConflicts = treeConflicts;
	}

}
