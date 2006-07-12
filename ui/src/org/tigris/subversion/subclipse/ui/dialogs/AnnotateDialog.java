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

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ShowAnnotationOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class AnnotateDialog extends TrayDialog {
	private IWorkbenchPart targetPart;
	private ISVNRemoteFile remoteFile;
	private Text fromRevisionText;
	private Button fromLogButton;
	private Button headButton;
	private Button revisionButton;
	private Text toRevisionText;
	private Button toLogButton;
	private Button okButton;
	private boolean success;

	public AnnotateDialog(Shell parentShell, IWorkbenchPart targetPart, ISVNRemoteFile remoteFile) {
		super(parentShell);
		this.remoteFile = remoteFile;
		this.targetPart = targetPart;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("AnnotateDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite urlGroup = new Composite(composite, SWT.NULL);
		GridLayout urlLayout = new GridLayout();
		urlLayout.numColumns = 2;
		urlGroup.setLayout(urlLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		urlGroup.setLayoutData(data);
		
		Label urlLabel = new Label(urlGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("AnnotateDialog.url")); //$NON-NLS-1$
		Text urlText = new Text(urlGroup, SWT.BORDER);
		urlText.setEditable(false);
		data = new GridData();
		data.widthHint = 300;
		urlText.setLayoutData(data);
		urlText.setText(remoteFile.getUrl().toString());
		
		Group fromGroup = new Group(composite, SWT.NULL);
		fromGroup.setText(Policy.bind("AnnotateDialog.from")); //$NON-NLS-1$
		GridLayout fromLayout = new GridLayout();
		fromLayout.numColumns = 3;
		fromGroup.setLayout(fromLayout);
		data = new GridData(GridData.FILL_BOTH);
		fromGroup.setLayoutData(data);
		
		Label fromRevisionLabel = new Label(fromGroup, SWT.NONE);
		fromRevisionLabel.setText(Policy.bind("AnnotateDialog.revision")); //$NON-NLS-1$
		fromRevisionText = new Text(fromGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		fromRevisionText.setLayoutData(data);
		fromRevisionText.setText("1"); //$NON-NLS-1$
		
		fromLogButton = new Button(fromGroup, SWT.PUSH);
		fromLogButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.showLog")); //$NON-NLS-1$
		fromLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
		});
		
		Group toGroup = new Group(composite, SWT.NULL);
		toGroup.setText(Policy.bind("AnnotateDialog.to")); //$NON-NLS-1$
		GridLayout toLayout = new GridLayout();
		toLayout.numColumns = 3;
		toGroup.setLayout(toLayout);
		data = new GridData(GridData.FILL_BOTH);
		toGroup.setLayoutData(data);
		
		headButton = new Button(toGroup, SWT.RADIO);
		headButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		revisionButton = new Button(toGroup, SWT.RADIO);
		revisionButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$
		
		headButton.setSelection(true);
		
		toRevisionText = new Text(toGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		toRevisionText.setLayoutData(data);
		toRevisionText.setEnabled(false);
		
		toLogButton = new Button(toGroup, SWT.PUSH);
		toLogButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.showLog")); //$NON-NLS-1$
		toLogButton.setEnabled(false);
		toLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
		});			
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();
			}		
		};
		
		fromRevisionText.addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toRevisionText.setEnabled(revisionButton.getSelection());
				toLogButton.setEnabled(revisionButton.getSelection());
				if (revisionButton.getSelection()) {
					toRevisionText.selectAll();
					toRevisionText.setFocus();
				}
				setOkButtonStatus();
			}
		};
		
		headButton.addSelectionListener(selectionListener);
		revisionButton.addSelectionListener(selectionListener);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.ANNOTATE_DIALOG);	

		fromRevisionText.selectAll();
		fromRevisionText.setFocus();
		
		return composite;
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
		}
        return button;
    }
	
	protected void okPressed() {
		success = true;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					int fromRevisionInt = Integer.parseInt(fromRevisionText.getText().trim());
					long fromRevisionLong = fromRevisionInt;
					SVNRevision fromRevision = new SVNRevision.Number(fromRevisionLong);
					SVNRevision toRevision = null;
					if (headButton.getSelection()) toRevision = SVNRevision.HEAD;
					else {
						int toRevisionInt = Integer.parseInt(toRevisionText.getText().trim());
						long toRevisionLong = toRevisionInt;
						toRevision = new SVNRevision.Number(toRevisionLong);
					}

					new ShowAnnotationOperation(targetPart, remoteFile, fromRevision, toRevision).run();
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("AnnotateDialog.title"), e.getMessage());
					success = false;
				}
			}			
		});
		if (!success) return;
		super.okPressed();
	}

	private void setOkButtonStatus() {
		boolean canFinish = true;
		if (fromRevisionText.getText().trim().length() == 0) canFinish = false;
		if (revisionButton.getSelection() && toRevisionText.getText().trim().length() == 0) canFinish = false;
		okButton.setEnabled(canFinish);
	}
	
	private void showLog(Object sourceButton) {
		HistoryDialog dialog = new HistoryDialog(getShell(), remoteFile);
		if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        if (sourceButton == fromLogButton) fromRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        else toRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setOkButtonStatus();
	}

}
