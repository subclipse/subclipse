/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.console;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class ConsolePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ConsolePreferencesPage() {
		super(GRID);
		setPreferenceStore(SVNUIPlugin.getPlugin().getPreferenceStore());
	}
	private ColorFieldEditor commandColorEditor;
	private ColorFieldEditor messageColorEditor;
	private ColorFieldEditor errorColorEditor;
	private BooleanFieldEditor showOnMessage;
    private BooleanFieldEditor showOnError;
	private BooleanFieldEditor restrictOutput;
	private IntegerFieldEditor highWaterMark;
	
	protected void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		IPreferenceStore store = getPreferenceStore();
		
		restrictOutput = new BooleanFieldEditor(ISVNUIConstants.PREF_CONSOLE_LIMIT_OUTPUT, Policy.bind("ConsolePreferencePage.limitOutput"), composite); 
		addField(restrictOutput);
		
		highWaterMark = new IntegerFieldEditor(ISVNUIConstants.PREF_CONSOLE_HIGH_WATER_MARK, Policy.bind("ConsolePreferencePage.highWaterMark"), composite); 
		highWaterMark.setValidRange(1000, Integer.MAX_VALUE - 1);
		addField(highWaterMark);
		highWaterMark.setEnabled(store.getBoolean(ISVNUIConstants.PREF_CONSOLE_LIMIT_OUTPUT), composite);
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		highWaterMark.getTextControl(composite).addFocusListener(focusListener);
		
		showOnMessage = new BooleanFieldEditor(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE,
	            Policy.bind("ConsolePreferencePage.showOnMessage"), composite); //$NON-NLS-1$
	        addField(showOnMessage);

	        showOnError = new BooleanFieldEditor(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_ERROR,
	                Policy.bind("ConsolePreferencePage.showOnError"), composite); //$NON-NLS-1$
	        addField(showOnError);
	        
		createLabel(composite, Policy.bind("ConsolePreferencePage.consoleColorSettings")); //$NON-NLS-1$
				
		commandColorEditor = createColorFieldEditor(ISVNUIConstants.PREF_CONSOLE_COMMAND_COLOR,
			Policy.bind("ConsolePreferencePage.commandColor"), composite); //$NON-NLS-1$
		addField(commandColorEditor);
		
		messageColorEditor = createColorFieldEditor(ISVNUIConstants.PREF_CONSOLE_MESSAGE_COLOR,
			Policy.bind("ConsolePreferencePage.messageColor"), composite); //$NON-NLS-1$
		addField(messageColorEditor);
		
		errorColorEditor = createColorFieldEditor(ISVNUIConstants.PREF_CONSOLE_ERROR_COLOR,
			Policy.bind("ConsolePreferencePage.errorColor"), composite); //$NON-NLS-1$
		addField(errorColorEditor);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.CONSOLE_PREFERENCE_PAGE);
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		highWaterMark.setEnabled(restrictOutput.getBooleanValue(), getFieldEditorParent());
	}

	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	private Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Creates a new color field editor.
	 */
	private ColorFieldEditor createColorFieldEditor(String preferenceName, String label, Composite parent) {
		ColorFieldEditor editor = new ColorFieldEditor(preferenceName, label, parent);
		editor.setPage(this);
		editor.setPreferenceStore(getPreferenceStore());
		return editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		SVNUIPlugin.getPlugin().savePluginPreferences();
		return super.performOk();
	}
}
