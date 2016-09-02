/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.clientadapter.Activator;
import org.tigris.subversion.clientadapter.ISVNClientWrapper;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.internal.SWTUtils;

/**
 * SVN Preference Page
 * 
 * Allows the configuration of SVN specific options.
 * 
 */
public class SVNPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button showCompareRevisionInDialog;
    private Button fetchChangePathOnDemand;
    private Button showTagsInRemoteHistory;
    private Button showOutOfDateFolders;
    private Button useJavaHLCommitHack;
    private Button shareNestedProjects;
    private Button warnOnCommitToTagPath;
    private Button ignoreHiddenChanges;
    private Button ignoreManagedDerivedResources;
    private Button removeOnReplace;
    private Text logEntriesToFetchText;
    private Button defaultConfigLocationRadio;
    private Button useDirectoryLocationRadio;
    private Text directoryLocationText;
    private Button browseConfigDirButton;
    
	private Button quickDiffAnnotateYes;
	private Button quickDiffAnnotateNo;
	private Button quickDiffAnnotatePrompt;

	private Button resourcesWithErrorsYes;
	private Button resourcesWithErrorsNo;
	private Button resourcesWithErrorsPrompt;
	
	private Button resourcesWithWarningsYes;
	private Button resourcesWithWarningsNo;
	private Button resourcesWithWarningsPrompt;
	
	protected final ArrayList fFields;
	private String [] CLIENT_VALUES;
	private String [] CLIENT_LABELS;

	public SVNPreferencesPage() {
		fFields = new ArrayList();
		// sort the options by display text
		setDescription(Policy.bind("SVNPreferencePage.description")); //$NON-NLS-1$
		ISVNClientWrapper[] clients = null;
		clients = Activator.getDefault().getAllClientWrappers();
		if (clients != null) {
			CLIENT_LABELS = new String[clients.length];
			CLIENT_VALUES = new String[clients.length];
			for (int i = 0; i < clients.length; i++) {
				CLIENT_LABELS[i] = clients[i].getDisplayName();
				CLIENT_VALUES[i] = clients[i].getAdapterID();
			}
		} else {
			CLIENT_LABELS = new String[0];
			CLIENT_VALUES = new String[0];
		}
	}

	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	private Label createLabel(Composite parent, String text, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/**
	 * Creates an new checkbox instance and sets the default
	 * layout data.
	 *
	 * @param group  the composite in which to create the checkbox
	 * @param label  the string to set into the checkbox
	 * @return the new checkbox
	 */
	private Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		return button;
	}	

    private Button createRadio(Composite group, String label, int horizontalSpan) {
        Button button = new Button(group, SWT.RADIO);
        button.setText(label);
        GridData data = new GridData();
        data.horizontalSpan = horizontalSpan;
        button.setLayoutData(data);
        return button;
    }
    
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		// create the composite
		Composite composite = new Composite(parent, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        composite.setLayoutData(gridData);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		
		showCompareRevisionInDialog = createCheckBox(composite, Policy.bind("SVNPreferencePage.showCompareMergeInSync")); //$NON-NLS-1$
		
//		showUnadded = createCheckBox(composite, Policy.bind("SVNPreferencePage.showUnadded")); //$NON-NLS-1$
//		
//		selectUnadded = createCheckBox(composite, Policy.bind("SVNPreferencePage.selectUnadded")); //$NON-NLS-1$
		
		removeOnReplace = createCheckBox(composite, Policy.bind("SVNPreferencePage.removeOnReplace")); //$NON-NLS-1$
		
		fetchChangePathOnDemand = createCheckBox(composite, Policy.bind("SVNPreferencePage.fetchChangePathOnDemand")); //$NON-NLS-1$
		
		showTagsInRemoteHistory = createCheckBox(composite, Policy.bind("SVNPreferencePage.showTags")); //$NON-NLS-1$
		
		showOutOfDateFolders = createCheckBox(composite, Policy.bind("SVNPreferencePage.showOutOfDateFolders")); //$NON-NLS-1$
		
		useJavaHLCommitHack = createCheckBox(composite, Policy.bind("SVNPreferencePage.useJavaHLCommitHack")); //$NON-NLS-1$
		
		shareNestedProjects = createCheckBox(composite, Policy.bind("SVNPreferencePage.shareNestedProjects")); //$NON-NLS-1$

		warnOnCommitToTagPath = createCheckBox(composite, Policy.bind("SVNPreferencePage.warnOnCommitToTagPath")); //$NON-NLS-1$
		
		ignoreHiddenChanges = createCheckBox(composite, Policy.bind("SVNPreferencesPage.0")); //$NON-NLS-1$
		
		boolean isHiddenSupported;
		try {
			isHiddenSupported = Util.isHiddenSupported();
		} catch (NoSuchMethodException e1) {
			isHiddenSupported = false;
		}
		ignoreHiddenChanges.setVisible(isHiddenSupported);
		
		ignoreManagedDerivedResources = createCheckBox(composite, Policy.bind("SVNPreferencesPage.1")); //$NON-NLS-1$
		
		createLabel(composite, "", 2); //$NON-NLS-1$
		
		createLabel(composite, Policy.bind("SVNPreferencePage.logEntriesToFetch"), 1); //$NON-NLS-1$
		logEntriesToFetchText = new Text(composite, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 40;
		logEntriesToFetchText.setLayoutData(gridData);
		
		createLabel(composite, "", 2); //$NON-NLS-1$
		
		Group group = new Group(composite, SWT.NONE);
		group.setText(Policy.bind("SVNPreferencePage.useQuickdiffAnnotateGroup")); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, true));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		quickDiffAnnotateYes = createRadio(group, Policy.bind("yes"), 1); //$NON-NLS-1$
		quickDiffAnnotateNo = createRadio(group, Policy.bind("no"), 1); //$NON-NLS-1$
		quickDiffAnnotatePrompt = createRadio(group, Policy.bind("prompt"), 1); //$NON-NLS-1$
		
		Group groupErrors = new Group(composite, SWT.NONE);
		groupErrors.setText(Policy.bind("SVNPreferencePage.commitWithErrors")); //$NON-NLS-1$
		groupErrors.setLayout(new GridLayout(3, true));
		groupErrors.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		resourcesWithErrorsYes = createRadio(groupErrors, Policy.bind("yes"), 1); //$NON-NLS-1$
		resourcesWithErrorsNo = createRadio(groupErrors, Policy.bind("no"), 1); //$NON-NLS-1$
		resourcesWithErrorsPrompt = createRadio(groupErrors, Policy.bind("prompt"), 1); //$NON-NLS-1$
		
		Group groupWarnings = new Group(composite, SWT.NONE);
		groupWarnings.setText(Policy.bind("SVNPreferencePage.commitWithWarnings")); //$NON-NLS-1$
		groupWarnings.setLayout(new GridLayout(3, true));
		groupWarnings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		resourcesWithWarningsYes = createRadio(groupWarnings, Policy.bind("yes"), 1); //$NON-NLS-1$
		resourcesWithWarningsNo = createRadio(groupWarnings, Policy.bind("no"), 1); //$NON-NLS-1$
		resourcesWithWarningsPrompt = createRadio(groupWarnings, Policy.bind("prompt"), 1); //$NON-NLS-1$
		
		createLabel(composite, "", 2); //$NON-NLS-1$
		
		// group javahl/command line
        group = new Group(composite, SWT.NULL);
        group.setText(Policy.bind("SVNPreferencePage.svnClientInterface")); //$NON-NLS-1$
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout); 
		new StringComboBox(
				group, 
				ISVNUIConstants.PREF_SVNINTERFACE, 
				Policy.bind("SVNPreferencePage.client"),   //$NON-NLS-1$
				"",  //$NON-NLS-1$
				CLIENT_LABELS, CLIENT_VALUES);
		
        createLabel(composite, "", 2); //$NON-NLS-1$
        
        // group for config location
        group = new Group(composite, SWT.NULL);
        group.setText(Policy.bind("SVNPreferencePage.configurationLocation")); //$NON-NLS-1$
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        layout = new GridLayout();
        layout.numColumns = 3;
        group.setLayout(layout); 
        defaultConfigLocationRadio = createRadio(group,
                Policy.bind("SVNPreferencePage.useDefaultConfigLocation"),3); //$NON-NLS-1$ 
        useDirectoryLocationRadio = createRadio(group,
                Policy.bind("SVNPreferencePage.useDirectoryConfig"),1); //$NON-NLS-1$
        directoryLocationText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.widthHint = 200;
        gridData.grabExcessHorizontalSpace = true;
        directoryLocationText.setLayoutData(gridData);
        directoryLocationText.setEditable(false);
        browseConfigDirButton = new Button(group, SWT.NONE);
        browseConfigDirButton.setText(Policy.bind("SVNPreferencePage.browseDirectory")); //$NON-NLS-1$

        Listener configUpdateEnablements = new Listener() {
            public void handleEvent(Event event) {
                browseConfigDirButton.setEnabled(useDirectoryLocationRadio.getSelection());
                verifyValidation();
            }
        };
        defaultConfigLocationRadio.addListener(SWT.Selection,configUpdateEnablements);
        useDirectoryLocationRadio.addListener(SWT.Selection,configUpdateEnablements);
        browseConfigDirButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent event) {
                DirectoryDialog directoryDialog = new DirectoryDialog(getShell(),SWT.OPEN);
                String res = directoryDialog.open();
                if (res != null) {
                    directoryLocationText.setText(res);
                }
                verifyValidation();
            }
        });
        
        
		initializeValues();
		verifyValidation();
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		logEntriesToFetchText.addFocusListener(focusListener);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.SVN_PREFERENCE_DIALOG);

		return composite;
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();
		for (Iterator iter = fFields.iterator(); iter.hasNext();) {
			((Field)iter.next()).initializeValue(store);
		}
	
		showCompareRevisionInDialog.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG));
		
		fetchChangePathOnDemand.setSelection(store.getBoolean(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND));
		
		showTagsInRemoteHistory.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE));
		
		showOutOfDateFolders.setSelection(SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHOW_OUT_OF_DATE_FOLDERS));

		useJavaHLCommitHack.setSelection(store.getBoolean(ISVNUIConstants.PREF_USE_JAVAHL_COMMIT_HACK));

		shareNestedProjects.setSelection(SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHARE_NESTED_PROJECTS));

		warnOnCommitToTagPath.setSelection(!SVNUIPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNUIConstants.PREF_COMMIT_TO_TAGS_PATH_WITHOUT_WARNING));
		
		ignoreHiddenChanges.setSelection(SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES));

		ignoreManagedDerivedResources.setSelection(SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES));
		
		removeOnReplace.setSelection(store.getBoolean(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE));
		
		logEntriesToFetchText.setText(Integer.toString(store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH)));
		
		quickDiffAnnotateYes.setSelection(MessageDialogWithToggle.ALWAYS.equals(store.getString(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE)));
		quickDiffAnnotateNo.setSelection(MessageDialogWithToggle.NEVER.equals(store.getString(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE)));
		quickDiffAnnotatePrompt.setSelection(MessageDialogWithToggle.PROMPT.equals(store.getString(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE)));
		
		resourcesWithErrorsYes.setSelection(MessageDialogWithToggle.ALWAYS.equals(store.getString(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS)));
		resourcesWithErrorsNo.setSelection(MessageDialogWithToggle.NEVER.equals(store.getString(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS)));
		resourcesWithErrorsPrompt.setSelection(MessageDialogWithToggle.PROMPT.equals(store.getString(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS)));
		
		resourcesWithWarningsYes.setSelection(MessageDialogWithToggle.ALWAYS.equals(store.getString(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS)));
		resourcesWithWarningsNo.setSelection(MessageDialogWithToggle.NEVER.equals(store.getString(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS)));
		resourcesWithWarningsPrompt.setSelection(MessageDialogWithToggle.PROMPT.equals(store.getString(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS)));
		
        String configLocation = store.getString(ISVNUIConstants.PREF_SVNCONFIGDIR); 
        directoryLocationText.setText(configLocation);
        if (configLocation.equals("")) { //$NON-NLS-1$
            defaultConfigLocationRadio.setSelection(true);
            useDirectoryLocationRadio.setSelection(false);
            browseConfigDirButton.setEnabled(false);
        } else {
            defaultConfigLocationRadio.setSelection(false);
            useDirectoryLocationRadio.setSelection(true);
            browseConfigDirButton.setEnabled(true);
        }
	}

    
   /**
	* @see IWorkbenchPreferencePage#init(IWorkbench)
	*/
	public void init(IWorkbench workbench) {
	}

	/**
	 * OK was clicked. Store the SVN preferences.  
	 *
	 * @return whether it is okay to close the preference page
	 */
	public boolean performOk() {
		final IPreferenceStore store = getPreferenceStore();
		for (Iterator iter = fFields.iterator(); iter.hasNext();) {
			((Field) iter.next()).performOk(store);
		}

        // save show compare revision in dialog pref
		store.setValue(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG, showCompareRevisionInDialog.getSelection());
		
		store.setValue(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND, fetchChangePathOnDemand.getSelection());
		
		store.setValue(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE, showTagsInRemoteHistory.getSelection());
		
		if (SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHOW_OUT_OF_DATE_FOLDERS) != showOutOfDateFolders.getSelection()) {
			SVNProviderPlugin.getPlugin().getPluginPreferences().setValue(ISVNCoreConstants.PREF_SHOW_OUT_OF_DATE_FOLDERS, showOutOfDateFolders.getSelection());
			SVNUIPlugin.getPlugin().getShowOutOfDateFoldersAction().setChecked(showOutOfDateFolders.getSelection());
		}

		SVNProviderPlugin.getPlugin().getPluginPreferences().setValue(ISVNCoreConstants.PREF_SHARE_NESTED_PROJECTS, shareNestedProjects.getSelection());
		
		SVNUIPlugin.getPlugin().getPluginPreferences().setValue(ISVNUIConstants.PREF_COMMIT_TO_TAGS_PATH_WITHOUT_WARNING, !warnOnCommitToTagPath.getSelection());
		
		SVNProviderPlugin.getPlugin().getPluginPreferences().setValue(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES, ignoreHiddenChanges.getSelection());
		
		SVNProviderPlugin.getPlugin().getPluginPreferences().setValue(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES, ignoreManagedDerivedResources.getSelection());
		
