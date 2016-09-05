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
package org.tigris.subversion.subclipse.ui.authentication;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.SubclipseTrayDialog;

public class QuestionDialog extends SubclipseTrayDialog {
    private String realm;
    private String question;
    private boolean showAnswer;
    private boolean maySave;
    private Text answerText;
    private String answer;
    private boolean save;
    private Button saveButton;
    
    private boolean isPassphrasePrompt;
    private boolean isFilePrompt;
    
    private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
    
    private static final int WIDTH = 300;

    public QuestionDialog(Shell parentShell, String realm, String question, 
            boolean showAnswer, boolean maySave) {
        super(parentShell);
        this.realm = realm;
        this.question = question;
        this.showAnswer = showAnswer;
        this.maySave = maySave;
        if (question != null) {
        	isPassphrasePrompt = question.indexOf("certificate passphrase") != -1; //$NON-NLS-1$
        	if (!isPassphrasePrompt) {
        		isFilePrompt = question.indexOf("certificate file") != -1; //$NON-NLS-1$
        	}
        }
    }
    
	protected Control createDialogArea(Composite parent) {
	    Composite rtnGroup = (Composite)super.createDialogArea(parent);
	    getShell().setText(Policy.bind("SVNPromptUserPassword.authentication")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rtnGroup.setLayout(layout);
		rtnGroup.setLayoutData(
		new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		
		Group realmGroup = new Group(rtnGroup, SWT.NONE);
		if (isPassphrasePrompt) { 
			realmGroup.setText(Policy.bind("QuestionDialog.clientCertificateFile")); //$NON-NLS-1$
		} else {
			realmGroup.setText(Policy.bind("PasswordPromptDialog.repository")); //$NON-NLS-1$
		}
		GridLayout realmLayout = new GridLayout();
		realmLayout.numColumns = 1;
		realmGroup.setLayout(realmLayout);
		realmGroup.setLayoutData(
		new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		Text realmText = new Text(realmGroup, SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = WIDTH;
		realmText.setLayoutData(gd);
		realmText.setEditable(false);
		realmText.setText(realm);
		
		Group questionGroup = new Group(rtnGroup, SWT.NONE);
		questionGroup.setText(question);
		GridLayout questionLayout = new GridLayout();
		questionLayout.numColumns = 2;
		questionGroup.setLayout(questionLayout);
		questionGroup.setLayoutData(
		new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		answerText = new Text(questionGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		answerText.setLayoutData(gd);

		if (!showAnswer) answerText.setEchoChar('*'); //$NON-NLS-1$
		
		if (isFilePrompt) { 
			String previousCertificateFile = null;
			try {
				previousCertificateFile = settings.get("QuestionDialog.certificateFile." + realm);
				if (previousCertificateFile != null) {
					answerText.setText(previousCertificateFile);
				}
			} catch (Exception e) {}
		}
		
		if (isFilePrompt) { 
			Button browseButton = new Button(questionGroup, SWT.PUSH);
			browseButton.setText(Policy.bind("browse")); //$NON-NLS-1$
			browseButton.addSelectionListener(new SelectionAdapter() {	
				public void widgetSelected(SelectionEvent e) {
					FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
					fileDialog.setText("Select Client Certificate File");
					String returnFile = fileDialog.open();
					if (returnFile != null) answerText.setText(returnFile);
				}
			});
		}
		
		if (maySave) {
		    saveButton = new Button(rtnGroup, SWT.CHECK);
		    saveButton.setText(Policy.bind("QuestionDialog.save")); //$NON-NLS-1$
		    gd = new GridData();
		    gd.horizontalSpan = 2;
		    saveButton.setLayoutData(gd);
		}
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(rtnGroup, IHelpContextIds.QUESTION_DIALOG);	

		answerText.setFocus();
		
		return rtnGroup;
	}
	
    protected void okPressed() {
    	answer = answerText.getText().trim();
        if (maySave) save = saveButton.getSelection();
        if (isFilePrompt) { //$NON-NLS-1$
        	settings.put("QuestionDialog.certificateFile." + realm, answer); //$NON-NLS-1$
        }
        super.okPressed();
    }	

    public String getAnswer() {
        return answer;
    }
    public boolean isSave() {
        return save;
    }
}
