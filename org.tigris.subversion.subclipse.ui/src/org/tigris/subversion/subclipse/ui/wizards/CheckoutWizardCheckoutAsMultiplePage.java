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
package org.tigris.subversion.subclipse.ui.wizards;

import java.text.ParseException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.DepthComboHelper;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CheckoutWizardCheckoutAsMultiplePage extends WizardPage {
	private Label textLabel;
	private Button projectsButton;
	private Text projectNamePrefixText;
	private Text projectNameSuffixText;
	private Label previewText;
	private Button existingButton;
	private Text revisionText;
    private Button headButton;
	private Combo depthCombo;
	private Button ignoreExternalsButton;
	private Button forceButton;

    private static final int REVISION_WIDTH_HINT = 40;
    private static final int NAME_WIDTH_HINT = 60;

	public CheckoutWizardCheckoutAsMultiplePage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	public void createControl(Composite parent) {
		CheckoutWizard wizard = (CheckoutWizard)getWizard();

		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		textLabel = new Label(outerContainer, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		textLabel.setLayoutData(data);

		if (wizard.getRemoteFolders() != null) {
			textLabel.setText(Policy.bind("CheckoutWizardCheckoutAsPage.multiple", Integer.toString(wizard.getRemoteFolders().length))); //$NON-NLS-1$
		}

		projectsButton = new Button(outerContainer, SWT.RADIO);
		projectsButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.projects")); //$NON-NLS-1$

		Group projectGroup = new Group(outerContainer,SWT.NONE);
		projectGroup.setText(Policy.bind("CheckoutWizardCheckoutAsPage.projectNaming")); //$NON-NLS-1$
        GridLayout projectLayout = new GridLayout();
        projectLayout.numColumns = 4;
        projectGroup.setLayout(projectLayout);
        projectGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        projectNamePrefixText = new Text(projectGroup, SWT.BORDER);
        data = new GridData();
        data.widthHint = NAME_WIDTH_HINT;
        projectNamePrefixText.setLayoutData(data);
        projectNamePrefixText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                CheckoutWizard wizard = (CheckoutWizard)getWizard();
                wizard.setProjectNamePrefix(projectNamePrefixText.getText().trim());
                setPreview();
            }
        });
        Label projectTextLabel = new Label(projectGroup, SWT.NONE);
        projectTextLabel.setText("<default_name>"); //$NON-NLS-1$
        projectNameSuffixText = new Text(projectGroup, SWT.BORDER);
        data = new GridData();
        data.horizontalSpan = 2;
        data.widthHint = NAME_WIDTH_HINT;
        projectNameSuffixText.setLayoutData(data);
        projectNameSuffixText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                CheckoutWizard wizard = (CheckoutWizard)getWizard();
                wizard.setProjectNameSuffix(projectNameSuffixText.getText().trim());
                setPreview();
            }
        });
        
		Composite previewGroup = new Composite(projectGroup,SWT.NONE);
        GridLayout previewLayout = new GridLayout();
        previewLayout.numColumns = 2;
        previewGroup.setLayout(previewLayout);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.horizontalSpan = 4;
        previewGroup.setLayoutData(data);
        
        Label previewLabel = new Label(previewGroup, SWT.NONE);
        previewLabel.setText(Policy.bind("CheckoutWizardCheckoutAsPage.namePreview")); //$NON-NLS-1$

        previewText = new Label(previewGroup, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        previewText.setLayoutData(data);
        previewText.setText("default_name"); //$NON-NLS-1$
        
		existingButton = new Button(outerContainer, SWT.RADIO);
		existingButton.setText(Policy.bind("CheckoutWizardCheckoutAsPage.existing")); //$NON-NLS-1$
		existingButton.setEnabled(false);
		existingButton.setVisible(false);

		projectsButton.setSelection(true);

		Composite revisionGroup = new Composite(outerContainer, SWT.NULL);
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		revisionGroup.setLayoutData(data);

		headButton = new Button(revisionGroup, SWT.CHECK);
		headButton.setText(Policy.bind("CheckoutWizard.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);

		headButton.setSelection(true);

		Label revisionLabel = new Label(revisionGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("CheckoutWizard.revision")); //$NON-NLS-1$

		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		revisionText.setEnabled(false);

		Button logButton = new Button(revisionGroup, SWT.PUSH);
		logButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		logButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog();
            }
		});

		SelectionListener revisionListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                revisionText.setEnabled(!headButton.getSelection());
                if (!headButton.getSelection()) {
                    revisionText.selectAll();
                    revisionText.setFocus();
                }
            }
		};

		headButton.addSelectionListener(revisionListener);

		Group parameterGroup = new Group(outerContainer, SWT.NULL);
		GridLayout parameterLayout = new GridLayout();
		parameterLayout.numColumns = 2;
		parameterGroup.setLayout(parameterLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		parameterGroup.setLayoutData(data);

		Label depthLabel = new Label(parameterGroup, SWT.NONE);
		depthLabel.setText(Policy.bind("SvnDialog.depth")); //$NON-NLS-1$
		depthCombo = new Combo(parameterGroup, SWT.READ_ONLY);
		DepthComboHelper.addDepths(depthCombo, false, ISVNUIConstants.DEPTH_INFINITY);

		ignoreExternalsButton = new Button(parameterGroup, SWT.CHECK);
		ignoreExternalsButton.setText(Policy.bind("SvnDialog.ignoreExternals")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		ignoreExternalsButton.setLayoutData(data);

		forceButton = new Button(parameterGroup, SWT.CHECK);
		forceButton.setText(Policy.bind("SvnDialog.force")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		forceButton.setLayoutData(data);
		forceButton.setSelection(true);

		setMessage(Policy.bind("CheckoutWizardCheckoutAsPage.text")); //$NON-NLS-1$

		setControl(outerContainer);
	}

	public void setText(String text) {
		textLabel.setText(text);
	}
	
	private void setPreview() {
		previewText.setText(projectNamePrefixText.getText().trim() + "default_name" + projectNameSuffixText.getText().trim()); //$NON-NLS-1$
	}

	private void showLog() {
		HistoryDialog dialog = new HistoryDialog(getShell(), getCommonParent());
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
        revisionText.setEnabled(true);
        headButton.setSelection(false);
	}

	private ISVNRemoteResource getCommonParent() {
		ISVNRemoteFolder[] remoteFolders = ((CheckoutWizard)getWizard()).getRemoteFolders();
		if (remoteFolders.length == 1) return remoteFolders[0];
		ISVNRemoteResource commonParent = null;
		ISVNRemoteResource parent = remoteFolders[0];
		while (commonParent == null) {
			parent = parent.getParent();
			if (parent == null) break;
			for (int i = 1; i < remoteFolders.length; i++) {
				if (!remoteFolders[i].getUrl().toString().startsWith(parent.getUrl().toString()))
					break;
			}
			commonParent = parent;
		}
		return commonParent;
	}

	public SVNRevision getRevision() {
		if (headButton.getSelection()) return SVNRevision.HEAD;
		SVNRevision svnRevision = null;
			try {
				svnRevision = SVNRevision.getRevision(revisionText.getText().trim());
			} catch (ParseException e) {}
		if (svnRevision == null)
			return SVNRevision.HEAD;
		return svnRevision;
	}

	public int getDepth() {
		return DepthComboHelper.getDepth(depthCombo);
	}

	public boolean isIgnoreExternals() {
		return ignoreExternalsButton.getSelection();
	}

	public boolean isForce() {
		return forceButton.getSelection();
	}

}