//		store.setValue(ISVNUIConstants.PREF_SHOW_UNADDED_RESOURCES_ON_COMMIT, showUnadded.getSelection());

		store.setValue(ISVNUIConstants.PREF_USE_JAVAHL_COMMIT_HACK, useJavaHLCommitHack.getSelection());
		
        // save select unadded resources on commit pref
//		store.setValue(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT, selectUnadded.getSelection());
		
		 // save remove unadded resources on replace
		store.setValue(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE, removeOnReplace.getSelection());

		if (quickDiffAnnotateYes.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE, MessageDialogWithToggle.ALWAYS);
		} else if (quickDiffAnnotateNo.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE, MessageDialogWithToggle.NEVER);
		} else if (quickDiffAnnotatePrompt.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE, MessageDialogWithToggle.PROMPT);
		}
		
		if (resourcesWithErrorsYes.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS, MessageDialogWithToggle.ALWAYS);
		} else if (resourcesWithErrorsNo.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS, MessageDialogWithToggle.NEVER);
		} else if (resourcesWithErrorsPrompt.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS, MessageDialogWithToggle.PROMPT);
		}
		
		if (resourcesWithWarningsYes.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS, MessageDialogWithToggle.ALWAYS);
		} else if (resourcesWithWarningsNo.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS, MessageDialogWithToggle.NEVER);
		} else if (resourcesWithWarningsPrompt.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS, MessageDialogWithToggle.PROMPT);
		}
		
		int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
		try {
			entriesToFetch = Integer.parseInt(logEntriesToFetchText.getText().trim());
		} catch (Exception e) {}
		
		store.setValue(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH, entriesToFetch);
		
