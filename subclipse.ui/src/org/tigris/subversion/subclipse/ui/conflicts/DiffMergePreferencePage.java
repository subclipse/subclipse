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
package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.WorkspaceAction;

/**
 * Preference page to set the program for merge
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public class DiffMergePreferencePage extends PreferencePage implements
        IWorkbenchPreferencePage {

    private Text mergeProgramLocationText;

    private Text mergeProgramParametersText;

    private Button mergeProgramParametersButton;

    private Button builtInMergeRadioButton;

    private Button externalMergeRadioButton;

    private Button browseMergeProgramButton;
    
    private Combo mergeImplementationCombo;
    
    private Button suggestMergeSourcesButton;
    
    private WorkspaceAction[] mergeProviders;

    class StringPair {
        String s1;

        String s2;
    }

    /**
     * creates a label
     */
    private Label createLabel(Composite parent, String text, int span,
            int horizontalIndent) {
        Label label = new Label(parent, SWT.LEFT);
        label.setText(text);
        GridData data = new GridData();
        data.horizontalSpan = span;
        data.horizontalAlignment = GridData.FILL;
        data.horizontalIndent = horizontalIndent;
        label.setLayoutData(data);
        return label;
    }

    /**
     * creates a label which the user can copy to clipboard (in fact it is a
     * Text)
     */
    private Text createCopiableLabel(Composite parent, String text, int span,
            int horizontalIndent) {
        Text textControl = new Text(parent, SWT.READ_ONLY);
        textControl.setText(text);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.horizontalSpan = span;
        data.horizontalAlignment = GridData.FILL;
        data.horizontalIndent = horizontalIndent;
        textControl.setLayoutData(data);
        return textControl;
    }

    /**
     * creates a radio button
     */
    private Button createRadio(Composite parent, String label, int span) {
        Button button = new Button(parent, SWT.RADIO);
        button.setText(label);
        GridData data = new GridData();
        data.horizontalSpan = span;
        button.setLayoutData(data);
        return button;
    }

    /**
     * creates a Text control
     */
    private Text createText(Composite parent, int widthHint) {
        Text textControl = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.widthHint = widthHint;
        gridData.grabExcessHorizontalSpace = true;
        textControl.setLayoutData(gridData);
        return textControl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(gridData);
        
        Label mergeImplementationLabel = new Label(composite, SWT.NONE);
        mergeImplementationLabel.setText(Policy.bind("DiffMergePreferencePage.mergeImplementation")); //$NON-NLS-1$
        
        mergeImplementationCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        mergeImplementationCombo.setLayoutData(gridData);
        
        mergeImplementationCombo.addSelectionListener(new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				suggestMergeSourcesButton.setVisible(mergeImplementationCombo.getText().equals("CollabNet Desktop")); //$NON-NLS-1$
			}
		});
        
        try {
			mergeProviders = SVNUIPlugin.getMergeProviders();
			for (int i = 0; i < mergeProviders.length; i++) 
				mergeImplementationCombo.add(mergeProviders[i].getName());
		} catch (Exception e) {}
        
        suggestMergeSourcesButton = new Button(composite, SWT.CHECK);
        suggestMergeSourcesButton.setText(Policy.bind("DiffMergePreferencePage.1")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        suggestMergeSourcesButton.setLayoutData(gridData);

        // Group "Merge program"
        Group group = new Group(composite, SWT.NULL);
        group.setText(Policy.bind("DiffMergePreferencePage.mergeProgramGroup")); //$NON-NLS-1$
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        layout = new GridLayout();
        layout.numColumns = 3;
        group.setLayout(layout);

        // program used to resolve conflicted files
        Listener updateEnablementsListener = new Listener() {
            public void handleEvent(Event event) {
                updateEnablements();
            }
        };
        builtInMergeRadioButton = createRadio(group, Policy
                .bind("DiffMergePreferencePage.builtInMerge"), 3); //$NON-NLS-1$
        builtInMergeRadioButton.addListener(SWT.Selection,
                updateEnablementsListener);

        externalMergeRadioButton = createRadio(group, Policy
                .bind("DiffMergePreferencePage.externalMerge"), 1); //$NON-NLS-1$
        externalMergeRadioButton.addListener(SWT.Selection,
                updateEnablementsListener);

        mergeProgramLocationText = createText(group, 200);
        mergeProgramLocationText.setEditable(false);
        browseMergeProgramButton = new Button(group, SWT.NONE);
        browseMergeProgramButton.setText(Policy
                .bind("DiffMergePreferencePage.browse")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        browseMergeProgramButton.setLayoutData(gridData);
        browseMergeProgramButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
                String res = fileDialog.open();
                if (res != null) {
                    mergeProgramLocationText.setText(res);
                }
            }
        });

        // parameters
        createLabel(group, Policy
                .bind("DiffMergePreferencePage.mergeProgramParameters"), 1, 20); //$NON-NLS-1$
        Control[] formatEditorControl = createFormatEditorControl(
                group,
                Policy.bind("DiffMergePreferencePage.mergeProgramVariables"), getMergeBindingDescriptions()); //$NON-NLS-1$
        mergeProgramParametersText = (Text) formatEditorControl[0];
        mergeProgramParametersButton = (Button) formatEditorControl[1];

        createLabel(group, Policy
                .bind("DiffMergePreferencePage.tortoiseMergeComment1"), 3, 20); //$NON-NLS-1$

        createCopiableLabel(group, Policy
                .bind("DiffMergePreferencePage.tortoiseMergeComment2"), //$NON-NLS-1$
                3, 20);

        initializeValues();
        
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		mergeProgramParametersText.addFocusListener(focusListener);
        
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.DIFF_MERGE_PREFERENCE_PAGE);

        return composite;
    }

    protected Control[] createFormatEditorControl(Composite composite,
            String buttonText, final Map supportedBindings) {

        Text format = new Text(composite, SWT.BORDER);
        format.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Button b = new Button(composite, SWT.NONE);
        b.setText(buttonText);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        b.setLayoutData(data);
        final Text formatToInsert = format;
        b.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                addVariables(formatToInsert, supportedBindings);
            }
        });

        return new Control[] { format, b };
    }

    /**
     * Add another variable to the given target. The variable is inserted at
     * current position A ListSelectionDialog is shown and the choose the
     * variables to add
     */
    private void addVariables(Text target, Map bindings) {

        final List variables = new ArrayList(bindings.size());

        ILabelProvider labelProvider = new LabelProvider() {
            public String getText(Object element) {
                return ((StringPair) element).s1
                        + " - " + ((StringPair) element).s2; //$NON-NLS-1$
            }
        };

        IStructuredContentProvider contentsProvider = new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return variables.toArray(new StringPair[variables.size()]);
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        };

        for (Iterator it = bindings.keySet().iterator(); it.hasNext();) {
            StringPair variable = new StringPair();
            variable.s1 = (String) it.next(); // variable
            variable.s2 = (String) bindings.get(variable.s1); // description
            variables.add(variable);
        }

        ListDialog dialog = new ListDialog(this.getShell());
        dialog.setContentProvider(contentsProvider);
        dialog.setAddCancelButton(true);
        dialog.setLabelProvider(labelProvider);
        dialog.setInput(variables);
        dialog.setTitle(Policy
                .bind("DiffMergePreferencePage.addVariableDialogTitle")); //$NON-NLS-1$
        if (dialog.open() != ListDialog.OK)
            return;

        Object[] result = dialog.getResult();

        for (int i = 0; i < result.length; i++) {
            target.insert("${" + ((StringPair) result[i]).s1 + "}"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * get the map of {variable,description} to use for merge program
     */
    private Map getMergeBindingDescriptions() {
        Map bindings = new HashMap();
        bindings
                .put(
                        "merged", Policy.bind("DiffMergePreferencePage.mergedVariableComment")); //$NON-NLS-1$ //$NON-NLS-2$
        bindings
                .put(
                        "theirs", Policy.bind("DiffMergePreferencePage.theirsVariableComment")); //$NON-NLS-1$ //$NON-NLS-2$
        bindings
                .put(
                        "yours", Policy.bind("DiffMergePreferencePage.yoursVariableComment")); //$NON-NLS-1$ //$NON-NLS-2$
        bindings
                .put(
                        "base", Policy.bind("DiffMergePreferencePage.baseVariableComment"));//$NON-NLS-1$ //$NON-NLS-2$
        return bindings;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    /**
     * Initializes states of the controls from the preference store.
     */
    private void initializeValues() {
        IPreferenceStore store = getPreferenceStore();

        String mergeProviderPreference = store.getString(ISVNUIConstants.PREF_MERGE_PROVIDER);
        if (mergeProviderPreference == null) mergeProviders[0].getName();
        for (int i = 0; i < mergeProviders.length; i++) {
        	if (mergeProviders[i].getName().equals(mergeProviderPreference)) {
        		mergeImplementationCombo.setText(mergeProviders[i].getName());
        		break;
        	}
        }
        if (mergeImplementationCombo.getText().length() == 0) mergeImplementationCombo.setText(mergeProviders[0].getName());
        
        suggestMergeSourcesButton.setSelection(store.getBoolean(ISVNUIConstants.PREF_SUGGEST_MERGE_SOURCES));
        suggestMergeSourcesButton.setVisible(mergeImplementationCombo.getText().equals("CollabNet Desktop")); //$NON-NLS-1$
        
        mergeProgramLocationText.setText(store
                .getString(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION));
        mergeProgramParametersText.setText(store
                .getString(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS));

        if (store.getBoolean(ISVNUIConstants.PREF_MERGE_USE_EXTERNAL)) {
            builtInMergeRadioButton.setSelection(false);
            externalMergeRadioButton.setSelection(true);
        } else {
            builtInMergeRadioButton.setSelection(true);
            externalMergeRadioButton.setSelection(false);
        }
        updateEnablements();
    }

    private void updateEnablements() {
        if (builtInMergeRadioButton.getSelection()) {
            browseMergeProgramButton.setEnabled(false);
            mergeProgramParametersText.setEnabled(false);
            mergeProgramParametersButton.setEnabled(false);
            mergeProgramLocationText.setEnabled(false);
        } else {
            browseMergeProgramButton.setEnabled(true);
            mergeProgramParametersText.setEnabled(true);
            mergeProgramParametersButton.setEnabled(true);
            mergeProgramLocationText.setEnabled(true);
        }
    }

    /**
     * Defaults was clicked. Restore the SVN preferences to their default values
     */
    protected void performDefaults() {
        super.performDefaults();
        initializeValues();
    }

    /**
     * OK was clicked. Store the SVN preferences.
     * 
     * @return whether it is okay to close the preference page
     */
    public boolean performOk() {
        IPreferenceStore store = getPreferenceStore();
        
        store.setValue(ISVNUIConstants.PREF_MERGE_PROVIDER, 
        		mergeImplementationCombo.getText());
        
        store.setValue(ISVNUIConstants.PREF_SUGGEST_MERGE_SOURCES,
        		suggestMergeSourcesButton.getSelection());
        

        store.setValue(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION,
                mergeProgramLocationText.getText());

        store.setValue(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS,
                mergeProgramParametersText.getText());

        store.setValue(ISVNUIConstants.PREF_MERGE_USE_EXTERNAL,
                externalMergeRadioButton.getSelection());
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
     */
    protected IPreferenceStore doGetPreferenceStore() {
        return SVNUIPlugin.getPlugin().getPreferenceStore();
    }

}