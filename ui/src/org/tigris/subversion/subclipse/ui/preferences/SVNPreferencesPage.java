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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;

/**
 * SVN Preference Page
 * 
 * Allows the configuration of SVN specific options.
 * 
 */
public class SVNPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button javahlRadio;
    private Button commandLineRadio;
	
	public SVNPreferencesPage() {
		// sort the options by display text
		setDescription(Policy.bind("SVNPreferencePage.description")); //$NON-NLS-1$
	}


	/**
	 * listener used when selection changes
	 */
	Listener checkInterfaceListener = new Listener() {
		public void handleEvent(Event event) {
			if (javahlRadio.getSelection()) {
				if (!SVNClientAdapterFactory.isSVNClientAvailable(SVNClientAdapterFactory.JAVAHL_CLIENT)) {
					setErrorMessage(Policy.bind("SVNPreferencePage.javahlNotAvailable")); //$NON-NLS-1$
				} else {
					setErrorMessage(null);															
				}
			}
			if (commandLineRadio.getSelection()) {
				if (!SVNClientAdapterFactory.isSVNClientAvailable(SVNClientAdapterFactory.COMMANDLINE_CLIENT)) {
					setErrorMessage(Policy.bind("SVNPreferencePage.commandLineNotAvailable")); //$NON-NLS-1$
				} else {
					setErrorMessage(null);															
				}
			}
		}
	};		



	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		// create the composite
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData());
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		// create the group
		Group group = new Group(composite, SWT.NULL);
		group.setText(Policy.bind("SVNPreferencePage.svnClientInterface")); //$NON-NLS-1$
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		group.setLayoutData(gridData);
		layout = new GridLayout();
		group.setLayout(layout); 	
				
		javahlRadio = new Button(group, SWT.RADIO);
		javahlRadio.setText(Policy.bind("SVNPreferencePage.svnjavahl")); //$NON-NLS-1$
		commandLineRadio = new Button(group, SWT.RADIO);
		commandLineRadio.setText(Policy.bind("SVNPreferencePage.svncommandline")); //$NON-NLS-1$
		
		javahlRadio.addListener(SWT.Selection,checkInterfaceListener);
		commandLineRadio.addListener(SWT.Selection,checkInterfaceListener);
		
		initializeValues();
		
		return composite;
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		IPreferenceStore store = getPreferenceStore();

        if (store.getInt(ISVNUIConstants.PREF_SVNINTERFACE) == SVNClientAdapterFactory.JAVAHL_CLIENT)
            javahlRadio.setSelection(true);
        else
            commandLineRadio.setSelection(true);
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
		
        if (javahlRadio.getSelection() )
            store.setValue(ISVNUIConstants.PREF_SVNINTERFACE, SVNClientAdapterFactory.JAVAHL_CLIENT);
        else
            store.setValue(ISVNUIConstants.PREF_SVNINTERFACE, SVNClientAdapterFactory.COMMANDLINE_CLIENT);
		
		SVNUIPlugin.getPlugin().savePluginPreferences();
		return true;
	}

	/**
	 * Defaults was clicked. Restore the SVN preferences to
	 * their default values
	 */
	protected void performDefaults() {
		super.performDefaults();
		IPreferenceStore store = getPreferenceStore();
        
		if (store.getInt(ISVNUIConstants.PREF_SVNINTERFACE) == SVNClientAdapterFactory.JAVAHL_CLIENT)
			javahlRadio.setSelection(true);
		else
			commandLineRadio.setSelection(true);
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */ 
	protected IPreferenceStore doGetPreferenceStore() {
		return SVNUIPlugin.getPlugin().getPreferenceStore();
	}

}