//		HistoryView historyView = HistoryView.getView();
//		if (historyView != null) {
//			IAction getNextAction = historyView.getGetNextAction();
//			if (getNextAction != null) {
//				if (entriesToFetch <= 0) getNextAction.setEnabled(false);
//				else {
//					getNextAction.setEnabled(true);
//					getNextAction.setToolTipText(Policy.bind("HistoryView.getNext") + " " + entriesToFetch);
//				}
//			}
//		}
		
        
        // save config location pref
        if (defaultConfigLocationRadio.getSelection()) {
        	store.setValue(ISVNUIConstants.PREF_SVNCONFIGDIR, ""); //$NON-NLS-1$
        } else {
            store.setValue(ISVNUIConstants.PREF_SVNCONFIGDIR,directoryLocationText.getText());
        }
        
		SVNUIPlugin.getPlugin().savePluginPreferences();
		return true;
	}

	/**
	 * Defaults was clicked. Restore the SVN preferences to
	 * their default values
	 */
	protected void performDefaults() {
		super.performDefaults();
        initializeValues();
		
        verifyValidation();
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */ 
	protected IPreferenceStore doGetPreferenceStore() {
		return SVNUIPlugin.getPlugin().getPreferenceStore();
	}

	/**
	 * Verify the selection of the interface method
	 */
	private void verifyValidation() {
		setErrorMessage(null);
        
        if (useDirectoryLocationRadio.getSelection()) {
            File configDir = new File(directoryLocationText.getText());
            if (!configDir.exists()) {
            	setErrorMessage(Policy.bind("SVNPreferencePage.svnConfigDirDoesNotExist")); //$NON-NLS-1$
            } else {
                File serversFile = new File(configDir,"servers"); //$NON-NLS-1$
                File configFile = new File(configDir,"config"); //$NON-NLS-1$
                if (!serversFile.exists() && !configFile.exists()) {
                	setErrorMessage(Policy.bind("SVNPreferencePage.isNotSvnConfigDir")); //$NON-NLS-1$
                }
            }
        }
        
	
		setValid(getErrorMessage() == null);
	}
	
	private abstract class Field {
		protected final String fKey;
		public Field(String key) { fFields.add(this); fKey= key; }
		public abstract void initializeValue(IPreferenceStore store);
		public abstract void performOk(IPreferenceStore store);
		public void performDefaults(IPreferenceStore store) {
			store.setToDefault(fKey);
			initializeValue(store);
		}
	}
	
	private abstract class ComboBox extends Field {
		protected final Combo fCombo;
		private final String [] fLabels;
		private final List fValues;
		
		public ComboBox(Composite composite, String key, String text, String helpID, String [] labels, Object [] values) {
			super(key);
			fLabels= labels;
			fValues= Arrays.asList(values);
			
			final Label label= SWTUtils.createLabel(composite, text);
			fCombo= new Combo(composite, SWT.READ_ONLY);
			fCombo.setLayoutData(SWTUtils.createHFillGridData());
			fCombo.setItems(labels);
			
			if (((GridLayout)composite.getLayout()).numColumns > 1) {
				label.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
			}
			
			if (helpID != null)
				PlatformUI.getWorkbench().getHelpSystem().setHelp(fCombo, helpID);
		}
		
		public Combo getCombo() {
			return fCombo;
		}
		
		public void initializeValue(IPreferenceStore store) {
			final Object value= getValue(store, fKey);
			final int index= fValues.indexOf(value); 
			if (index >= 0 && index < fLabels.length)
				fCombo.select(index);
			else 
				fCombo.select(0);
		}
		
		public void performOk(IPreferenceStore store) {
			saveValue(store, fKey, fValues.get(fCombo.getSelectionIndex()));
		}
		
		protected abstract void saveValue(IPreferenceStore store, String key, Object object);
		protected abstract Object getValue(IPreferenceStore store, String key);
	}
	
	private class StringComboBox extends ComboBox {
		
		public StringComboBox(Composite composite, String key, String label, String helpID, String [] labels, String [] values) {
			super(composite, key, label, helpID, labels, values);
		}

		protected Object getValue(IPreferenceStore store, String key) {
			return store.getString(key);
		}
		
		protected void saveValue(IPreferenceStore store, String key, Object object) {
			store.setValue(key, (String)object);
		}
	}
    
}
