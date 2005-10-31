/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion  
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui.preferences;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
import org.eclipse.ui.help.WorkbenchHelp;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.commandline.CmdLineClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.javasvn.JavaSvnClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapter;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;

/**
 * SVN Preference Page
 * 
 * Allows the configuration of SVN specific options.
 * 
 */
public class SVNPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button javahlRadio;
    private Button javaSvnRadio;
    private Button commandLineRadio;
    private Button showCompareRevisionInDialog;
    private Button fetchChangePathOnDemand;
    private Button selectUnadded;
    private Button defaultConfigLocationRadio;
    private Button useDirectoryLocationRadio;
    private Text directoryLocationText;
    private Button browseConfigDirButton;
    
    private boolean javahlErrorShown = false;

	public SVNPreferencesPage() {
		// sort the options by display text
		setDescription(Policy.bind("SVNPreferencePage.description")); //$NON-NLS-1$
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
		
		selectUnadded = createCheckBox(composite, Policy.bind("SVNPreferencePage.selectUnadded")); //$NON-NLS-1$
		createLabel(composite, "", 2); //$NON-NLS-1$
		
		fetchChangePathOnDemand = createCheckBox(composite, Policy.bind("SVNPreferencePage.fetchChangePathOnDemand")); //$NON-NLS-1$
		createLabel(composite, "", 2); //$NON-NLS-1$
		
		// group javahl/command line
		Group group = new Group(composite, SWT.NULL);
		group.setText(Policy.bind("SVNPreferencePage.svnClientInterface")); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		group.setLayoutData(gridData);
		layout = new GridLayout();
		group.setLayout(layout); 	
		javahlRadio = createRadio(group, Policy.bind("SVNPreferencePage.svnjavahl"),1); //$NON-NLS-1$
		javaSvnRadio = createRadio(group, Policy.bind("SVNPreferencePage.javasvn"),1); //$NON-NLS-1$
		commandLineRadio = createRadio(group, Policy.bind("SVNPreferencePage.svncommandline"),1); //$NON-NLS-1$
        Listener checkInterfaceListener = new Listener() {
            public void handleEvent(Event event) {
                verifyValidation();
            }
        };
        javahlRadio.addListener(SWT.Selection,checkInterfaceListener);
        javaSvnRadio.addListener(SWT.Selection,checkInterfaceListener);
		commandLineRadio.addListener(SWT.Selection,checkInterfaceListener);
		
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

		WorkbenchHelp.setHelp(composite, IHelpContextIds.SVN_PREFERENCE_DIALOG);

		return composite;
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		IPreferenceStore store = getPreferenceStore();
		
		showCompareRevisionInDialog.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG));
		
		fetchChangePathOnDemand.setSelection(store.getBoolean(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND));
		
		selectUnadded.setSelection(store.getBoolean(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT));
		
        javahlRadio.setSelection(store.getString(
                ISVNUIConstants.PREF_SVNINTERFACE).equals(JhlClientAdapterFactory.JAVAHL_CLIENT));
        javaSvnRadio.setSelection(store.getString(
                ISVNUIConstants.PREF_SVNINTERFACE).equals(JavaSvnClientAdapterFactory.JAVASVN_CLIENT));
        commandLineRadio.setSelection(store.getString(
                ISVNUIConstants.PREF_SVNINTERFACE).equals(CmdLineClientAdapterFactory.COMMANDLINE_CLIENT));
        
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
		IPreferenceStore store = getPreferenceStore();

        // save show compare revision in dialog pref
		store.setValue(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG, showCompareRevisionInDialog.getSelection());
		
		store.setValue(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND, fetchChangePathOnDemand.getSelection());
		
        // save select unadded resources on commit pref
		store.setValue(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT, selectUnadded.getSelection());

        // save svn interface pref
        if (javahlRadio.getSelection() ){
            store.setValue(ISVNUIConstants.PREF_SVNINTERFACE, JhlClientAdapterFactory.JAVAHL_CLIENT);
        }else{
            if (javaSvnRadio.getSelection() ){
                store.setValue(ISVNUIConstants.PREF_SVNINTERFACE, JavaSvnClientAdapterFactory.JAVASVN_CLIENT);
            }else{
                store.setValue(ISVNUIConstants.PREF_SVNINTERFACE, CmdLineClientAdapterFactory.COMMANDLINE_CLIENT);
            }
        }
        
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
        
        if (javahlRadio.getSelection()) {
			if (!SVNClientAdapterFactory.isSVNClientAvailable(JhlClientAdapterFactory.JAVAHL_CLIENT)) {
				setErrorMessage(Policy.bind("SVNPreferencePage.javahlNotAvailable")); //$NON-NLS-1$
				if (!javahlErrorShown) {
				    javahlErrorShown = true;
					MessageDialog.openError(
							getShell(),
							"Error Loading JavaHL Library",
							JhlClientAdapter.getLibraryLoadErrors());
				}
			}
		}
        if (javaSvnRadio.getSelection()) {
			if (!SVNClientAdapterFactory.isSVNClientAvailable(JavaSvnClientAdapterFactory.JAVASVN_CLIENT)) {
				setErrorMessage(Policy.bind("SVNPreferencePage.javaSvnNotAvailable")); //$NON-NLS-1$
			}
		}
		if (commandLineRadio.getSelection()) {
			if (!SVNClientAdapterFactory.isSVNClientAvailable(CmdLineClientAdapterFactory.COMMANDLINE_CLIENT)) {
				setErrorMessage(Policy.bind("SVNPreferencePage.commandLineNotAvailable")); //$NON-NLS-1$
			}
		}
		
		setValid(getErrorMessage() == null);
	}
    
}
