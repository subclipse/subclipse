/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;

public class BranchTagPropertyUpdateDialog extends SvnDialog {
	private IResource resource;
	private Alias newAlias;
	private ISVNLocalResource svnResource;
	private Text revisionText;
	private Text nameText;
	private Text pathText;
	private Button branchButton;
	private Button okButton;

	public BranchTagPropertyUpdateDialog(Shell parentShell, IResource resource, Alias newAlias, String id) {
		super(parentShell, id);
		this.resource = resource;
		this.newAlias = newAlias;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("BranchTagPropertyUpdateDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));		
		
		Composite urlGroup = new Composite(composite, SWT.NONE);
		GridLayout urlLayout = new GridLayout();
		urlLayout.numColumns = 2;
		urlGroup.setLayout(urlLayout);
		urlGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label urlLabel = new Label(urlGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("ConfigureTagsDialog.url")); //$NON-NLS-1$
		Text urlText = new Text(urlGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);		
		try {
			urlText.setText(svnResource.getStatus().getUrlString());
		} catch (SVNException e) {}
		
		Label textLabel = createWrappingLabel(composite, Policy.bind("BranchTagPropertyUpdateDialog.text"), 0 /* indent */, 1 /* columns */); //$NON-NLS-1$
		textLabel.setText(Policy.bind("BranchTagPropertyUpdateDialog.text")); //$NON-NLS-1$
		
		Group tagGroup = new Group(composite, SWT.NONE);
		GridLayout tagLayout = new GridLayout();
		tagLayout.numColumns = 2;
		tagGroup.setLayout(tagLayout);
		tagGroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
		
		Label revisionLabel = new Label(tagGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("ConfigureTagsDialog.revision")); //$NON-NLS-1$
		revisionText = new Text(tagGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 50;
		revisionText.setLayoutData(data);
		revisionText.setText(Integer.toString(newAlias.getRevision()));
		
		Label nameLabel = new Label(tagGroup, SWT.NONE);
		nameLabel.setText(Policy.bind("ConfigureTagsDialog.name")); //$NON-NLS-1$
		nameText = new Text(tagGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		nameText.setLayoutData(data);
		nameText.setText(newAlias.getName());
		
		Label pathLabel = new Label(tagGroup, SWT.NONE);
		pathLabel.setText(Policy.bind("ConfigureTagsDialog.path")); //$NON-NLS-1$
		pathText = new Text(tagGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		pathText.setLayoutData(data);
		pathText.setText(newAlias.getRelativePath());
		
		branchButton = new Button(tagGroup, SWT.CHECK);
		branchButton.setText(Policy.bind("ConfigureTagsDialog.branch")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		branchButton.setLayoutData(data);
		branchButton.setSelection(newAlias.isBranch());
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				okButton.setEnabled(revisionText.getText().trim().length() > 0 &&
						nameText.getText().trim().length() > 0 &&
						pathText.getText().trim().length() > 0);
			}			
		};
		
		revisionText.addModifyListener(modifyListener);
		nameText.addModifyListener(modifyListener);
		pathText.addModifyListener(modifyListener);
		
		FocusListener focusListener = new FocusListener() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}		
		};
		
		revisionText.addFocusListener(focusListener);
		nameText.addFocusListener(focusListener);
		pathText.addFocusListener(focusListener);

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.BRANCH_TAG_PROPERTY_UPDATE_DIALOG);	

		return composite;
	}

	protected void okPressed() {
		newAlias.setBranch(branchButton.getSelection());
		newAlias.setName(nameText.getText().trim());
		newAlias.setRelativePath(pathText.getText().trim());
		newAlias.setRevision(Integer.parseInt(revisionText.getText().trim()));
		super.okPressed();
	}
	
	protected Button createButton(
			Composite parent,
			int id,
			String label,
			boolean defaultButton) {
			Button button = super.createButton(parent, id, label, defaultButton);
			if (id == IDialogConstants.OK_ID) okButton = button;
			return button;
		}

	public Alias getNewAlias() {
		return newAlias;
	}	   

}
