/*******************************************************************************
 * Copyright (c) 2005 Maik Schreiber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Maik Schreiber - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui.comments;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.*;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class CommentTemplatesPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage, ISelectionChangedListener {

	private ListViewer viewer;
	private Button editButton;
	private Button removeButton;
	private Text preview;

	protected Control createContents(Composite ancestor) {
		Composite parent = new Composite(ancestor, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));

		createListAndButtons(parent);

		Label previewLabel = new Label(parent, SWT.NONE);
		previewLabel.setText(Policy.bind("CommentTemplatesPreferencePage.Preview")); //$NON-NLS-1$
		
		preview = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = convertHeightInCharsToPixels(5);
		preview.setLayoutData(gd);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.COMMENT_TEMPLATE_PREFERENCE_PAGE);
		Dialog.applyDialogFont(ancestor);
		
		return parent;
	}

	private Composite createListAndButtons(Composite parent) {
		Composite listAndButtons = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;
		listAndButtons.setLayout(layout);
		listAndButtons.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		viewer = new ListViewer(listAndButtons);
		viewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				String template = (String) element;
				return Util.flattenText(template);
			}
		});
		viewer.addSelectionChangedListener(this);
		viewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				String template1 = Util.flattenText((String) e1);
				String template2 = Util.flattenText((String) e2);
				return template1.compareToIgnoreCase(template2);
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				editTemplate();
			}
		});
		List list = viewer.getList();
		list.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// populate list
		String[] templates =
			SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().getCommentTemplates();
		for (int i = 0; i < templates.length; i++) {
			viewer.add(templates[i]);
		}

		createButtons(listAndButtons);
		return listAndButtons;
	}

	private void createButtons(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NONE);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);

		Button newButton = new Button(buttons, SWT.PUSH);
		newButton.setText(Policy.bind("CommentTemplatesPreferencePage.New")); //$NON-NLS-1$
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint,
				newButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		newButton.setLayoutData(data);
		newButton.setEnabled(true);
		newButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				newTemplate();
			}
		});

		editButton = new Button(buttons, SWT.PUSH);
		editButton.setText(Policy.bind("CommentTemplatesPreferencePage.Edit")); //$NON-NLS-1$
		
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint,
				editButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		editButton.setLayoutData(data);
		editButton.setEnabled(false);
		editButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				editTemplate();
			}
		});

		removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText(Policy.bind("CommentTemplatesPreferencePage.Remove"));
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint,
				removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		removeButton.setLayoutData(data);
		removeButton.setEnabled(false);
		removeButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				remove();
			}
		});
	}
	
	public void init(IWorkbench workbench) {
		setDescription(Policy.bind("CommentTemplatesPreferencePage.Description")); //$NON-NLS-1$
	}

	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		switch (selection.size()) {
			case 0:
				editButton.setEnabled(false);
				removeButton.setEnabled(false);
				preview.setText(""); //$NON-NLS-1$
				break;
			
			case 1:
				editButton.setEnabled(true);
				removeButton.setEnabled(true);
				preview.setText((String) selection.getFirstElement());
				break;
			
			default:
				editButton.setEnabled(false);
				removeButton.setEnabled(true);
				preview.setText(""); //$NON-NLS-1$
				break;
		}
	}
	
	void newTemplate() {
		CommentTemplateEditDialog dialog = new CommentTemplateEditDialog(
				getShell(),
				Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateTitle"),
				Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateMessage"),
				"", null); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			viewer.add(dialog.getValue());
		}
	}

	void editTemplate() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (selection.size() == 1) {
			String oldTemplate = (String) selection.getFirstElement();
			CommentTemplateEditDialog dialog = new CommentTemplateEditDialog(
					getShell(),
					Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateTitle"),
					Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateMessage"),
					oldTemplate, null);
			if (dialog.open() == Window.OK) {
				viewer.remove(oldTemplate);
				viewer.add(dialog.getValue());
			}
		}
	}
	
	void remove() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		viewer.remove(selection.toArray());
	}
	
	public boolean performOk() {
		int numTemplates = viewer.getList().getItemCount();
		String[] templates = new String[numTemplates];
		for (int i = 0; i < numTemplates; i++) {
			templates[i] = (String) viewer.getElementAt(i);
		}
		try {
			SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().replaceAndSaveCommentTemplates(templates);
		} catch (TeamException e) {
			SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.LOG_OTHER_EXCEPTIONS);
		}
		
		return super.performOk();
	}
}
