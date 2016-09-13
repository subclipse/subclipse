package org.tigris.subversion.subclipse.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class UpdateToHeadPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Button ignoreExternalsButton;
	private Button forceButton;
	
	private Button textConflictPromptButton;
	private Button textConflictMarkButton;
	
	private Button propertyConflictPromptButton;
	private Button propertyConflictMarkButton;

	private Button binaryConflictPromptButton;
	private Button binaryConflictMarkButton;
	private Button binaryConflictUserButton;
	private Button binaryConflictIncomingButton;
	
	private Button treeConflictPromptButton;
	private Button treeConflictMarkButton;
	private Button treeConflictUserButton;
	private Button treeConflictResolveButton;
	
	private IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		ignoreExternalsButton = new Button(composite, SWT.CHECK);
		ignoreExternalsButton.setText(Policy.bind("SvnDialog.ignoreExternals")); //$NON-NLS-1$
		data = new GridData();
		ignoreExternalsButton.setLayoutData(data);
		
		forceButton = new Button(composite, SWT.CHECK);
		forceButton.setText(Policy.bind("SvnDialog.force")); //$NON-NLS-1$
		data = new GridData();
		forceButton.setLayoutData(data);
		
		Group conflictGroup = new Group(composite, SWT.NONE);
		conflictGroup.setText(Policy.bind("SvnWizardUpdatePage.0")); //$NON-NLS-1$
		GridLayout conflictLayout = new GridLayout();
		conflictLayout.numColumns = 1;
		conflictGroup.setLayout(conflictLayout);
		conflictGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Group textGroup = new Group(conflictGroup, SWT.NONE);
		textGroup.setText(Policy.bind("SvnWizardUpdatePage.1")); //$NON-NLS-1$
		GridLayout textLayout = new GridLayout();
		textLayout.numColumns = 1;
		textGroup.setLayout(textLayout);
		textGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		textConflictPromptButton = new Button(textGroup, SWT.RADIO);
		textConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.2")); //$NON-NLS-1$
		textConflictMarkButton = new Button(textGroup, SWT.RADIO);
		textConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.3")); //$NON-NLS-1$
		
		Group binaryGroup = new Group(conflictGroup, SWT.NONE);
		binaryGroup.setText(Policy.bind("SvnWizardUpdatePage.4")); //$NON-NLS-1$
		GridLayout binaryLayout = new GridLayout();
		binaryLayout.numColumns = 1;
		binaryGroup.setLayout(binaryLayout);
		binaryGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		binaryConflictPromptButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.5")); //$NON-NLS-1$
		binaryConflictMarkButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.6")); //$NON-NLS-1$
		binaryConflictUserButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictUserButton.setText(Policy.bind("SvnWizardUpdatePage.7")); //$NON-NLS-1$
		binaryConflictIncomingButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictIncomingButton.setText(Policy.bind("SvnWizardUpdatePage.8")); //$NON-NLS-1$

		Group propertyGroup = new Group(conflictGroup, SWT.NONE);
		propertyGroup.setText(Policy.bind("SvnWizardUpdatePage.9")); //$NON-NLS-1$
		GridLayout propertyLayout = new GridLayout();
		propertyLayout.numColumns = 1;
		propertyGroup.setLayout(propertyLayout);
		propertyGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));			

		propertyConflictPromptButton = new Button(propertyGroup, SWT.RADIO);
		propertyConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.10")); //$NON-NLS-1$
		propertyConflictMarkButton = new Button(propertyGroup, SWT.RADIO);
		propertyConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.11")); //$NON-NLS-1$
		
		Group treeConflictGroup = new Group(conflictGroup, SWT.NONE);
		treeConflictGroup.setText(Policy.bind("SvnWizardUpdatePage.12")); //$NON-NLS-1$
		GridLayout treeConflictLayout = new GridLayout();
		treeConflictLayout.numColumns = 1;
		treeConflictGroup.setLayout(treeConflictLayout);
		treeConflictGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		treeConflictPromptButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.10")); //$NON-NLS-1$
		treeConflictMarkButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.11")); //$NON-NLS-1$
		treeConflictUserButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictUserButton.setText(Policy.bind("SvnWizardUpdatePage.13")); //$NON-NLS-1$
		treeConflictResolveButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictResolveButton.setText(Policy.bind("SvnWizardUpdatePage.14")); //$NON-NLS-1$
		
		initializeValues();
		
		return composite;
	}	

	@Override
	public boolean performOk() {
		store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_IGNORE_EXTERNALS, ignoreExternalsButton.getSelection());
		store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_ALLOW_UNVERSIONED_OBSTRUCTIONS, forceButton.getSelection());
		
		if (textConflictMarkButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TEXT_FILES, ISVNConflictResolver.Choice.postpone);
		else store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TEXT_FILES, ISVNConflictResolver.Choice.chooseMerged);
		
		if (binaryConflictIncomingButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES, ISVNConflictResolver.Choice.chooseTheirsFull);
		else if (binaryConflictUserButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES, ISVNConflictResolver.Choice.chooseMineFull);
		else if (binaryConflictMarkButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES, ISVNConflictResolver.Choice.postpone);
		else store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES, ISVNConflictResolver.Choice.chooseMerged);		
		
		if (propertyConflictMarkButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_PROPERTIES, ISVNConflictResolver.Choice.postpone);
		else store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_PROPERTIES, ISVNConflictResolver.Choice.chooseMerged);
		
		if (treeConflictMarkButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS, ISVNConflictResolver.Choice.postpone);
		else if (treeConflictResolveButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS, ISVNConflictResolver.Choice.chooseMerged);
		else if (treeConflictUserButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS, ISVNConflictResolver.Choice.chooseMine);
		else if (treeConflictPromptButton.getSelection()) store.setValue(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS, SVNConflictResolver.PROMPT);
		
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		ignoreExternalsButton.setSelection(false);
		forceButton.setSelection(true);
		textConflictMarkButton.setSelection(true);
		textConflictPromptButton.setSelection(false);
		binaryConflictMarkButton.setSelection(true);
		binaryConflictPromptButton.setSelection(false);
		binaryConflictIncomingButton.setSelection(false);
		binaryConflictUserButton.setSelection(false);
		propertyConflictMarkButton.setSelection(true);
		propertyConflictPromptButton.setSelection(false);
		treeConflictMarkButton.setSelection(true);
		treeConflictPromptButton.setSelection(false);
		treeConflictResolveButton.setSelection(false);
		treeConflictUserButton.setSelection(false);
		super.performDefaults();
	}

	private void initializeValues() {
		ignoreExternalsButton.setSelection(store.getBoolean(ISVNUIConstants.PREF_UPDATE_TO_HEAD_IGNORE_EXTERNALS));
		forceButton.setSelection(store.getBoolean(ISVNUIConstants.PREF_UPDATE_TO_HEAD_ALLOW_UNVERSIONED_OBSTRUCTIONS));
		switch (store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TEXT_FILES)) {
		case ISVNConflictResolver.Choice.chooseMerged:
			textConflictPromptButton.setSelection(true);
			break;
		default:
			textConflictMarkButton.setSelection(true);
			break;
		}
		switch (store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES)) {
		case ISVNConflictResolver.Choice.chooseMerged:
			binaryConflictPromptButton.setSelection(true);
			break;
		case ISVNConflictResolver.Choice.chooseTheirsFull:
			binaryConflictIncomingButton.setSelection(true);
			break;	
		case ISVNConflictResolver.Choice.chooseMineFull:
			binaryConflictUserButton.setSelection(true);
			break;				
		default:
			binaryConflictMarkButton.setSelection(true);
			break;
		}		
		switch (store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_PROPERTIES)) {
		case ISVNConflictResolver.Choice.chooseMerged:
			propertyConflictPromptButton.setSelection(true);
			break;
		default:
			propertyConflictMarkButton.setSelection(true);
			break;
		}
		switch (store.getInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS)) {
		case SVNConflictResolver.PROMPT:
			binaryConflictPromptButton.setSelection(true);
			break;
		case ISVNConflictResolver.Choice.chooseMerged:
			treeConflictResolveButton.setSelection(true);
			break;	
		case ISVNConflictResolver.Choice.chooseMine:
			treeConflictUserButton.setSelection(true);
			break;				
		default:
			treeConflictMarkButton.setSelection(true);
			break;
		}		
	}

	public void init(IWorkbench workbench) {}

}
