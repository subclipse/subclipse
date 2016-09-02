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

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ExportRemoteFolderOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class ExportRemoteFolderDialog extends SvnDialog {
	private ISVNRemoteResource remoteResource;
	private IWorkbenchPart targetPart;
	private Text directoryText;
	private Text revisionText;
    private Button logButton;
    private Button headButton;

	public ExportRemoteFolderDialog(Shell parentShell, ISVNRemoteResource remoteResource, IWorkbenchPart targetPart) {
		super(parentShell, "ExportRemoteFolderDialog"); //$NON-NLS-1$
		this.remoteResource = remoteResource;
		this.targetPart = targetPart;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("ExportRemoteFolderAction.directoryDialogText")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Composite repositoryGroup = new Composite(composite, SWT.NULL);		
		GridLayout repositoryLayout = new GridLayout();
		repositoryLayout.numColumns = 2;
		repositoryGroup.setLayout(repositoryLayout);
		data = new GridData(GridData.FILL_BOTH);
		repositoryGroup.setLayoutData(data);
		
		Label urlLabel = new Label(repositoryGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("ExportRemoteFolderDialog.url"));
		data = new GridData();
		data.horizontalSpan = 2;
		urlLabel.setLayoutData(data);
		
		Text urlText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		urlText.setText(remoteResource.getUrl().toString());
		
		Label directoryLabel = new Label(repositoryGroup, SWT.NONE);
		directoryLabel.setText(Policy.bind("ExportRemoteFolderDialog.directory"));
		data = new GridData();
		data.horizontalSpan = 2;
		directoryLabel.setLayoutData(data);
		directoryText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		directoryText.setLayoutData(data);
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();		
			}			
		});
		
		Button directoryBrowseButton = new Button(repositoryGroup, SWT.PUSH);
		directoryBrowseButton.setText(Policy.bind("ExportRemoteFolderDialog.browse"));
		directoryBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
				dialog.setText(Policy.bind("ExportRemoteFolderAction.directoryDialogText"));
				String directory = dialog.open();
				if (directory != null) {
					directoryText.setText(directory);
					setOkButtonStatus();
				}
			}
		});
		
		Composite revisionGroup = new Composite(repositoryGroup, SWT.NULL);
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionLayout.marginWidth = 0;
		revisionLayout.marginHeight = 0;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.CHECK);
		headButton.setText(Policy.bind("ExportRemoteFolderDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		Label revisionLabel = new Label(revisionGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("ExportRemoteFolderDialog.revision")); //$NON-NLS-1$
		
		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		revisionText.setLayoutData(data);
		revisionText.setEnabled(false);

		logButton = new Button(revisionGroup, SWT.PUSH);
		logButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		logButton.setEnabled(false);
		logButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog();
            }
		});	
		
        if(SVNRevision.HEAD.equals(remoteResource.getRevision())) {
          headButton.setSelection(true);
        } else {
          revisionText.setText(remoteResource.getRevision().toString());
          revisionText.setEnabled(true);
          logButton.setEnabled(true);
        }
        
        revisionText.addModifyListener(new ModifyListener() {
          public void modifyText(ModifyEvent e) {
              setOkButtonStatus();
          }         
        });
        
		SelectionListener listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	revisionText.setEnabled(!headButton.getSelection());
            	logButton.setEnabled(!headButton.getSelection());
                setOkButtonStatus();
                if (!headButton.getSelection()) {
                    revisionText.selectAll();
                    revisionText.setFocus();
                }
            }
		};
        
		headButton.addSelectionListener(listener);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.EXPORT_REMOTE_FOLDER_DIALOG);	

		directoryText.setFocus();
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		revisionText.addFocusListener(focusListener);
		directoryText.addFocusListener(focusListener);
		
		return composite;
	}
    
    private void setOkButtonStatus() {
        getButton(IDialogConstants.OK_ID).setEnabled((directoryText.getText().trim().length() > 0) && (headButton.getSelection() || (revisionText.getText().trim().length() > 0)));
    }
    
	protected void showLog() {
        HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setOkButtonStatus();
    }

	protected void okPressed() {
		boolean success = true;
		SVNRevision revision = null;
		if (headButton.getSelection()) revision = SVNRevision.HEAD;
		else {
			int revisionNumber = Integer.parseInt(revisionText.getText().trim());
			long revisionLong = revisionNumber;
			revision = new SVNRevision.Number(revisionLong);
		}
		File directory = new File(directoryText.getText().trim() + File.separator + remoteResource.getName());
		try {
			new ExportRemoteFolderOperation(targetPart, remoteResource, directory, revision).run();
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy.bind("ExportRemoteFolderAction.directoryDialogText"), e.getMessage()); //$NON-NLS-1$
			success = false;
		}
		if (!success) return;
		super.okPressed();
	}

}
