/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.MergeFileAssociation;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class ConflictHandlingWizardPage extends WizardPage {
	private SVNConflictDescriptor conflictDescriptor;
	private IResource resource;
	private Button markConflictedButton;
	private Button chooseUserVersionButton;
	private Button chooseUserVersionForConflictsButton;
	private Button chooseIncomingVersionButton;
	private Button chooseIncomingVersionForConflictsButton;
	private Button chooseBaseVersionButton;
	private Button fileEditorButton;
	private Button conflictEditorButton;
	private Button applyToAllButton;
	private IDialogSettings settings;
	private final static String LAST_TEXT_CHOICE = "ConflictHandlingDialog.lastTextChoice"; //$NON-NLS-1$
	private final static String LAST_BINARY_CHOICE = "ConflictHandlingDialog.lastBinaryChoice";	 //$NON-NLS-1$

	public ConflictHandlingWizardPage(String pageName) {
		super(pageName, Messages.ConflictHandlingWizardPage_0, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN));
		settings = SVNUIPlugin.getPlugin().getDialogSettings();	
	}

	public void createControl(Composite parent) {	
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);	

		IResource conflictResource = SVNWorkspaceRoot.getResourceFor(this.resource, new Path(conflictDescriptor.getPath()));
		Label label = new Label(composite, SWT.WRAP);
		
		if (conflictResource == null) {
			String workspaceLocation = resource.getWorkspace().getRoot().getLocation().toString();
			String relativePath;
			if (conflictDescriptor.getPath().startsWith(workspaceLocation))
				relativePath = conflictDescriptor.getPath().substring(workspaceLocation.length() + 1);
			else
				relativePath = conflictDescriptor.getPath();
			label.setText(Messages.ConflictHandlingWizardPage_1 + convertTempFileName(relativePath));
		}
		else label.setText(Messages.ConflictHandlingWizardPage_2 + conflictResource.getFullPath().makeRelative().toOSString());
		
		data = new GridData();
		data.widthHint = 500;
		label.setLayoutData(data);
		
		new Label(composite, SWT.NONE);
		
		Group conflictGroup = new Group(composite, SWT.NULL);
		
		conflictGroup.setText(Messages.ConflictHandlingWizardPage_3);
		GridLayout conflictLayout = new GridLayout();
		conflictLayout.numColumns = 1;
		conflictGroup.setLayout(conflictLayout);
		data = new GridData(GridData.FILL_BOTH);
		conflictGroup.setLayoutData(data);
		
		markConflictedButton = new Button(conflictGroup, SWT.RADIO);
		markConflictedButton.setText(Messages.ConflictHandlingWizardPage_4);
		
		if (conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.deleted || conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.moved_away) {
			chooseUserVersionButton = new Button(conflictGroup, SWT.RADIO);
			chooseUserVersionButton.setText(Messages.ConflictHandlingWizardPage_18);
			chooseIncomingVersionButton = new Button(conflictGroup, SWT.RADIO);
			chooseIncomingVersionButton.setText(Messages.ConflictHandlingWizardPage_19);
		}
		else {
			if (conflictDescriptor.getMyPath() != null) {
				chooseUserVersionButton = new Button(conflictGroup, SWT.RADIO);
				if (conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property) {
					chooseUserVersionButton.setText(Messages.ConflictHandlingWizardPage_5);
				} else {
					chooseUserVersionButton.setText(Messages.ConflictHandlingWizardPage_6);
					if (!conflictDescriptor.isBinary()) {
						chooseUserVersionForConflictsButton = new Button(conflictGroup, SWT.RADIO);
						chooseUserVersionForConflictsButton.setText(Messages.ConflictHandlingWizardPage_16);
					}
				}
			}
			if (conflictDescriptor.getTheirPath() != null) {
				chooseIncomingVersionButton = new Button(conflictGroup, SWT.RADIO);
				if (conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property) {
					chooseIncomingVersionButton.setText(Messages.ConflictHandlingWizardPage_7);		
				} else {
					chooseIncomingVersionButton.setText(Messages.ConflictHandlingWizardPage_8);
					if (!conflictDescriptor.isBinary()) {
						chooseIncomingVersionForConflictsButton = new Button(conflictGroup, SWT.RADIO);
						chooseIncomingVersionForConflictsButton.setText(Messages.ConflictHandlingWizardPage_17);
					}
				}
			}
			if (!conflictDescriptor.isBinary()) {
				if (conflictDescriptor.getConflictKind() != SVNConflictDescriptor.Kind.property && fileExists(conflictDescriptor.getMergedPath())) {
					fileEditorButton = new Button(conflictGroup, SWT.RADIO);
					fileEditorButton.setText(Messages.ConflictHandlingWizardPage_9);
				}
			}
			if (showConflictEditorOption()) {
				conflictEditorButton = new Button(conflictGroup, SWT.RADIO);
				if (conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property && conflictDescriptor.getMergedPath() == null)
					conflictEditorButton.setText(Messages.ConflictHandlingWizardPage_10);	
				else
					conflictEditorButton.setText(Messages.ConflictHandlingWizardPage_11);	
			}
		}
		int lastChoice = ISVNConflictResolver.Choice.postpone;
		try {
			if (conflictDescriptor.isBinary()) lastChoice = settings.getInt(LAST_BINARY_CHOICE);
			else lastChoice = settings.getInt(LAST_TEXT_CHOICE);
		} catch (Exception e) {}
		if (lastChoice == ISVNConflictResolver.Choice.postpone) markConflictedButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseMine && chooseUserVersionForConflictsButton != null) chooseUserVersionForConflictsButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseMineFull && chooseUserVersionButton != null) chooseUserVersionButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseTheirs && chooseIncomingVersionForConflictsButton != null) chooseIncomingVersionForConflictsButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseTheirsFull && chooseIncomingVersionButton != null) chooseIncomingVersionButton.setSelection(true);
		else if (lastChoice == ISVNConflictResolver.Choice.chooseBase && chooseBaseVersionButton != null) chooseBaseVersionButton.setSelection(true);
		if (lastChoice == ConflictResolution.CONFLICT_EDITOR && conflictEditorButton != null) conflictEditorButton.setSelection(true);
		if (!conflictDescriptor.isBinary()) {
			if (conflictDescriptor.getConflictKind() != SVNConflictDescriptor.Kind.property && lastChoice == ConflictResolution.FILE_EDITOR) fileEditorButton.setSelection(true);
		}		
			
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (conflictDescriptor.isBinary()) {
					if (markConflictedButton.getSelection()) settings.put(LAST_BINARY_CHOICE, ISVNConflictResolver.Choice.postpone);
					else if (chooseUserVersionButton != null && chooseUserVersionButton.getSelection()) settings.put(LAST_BINARY_CHOICE, ISVNConflictResolver.Choice.chooseMineFull);
					else if (chooseIncomingVersionButton != null && chooseIncomingVersionButton.getSelection()) settings.put(LAST_BINARY_CHOICE, ISVNConflictResolver.Choice.chooseTheirsFull);
					else if (chooseBaseVersionButton != null && chooseBaseVersionButton.getSelection()) settings.put(LAST_BINARY_CHOICE, ISVNConflictResolver.Choice.chooseBase);
				} else {
					if (markConflictedButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ISVNConflictResolver.Choice.postpone);
					else if (chooseUserVersionButton != null && chooseUserVersionButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ISVNConflictResolver.Choice.chooseMineFull);
					else if (chooseUserVersionForConflictsButton != null && chooseUserVersionForConflictsButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ISVNConflictResolver.Choice.chooseMine);
					else if (chooseIncomingVersionButton != null && chooseIncomingVersionButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ISVNConflictResolver.Choice.chooseTheirsFull);
					else if (chooseIncomingVersionForConflictsButton != null && chooseIncomingVersionForConflictsButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ISVNConflictResolver.Choice.chooseTheirs);
					else if (chooseBaseVersionButton != null && chooseBaseVersionButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ISVNConflictResolver.Choice.chooseBase);
					else if (fileEditorButton != null && fileEditorButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ConflictResolution.FILE_EDITOR);
				}
				if (conflictEditorButton != null && conflictEditorButton.getSelection()) settings.put(LAST_TEXT_CHOICE, ConflictResolution.CONFLICT_EDITOR);
			}			
		};
		
		markConflictedButton.addSelectionListener(selectionListener);
		if (chooseIncomingVersionButton != null) chooseIncomingVersionButton.addSelectionListener(selectionListener);
		if (chooseIncomingVersionForConflictsButton != null) chooseIncomingVersionForConflictsButton.addSelectionListener(selectionListener);
		if (chooseUserVersionButton != null) chooseUserVersionButton.addSelectionListener(selectionListener);
		if (chooseUserVersionForConflictsButton != null) chooseUserVersionForConflictsButton.addSelectionListener(selectionListener);
		if (chooseBaseVersionButton != null) chooseBaseVersionButton.addSelectionListener(selectionListener);
		if (!conflictDescriptor.isBinary()) {
			if (fileEditorButton != null) fileEditorButton.addSelectionListener(selectionListener);
		}		
		if (conflictEditorButton != null) conflictEditorButton.addSelectionListener(selectionListener);
		
		applyToAllButton = new Button(composite, SWT.CHECK);
		if (conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.deleted || conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.moved_away)
			applyToAllButton.setText(Messages.ConflictHandlingWizardPage_20);
		else if (conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property)
			applyToAllButton.setText(Messages.ConflictHandlingWizardPage_12);
		else if (conflictDescriptor.isBinary())
			applyToAllButton.setText(Messages.ConflictHandlingWizardPage_13);
		else
			applyToAllButton.setText(Messages.ConflictHandlingWizardPage_14);
		
		File file = new File(conflictDescriptor.getPath());
		if (conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.deleted || conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.moved_away)
			setMessage("Tree conflict on " + convertTempFileName(file.getName()) + "."); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
		else if (conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property)
			setMessage("SVN cannot automatically merge property " + conflictDescriptor.getPropertyName() + Messages.ConflictHandlingWizardPage_15 + convertTempFileName(file.getName()) + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else
			setMessage("SVN cannot automatically merge file " + convertTempFileName(file.getName()) + "."); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
		
		setControl(outerContainer);
	}

	public void setConflictDescriptor(SVNConflictDescriptor conflictDescriptor) {
		this.conflictDescriptor = conflictDescriptor;
	}
	
	public ConflictResolution getConflictResolution() {
		int resolution = ISVNConflictResolver.Choice.postpone;
		if (markConflictedButton.getSelection()) resolution = ISVNConflictResolver.Choice.postpone;
		else if (chooseIncomingVersionButton != null && chooseIncomingVersionButton.getSelection()) {
			if (conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.deleted || conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.moved_away) {
				resolution = ISVNConflictResolver.Choice.chooseMerged;
			}
			else {
				resolution = ISVNConflictResolver.Choice.chooseTheirsFull;
			}
		}
		else if (chooseIncomingVersionForConflictsButton != null && chooseIncomingVersionForConflictsButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseTheirs;
		else if (chooseUserVersionButton != null && chooseUserVersionButton.getSelection()) {
			if (conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.deleted || conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.moved_away) {
				resolution = ISVNConflictResolver.Choice.chooseMine;
			}
			else {	
				resolution = ISVNConflictResolver.Choice.chooseMineFull;
			}
		}
		else if (chooseUserVersionForConflictsButton != null && chooseUserVersionForConflictsButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseMine;
		else if (chooseBaseVersionButton != null && chooseBaseVersionButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseBase;
	    if (!conflictDescriptor.isBinary()) {
	    	if (fileEditorButton != null && fileEditorButton.getSelection()) resolution = ConflictResolution.FILE_EDITOR;
	    }
	    if (conflictEditorButton != null && conflictEditorButton.getSelection()) resolution = ConflictResolution.CONFLICT_EDITOR;
	    ConflictResolution conflictResolution = new ConflictResolution(conflictDescriptor, resolution);
	    conflictResolution.setApplyToAll(applyToAllButton.getSelection());
		return conflictResolution;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}
	
	// This is a workaround for an SVNKit bug that is causing the
	// conflict descriptor path to come in as a temp file path
	// rather than the actual conflicted resource path.
	private String convertTempFileName(String name) {
		if (name.endsWith(".tmp")) { //$NON-NLS-1$
			String newName = name.substring(0, name.length() - 4);
			int index = newName.lastIndexOf("."); //$NON-NLS-1$
			if (index != -1) {
				if (newName.substring(index).indexOf("/") == -1) { //$NON-NLS-1$
					newName = newName.substring(0, index);
					return newName;
				}
			}
		}
		return name;
	}
	
	private boolean fileExists(String path) {
		if (path != null && path.length() > 0) {
			File file = new File(path);
			return file.exists();
		}
		return false;
	}
	
	private boolean showConflictEditorOption() {
		if (!fileExists(conflictDescriptor.getBasePath())) {
			return false;
		}
		if (conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.deleted || conflictDescriptor.getReason() == SVNConflictDescriptor.Reason.moved_away) {
			return false;
		}
		if (conflictDescriptor.isBinary()) {
			File pathFile = new File(conflictDescriptor.getPath());
			try {
				MergeFileAssociation[] mergeFileAssociations = SVNUIPlugin.getPlugin().getMergeFileAssociations();
				if (mergeFileAssociations != null) {
					for (int i = 0; i < mergeFileAssociations.length; i++) {
						if (mergeFileAssociations[i].matches(pathFile.getName()) || mergeFileAssociations[i].getFileType().equals(pathFile.getName())) {
							return true;
						}
					}
				}
			} catch (Exception e) {}
			return false;
		}
		return true;
	}

}
