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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * SVN Preference Page
 * 
 * Allows the configuration of SVN specific options.
 * 
 */
public class SVNMenuIconsPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button useDefault;
	private Button useTortoiseSVN;
	private Button useSubversive;
	
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
		
		Group group = new Group(composite, SWT.NULL);
		group.setText(Policy.bind("MenuIconsPreferencePage.iconSet")); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		group.setLayoutData(gridData);
		layout = new GridLayout();
		group.setLayout(layout); 	

		
		useDefault = createRadio(group, Policy.bind("MenuIconsPreferencePage.default"), 1); //$NON-NLS-1$
		useTortoiseSVN = createRadio(group, "&TortoiseSVN", 1); //$NON-NLS-1$
		useSubversive  = createRadio(group, "&Subversive", 1); //$NON-NLS-1$
		
		createLabel(composite, "", 2); //$NON-NLS-1$
		createLabel(composite, Policy.bind("MenuIconsPreferencePage.restart"), 2); //$NON-NLS-1$

		initializeValues();
		verifyValidation();

//		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.SVN_PREFERENCE_DIALOG);

		return composite;
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		IPreferenceStore store = getPreferenceStore();

		int iconSet = store.getInt(ISVNUIConstants.PREF_MENU_ICON_SET);
		useTortoiseSVN.setSelection(ISVNUIConstants.MENU_ICON_SET_TORTOISESVN == iconSet);
		useDefault.setSelection(ISVNUIConstants.MENU_ICON_SET_DEFAULT == iconSet);
		useSubversive.setSelection(ISVNUIConstants.MENU_ICON_SET_SUBVERSIVE == iconSet);
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

		if (useDefault.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_MENU_ICON_SET, ISVNUIConstants.MENU_ICON_SET_DEFAULT);
		} else if (useTortoiseSVN.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_MENU_ICON_SET, ISVNUIConstants.MENU_ICON_SET_TORTOISESVN);
		} else if (useSubversive.getSelection()) {
			store.setValue(ISVNUIConstants.PREF_MENU_ICON_SET, ISVNUIConstants.MENU_ICON_SET_SUBVERSIVE);
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

		IPreferenceStore store = getPreferenceStore();

		int iconSet = store.getDefaultInt(ISVNUIConstants.PREF_MENU_ICON_SET);
		useTortoiseSVN.setSelection(ISVNUIConstants.MENU_ICON_SET_TORTOISESVN == iconSet);
		useDefault.setSelection(ISVNUIConstants.MENU_ICON_SET_DEFAULT == iconSet);
		useDefault.setSelection(ISVNUIConstants.MENU_ICON_SET_SUBVERSIVE == iconSet);
		
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
		setValid(getErrorMessage() == null);
	}
    
}
