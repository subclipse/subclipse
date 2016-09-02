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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class CommentTemplatesPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage, ISelectionChangedListener {

	private ListViewer viewer;
	private Button editButton;
	private Button removeButton;
	private Text preview;
	private Text commentsText;
	
	public static final int MAX_COMMENTS_TO_SAVE = 100;

	protected Control createContents(Composite ancestor) {
		Composite parent = new Composite(ancestor, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite templateGroup = createListAndButtons(parent);

		Group previewGroup = new Group(templateGroup, SWT.NONE);
		previewGroup.setText(Policy.bind("CommentTemplatesPreferencePage.Preview")); //$NON-NLS-1$
		GridLayout previewLayout = new GridLayout();
		previewLayout.marginWidth = 0;
		previewLayout.marginHeight = 0;
		previewGroup.setLayout(previewLayout);
		previewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		preview = new Text(previewGroup, SWT.MULTI | SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = convertHeightInCharsToPixels(5);
		preview.setLayoutData(gd);
		
		Composite commentsGroup = new Composite(parent, SWT.NONE);
		GridLayout commentsLayout = new GridLayout();
		commentsLayout.marginWidth = 0;
		commentsLayout.marginHeight = 0;
		commentsLayout.numColumns = 3;
		commentsGroup.setLayout(commentsLayout);
		commentsGroup.setLayoutData(new GridData());
		
		Label commentsLabel = new Label(commentsGroup, SWT.NONE);
		commentsLabel.setText(Policy.bind("CommentTemplatesPreferencePage.0")); //$NON-NLS-1$
		
		commentsText = new Text(commentsGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = 50;
		commentsText.setLayoutData(gd);
		commentsText.setText(Integer.toString(SVNUIPlugin.getPlugin().getPreferenceStore().getInt(ISVNUIConstants.PREF_COMMENTS_TO_SAVE)));
		
		commentsText.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
		});
		commentsText.addVerifyListener(new VerifyListener() {			
			public void verifyText(VerifyEvent e) {
		    	String text = e.text;
		    	for (int i = 0; i < text.length(); i++) {
		    		if ("0123456789".indexOf(text.substring(i, i+1)) == -1) { //$NON-NLS-1$
		    			e.doit = false;
		    			break;
		    		}
		    	}
			}
		});
		commentsText.addModifyListener(new ModifyListener() {			
			public void modifyText(ModifyEvent e) {
				if (getCommentsToSave() > MAX_COMMENTS_TO_SAVE) {
					setValid(false);
					setErrorMessage(Policy.bind("CommentTemplatesPreferencePage.1") + " " + MAX_COMMENTS_TO_SAVE + "."); //$NON-NLS-1$
				}
				else {
					setValid(true);
					setErrorMessage(null);
				}
			}
		});
		
		Label rangeLabel = new Label(commentsGroup, SWT.NONE);
		rangeLabel.setText("(0-" + MAX_COMMENTS_TO_SAVE + ")");
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.COMMENT_TEMPLATE_PREFERENCE_PAGE);
		Dialog.applyDialogFont(ancestor);
		
		return parent;
	}

	private Composite createListAndButtons(Composite parent) {
		Group templateGroup = new Group(parent, SWT.NONE);
		templateGroup.setText(Policy.bind("CommentTemplatesPreferencePage.2")); //$NON-NLS-1$
		GridLayout templateLayout = new GridLayout();
		templateLayout.marginWidth = 0;
		templateLayout.marginHeight = 0;
		templateGroup.setLayout(templateLayout);
		templateGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite listAndButtons = new Composite(templateGroup, SWT.NONE);
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
		return templateGroup;
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
		removeButton.setText(Policy.bind("CommentTemplatesPreferencePage.Remove")); //$NON-NLS-1$
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
	
	public void init(IWorkbench workbench) {}

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
				Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateTitle"), //$NON-NLS-1$
				Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateMessage"), //$NON-NLS-1$
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
					Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateTitle"), //$NON-NLS-1$
					Policy.bind("CommentTemplatesPreferencePage.EditCommentTemplateMessage"), //$NON-NLS-1$
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
		
		SVNUIPlugin.getPlugin().getPreferenceStore().setValue(ISVNUIConstants.PREF_COMMENTS_TO_SAVE, getCommentsToSave());
		
		return super.performOk();
	}
	
	private int getCommentsToSave() {
		int commentsToRemember;
		if (commentsText.getText().trim().length() == 0) {
			commentsToRemember = 0;
		}
		else {
			commentsToRemember = Integer.parseInt(commentsText.getText());
		}
		return commentsToRemember;
	}
}
