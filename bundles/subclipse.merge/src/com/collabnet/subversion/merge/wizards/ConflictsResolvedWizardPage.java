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
package com.collabnet.subversion.merge.wizards;

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
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.Messages;

public class ConflictsResolvedWizardPage extends WizardPage {
	private MergeOutput mergeOutput;
	private Button textConflictPromptButton;
	private Button textConflictMarkButton;

	private Button binaryConflictPromptButton;
	private Button binaryConflictMarkButton;
	private Button binaryConflictUserButton;
	private Button binaryConflictIncomingButton;
	
	private IDialogSettings settings;
	
	private static final String LAST_TEXT_CONFLICT_CHOICE = "MergeWizardMainPage.lastTextConflictChoice"; //$NON-NLS-1$
	private static final String LAST_BINARY_CONFLICT_CHOICE = "MergeWizardMainPage.lastBinaryConflictChoice";		 //$NON-NLS-1$

	public ConflictsResolvedWizardPage(String pageName) {
		super(pageName, Messages.ConflictsResolvedWizardPage_allResolved, Activator.getDefault().getImageDescriptor(Activator.IMAGE_SVN));
		settings = Activator.getDefault().getDialogSettings();	
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
		
		Label label = new Label(composite, SWT.NONE);
		if (mergeOutput.isIncomplete()) {
			label.setText(Messages.ConflictsResolvedWizardPage_confirmResume);
			new Label(composite, SWT.NONE);
			Group conflictGroup = new Group(composite, SWT.NONE);
			conflictGroup.setText(Messages.ConflictsResolvedWizardPage_conflictHandling);
			GridLayout conflictLayout = new GridLayout();
			conflictLayout.numColumns = 1;
			conflictGroup.setLayout(conflictLayout);
			conflictGroup.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
			
			Group textGroup = new Group(conflictGroup, SWT.NONE);
			textGroup.setText(Messages.ConflictsResolvedWizardPage_textFiles);
			GridLayout textLayout = new GridLayout();
			textLayout.numColumns = 1;
			textGroup.setLayout(textLayout);
			textGroup.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
			
			textConflictPromptButton = new Button(textGroup, SWT.RADIO);
			textConflictPromptButton.setText(Messages.ConflictsResolvedWizardPage_prompt);
			textConflictMarkButton = new Button(textGroup, SWT.RADIO);
			textConflictMarkButton.setText(Messages.ConflictsResolvedWizardPage_mark);
			
			Group binaryGroup = new Group(conflictGroup, SWT.NONE);
			binaryGroup.setText(Messages.ConflictsResolvedWizardPage_binaryFiles);
			GridLayout binaryLayout = new GridLayout();
			binaryLayout.numColumns = 1;
			binaryGroup.setLayout(binaryLayout);
			binaryGroup.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
			
			binaryConflictPromptButton = new Button(binaryGroup, SWT.RADIO);
			binaryConflictPromptButton.setText(Messages.ConflictsResolvedWizardPage_prompt);
			binaryConflictMarkButton = new Button(binaryGroup, SWT.RADIO);
			binaryConflictMarkButton.setText(Messages.ConflictsResolvedWizardPage_mark);
			binaryConflictUserButton = new Button(binaryGroup, SWT.RADIO);
			binaryConflictUserButton.setText(Messages.ConflictsResolvedWizardPage_useMine);
			binaryConflictIncomingButton = new Button(binaryGroup, SWT.RADIO);
			binaryConflictIncomingButton.setText(Messages.ConflictsResolvedWizardPage_useIncoming);

			SelectionListener conflictSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (textConflictMarkButton.getSelection()) settings.put(LAST_TEXT_CONFLICT_CHOICE, ISVNConflictResolver.Choice.postpone);
					else settings.put(LAST_TEXT_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
					if (binaryConflictIncomingButton.getSelection()) settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseTheirsFull);
					else if (binaryConflictUserButton.getSelection()) settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMineFull);
					else if (binaryConflictMarkButton.getSelection()) settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.postpone);
					else settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMerged);				
				}		
			};
			
			textConflictMarkButton.addSelectionListener(conflictSelectionListener);
			textConflictPromptButton.addSelectionListener(conflictSelectionListener);
			binaryConflictIncomingButton.addSelectionListener(conflictSelectionListener);
			binaryConflictUserButton.addSelectionListener(conflictSelectionListener);
			binaryConflictMarkButton.addSelectionListener(conflictSelectionListener);
			binaryConflictPromptButton.addSelectionListener(conflictSelectionListener);							

			int lastTextConflictChoice = ISVNConflictResolver.Choice.chooseMerged;
			try {
				lastTextConflictChoice = settings.getInt(LAST_TEXT_CONFLICT_CHOICE);
			} catch (Exception e) {}
			
			switch (lastTextConflictChoice) {
			case ISVNConflictResolver.Choice.chooseMerged:
				textConflictPromptButton.setSelection(true);
				break;
			case ISVNConflictResolver.Choice.postpone:
				textConflictMarkButton.setSelection(true);
				break;	
			default:
				textConflictPromptButton.setSelection(true);
				break;
			}
			
			int lastBinaryConflictChoice = ISVNConflictResolver.Choice.chooseMerged;
			try {
				lastBinaryConflictChoice = settings.getInt(LAST_BINARY_CONFLICT_CHOICE);
			} catch (Exception e) {}
			
			switch (lastBinaryConflictChoice) {
			case ISVNConflictResolver.Choice.chooseMerged:
				binaryConflictPromptButton.setSelection(true);
				break;
			case ISVNConflictResolver.Choice.postpone:
				binaryConflictMarkButton.setSelection(true);
				break;
			case ISVNConflictResolver.Choice.chooseMineFull:
				binaryConflictUserButton.setSelection(true);
				break;	
			case ISVNConflictResolver.Choice.chooseTheirsFull:
				binaryConflictIncomingButton.setSelection(true);
				break;			
			default:
				binaryConflictPromptButton.setSelection(true);
				break;
			}		
		
		} else {
			label.setText(Messages.ConflictsResolvedWizardPage_confirmDelete);
		}
		
		setMessage(Messages.ConflictsResolvedWizardPage_noMoreConflicts);
		
		setControl(outerContainer);
	}

	public void setMergeOutput(MergeOutput mergeOutput) {
		this.mergeOutput = mergeOutput;
	}

}
