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
package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.util.FileNameMatcher;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * the dialog used to add patterns to svn:ignore 
 */
public class IgnoreResourcesDialog extends SubclipseTrayDialog {
	// resources that should be ignored
	private IResource[] resources;

	// preference keys
	private final String ACTION_KEY = "Action"; //$NON-NLS-1$
	private static final int ADD_NAME_ENTRY = 0;
	private static final int ADD_EXTENSION_ENTRY = 1;
	private static final int ADD_CUSTOM_ENTRY = 2;

	// dialogs settings that are persistent between workbench sessions
	private IDialogSettings settings;

	// buttons
	private Button addNameEntryButton;
	private Button addExtensionEntryButton;
	private Button addCustomEntryButton;
	private Text customEntryText;
	private Label statusMessageLabel;
	
	private int selectedAction;
	private String customPattern;
	
	private static final int LABEL_INDENT_WIDTH = 32;

	/**
	 * Creates a new dialog for ignoring resources.
	 * @param shell the parent shell
	 * @param resources the array of resources to be ignored
	 */
	public IgnoreResourcesDialog(Shell shell, IResource[] resources) {
		super(shell);
		this.resources = resources;

		IDialogSettings workbenchSettings = SVNUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("IgnoreResourcesDialog");//$NON-NLS-1$
		if (settings == null) {
			this.settings = workbenchSettings.addNewSection("IgnoreResourcesDialog");//$NON-NLS-1$
		}
		
		try {
			selectedAction = settings.getInt(ACTION_KEY);
		} catch (NumberFormatException e) {
			selectedAction = ADD_NAME_ENTRY;
		}
	}
	
	/**
	 * Determines the ignore pattern to use for a resource given the selected action.
	 * 
	 * @param resource the resource
	 * @return the ignore pattern for the specified resource
	 */
	public String getIgnorePatternFor(IResource resource) {
		switch (selectedAction) {
			case ADD_NAME_ENTRY:
				return resource.getName();
			case ADD_EXTENSION_ENTRY: {
				String extension = resource.getFileExtension();
				return (extension == null) ? resource.getName() : "*." + extension; //$NON-NLS-1$
			}
			case ADD_CUSTOM_ENTRY:
				return customPattern;
		}
		throw new IllegalStateException();
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (resources.length == 1) {
			newShell.setText(Policy.bind("IgnoreResourcesDialog.titleSingle", resources[0].getName())); //$NON-NLS-1$
		} else {
			newShell.setText(Policy.bind("IgnoreResourcesDialog.titleMany", Integer.toString(resources.length))); //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		updateEnablements();
		return control;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout());
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(top, IHelpContextIds.ADD_TO_SVNIGNORE);
		
		createIndentedLabel(top, Policy.bind("IgnoreResourcesDialog.prompt"), 0); //$NON-NLS-1$
		
		Listener selectionListener = new Listener() {
			public void handleEvent(Event event) {
				updateEnablements();
			}
		};
		Listener modifyListener = new Listener() {
			public void handleEvent(Event event) {
				validate();
			}
		};
		
		addNameEntryButton = createRadioButton(top, Policy.bind("IgnoreResourcesDialog.addNameEntryButton")); //$NON-NLS-1$
		addNameEntryButton.addListener(SWT.Selection, selectionListener);
		addNameEntryButton.setSelection(selectedAction == ADD_NAME_ENTRY);
		createIndentedLabel(top, Policy.bind("IgnoreResourcesDialog.addNameEntryExample"), LABEL_INDENT_WIDTH); //$NON-NLS-1$
		
		addExtensionEntryButton = createRadioButton(top, Policy.bind("IgnoreResourcesDialog.addExtensionEntryButton")); //$NON-NLS-1$
		addExtensionEntryButton.addListener(SWT.Selection, selectionListener);
		addExtensionEntryButton.setSelection(selectedAction == ADD_EXTENSION_ENTRY);
		createIndentedLabel(top, Policy.bind("IgnoreResourcesDialog.addExtensionEntryExample"), LABEL_INDENT_WIDTH); //$NON-NLS-1$

		addCustomEntryButton = createRadioButton(top, Policy.bind("IgnoreResourcesDialog.addCustomEntryButton")); //$NON-NLS-1$
		addCustomEntryButton.addListener(SWT.Selection, selectionListener);
		addCustomEntryButton.setSelection(selectedAction == ADD_CUSTOM_ENTRY);
		createIndentedLabel(top, Policy.bind("IgnoreResourcesDialog.addCustomEntryExample"), LABEL_INDENT_WIDTH); //$NON-NLS-1$
		
		customEntryText = createIndentedText(top, resources[0].getName(), LABEL_INDENT_WIDTH);
		customEntryText.addListener(SWT.Modify, modifyListener);
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		customEntryText.addFocusListener(focusListener);

		statusMessageLabel = createIndentedLabel(top, "", 0); //$NON-NLS-1$
		statusMessageLabel.setFont(parent.getFont());
		return top;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void okPressed() {
		settings.put(ACTION_KEY, selectedAction);
		super.okPressed();
	}

	private Label createIndentedLabel(Composite parent, String text, int indent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}

	private Text createIndentedText(Composite parent, String text, int indent) {
		Text textbox = new Text(parent, SWT.BORDER);
		textbox.setText(text);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalIndent = indent;
		textbox.setLayoutData(data);
		return textbox;
	}

	private Button createRadioButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		button.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL));
		return button;
	}

	private void updateEnablements() {
		if (addNameEntryButton.getSelection()) {
			selectedAction = ADD_NAME_ENTRY;
		} else if (addExtensionEntryButton.getSelection()) {
			selectedAction = ADD_EXTENSION_ENTRY;
		} else if (addCustomEntryButton.getSelection()) {
			selectedAction = ADD_CUSTOM_ENTRY;
			customEntryText.setFocus();
		}
		customEntryText.setEnabled(selectedAction == ADD_CUSTOM_ENTRY);
		validate();
	}
	
	private void validate() {
		if (selectedAction == ADD_CUSTOM_ENTRY) {
			customPattern = customEntryText.getText();
			if (customPattern.length() == 0) {
				setError(Policy.bind("IgnoreResourcesDialog.patternMustNotBeEmpty")); //$NON-NLS-1$
				return;
			}
            
            // we check that the pattern match all the files
			FileNameMatcher matcher = new FileNameMatcher(new String[] { customPattern });
			for (int i = 0; i < resources.length; i++) {
				String name = resources[i].getName();
				if (! matcher.match(name)) {
					setError(Policy.bind("IgnoreResourcesDialog.patternDoesNotMatchFile", name)); //$NON-NLS-1$
					return;
				}
			}
		}
		setError(null);
	}
	
	private void setError(String text) {
		if (text == null) {
			statusMessageLabel.setText(""); //$NON-NLS-1$
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		} else {
			statusMessageLabel.setText(text);
			statusMessageLabel.setForeground(JFaceColors.getErrorText(getShell().getDisplay()));
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
}
