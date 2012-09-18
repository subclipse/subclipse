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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ShowDifferencesAsUnifiedDiffDialog extends SubclipseTrayDialog {
//	private ISVNRemoteResource[] remoteResources;
	private ISVNResource[] remoteResources;
	private IWorkbenchPart targetPart;
//	private ISVNRemoteResource fromResource;
	private ISVNResource fromResource;
	private Text fileText;
	private Text fromUrlText;
	private Button fromHeadButton;
	private  Button fromRevisionButton;
	private Text fromRevisionText;
	private Button fromLogButton;
	private Text toUrlText;
	private Button toHeadButton;
	private  Button toRevisionButton;
	private Text toRevisionText;
	private Button toLogButton;
	private Button okButton;
	private boolean success;
	private String fromRevision;
	private String toRevision;

	public ShowDifferencesAsUnifiedDiffDialog(Shell parentShell, ISVNResource[] remoteResources, IWorkbenchPart targetPart) {
		super(parentShell);
		this.remoteResources = remoteResources;
		this.targetPart = targetPart;
		fromResource = remoteResources[0];
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("HistoryView.showDifferences")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite fileGroup = new Composite(composite, SWT.NULL);
		GridLayout fileLayout = new GridLayout();
		fileLayout.numColumns = 3;
		fileGroup.setLayout(fileLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		fileGroup.setLayoutData(data);
		
		Label fileLabel = new Label(fileGroup, SWT.NONE);
		fileLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.file")); //$NON-NLS-1$
		fileText = new Text(fileGroup, SWT.BORDER);
		data = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		data.widthHint = 300;
		fileText.setLayoutData(data);
		
		Button browseButton = new Button(fileGroup, SWT.PUSH);
		browseButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.fileBrowse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
				dialog.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.fileDialogText")); //$NON-NLS-1$
				dialog.setFileName("revision.diff"); //$NON-NLS-1$
				String outFile = dialog.open();
				if (outFile != null) fileText.setText(outFile);
			}		
		});
		
		Group fromGroup = new Group(composite, SWT.NULL);
		fromGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareFrom")); //$NON-NLS-1$
		GridLayout fromLayout = new GridLayout();
		fromLayout.numColumns = 2;
		fromGroup.setLayout(fromLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		fromGroup.setLayoutData(data);
		
		Label fromUrlLabel = new Label(fromGroup, SWT.NONE);
		fromUrlLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.url")); //$NON-NLS-1$
		fromUrlText = new Text(fromGroup, SWT.BORDER);
		fromUrlText.setEditable(false);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 300;
		fromUrlText.setLayoutData(data);
		fromUrlText.setText(remoteResources[0].getUrl().toString());
		
		Group fromRevisionGroup = new Group(fromGroup, SWT.NULL);
		fromRevisionGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$
		GridLayout fromRevisionLayout = new GridLayout();
		fromRevisionLayout.numColumns = 3;
		fromRevisionGroup.setLayout(fromRevisionLayout);
		data = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
		fromRevisionGroup.setLayoutData(data);
		
		fromHeadButton = new Button(fromRevisionGroup, SWT.RADIO);
		fromHeadButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		fromHeadButton.setLayoutData(data);
		
		fromRevisionButton = new Button(fromRevisionGroup, SWT.RADIO);
		fromRevisionButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$
		
		fromHeadButton.setSelection(true);
		
		fromRevisionText = new Text(fromRevisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		fromRevisionText.setLayoutData(data);
		fromRevisionText.setEnabled(false);
		
		fromLogButton = new Button(fromRevisionGroup, SWT.PUSH);
		fromLogButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.showLog")); //$NON-NLS-1$
		fromLogButton.setEnabled(false);
		fromLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
		});	
		
		Group toGroup = new Group(composite, SWT.NULL);
		toGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareTo")); //$NON-NLS-1$
		GridLayout toLayout = new GridLayout();
		toLayout.numColumns = 2;
		toGroup.setLayout(toLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		toGroup.setLayoutData(data);
		
		Label toUrlLabel = new Label(toGroup, SWT.NONE);
		toUrlLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.url")); //$NON-NLS-1$
		toUrlText = new Text(toGroup, SWT.BORDER);
		toUrlText.setEditable(false);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 300;
		toUrlText.setLayoutData(data);
		toUrlText.setText(remoteResources[1].getUrl().toString());
		
		Group toRevisionGroup = new Group(toGroup, SWT.NULL);
		toRevisionGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$
		GridLayout toRevisionLayout = new GridLayout();
		toRevisionLayout.numColumns = 3;
		toRevisionGroup.setLayout(toRevisionLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		toRevisionGroup.setLayoutData(data);
		
		toHeadButton = new Button(toRevisionGroup, SWT.RADIO);
		toHeadButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		toHeadButton.setLayoutData(data);
		
		toRevisionButton = new Button(toRevisionGroup, SWT.RADIO);
		toRevisionButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$
		
		toHeadButton.setSelection(true);
		
		toRevisionText = new Text(toRevisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		toRevisionText.setLayoutData(data);
		toRevisionText.setEnabled(false);
		
		toLogButton = new Button(toRevisionGroup, SWT.PUSH);
		toLogButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.showLog")); //$NON-NLS-1$
		toLogButton.setEnabled(false);
		toLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
		});	
		
		if (fromRevision != null) {
			fromRevisionText.setText(fromRevision);
			fromRevisionText.setEnabled(true);
			fromLogButton.setEnabled(true);
			fromRevisionButton.setSelection(true);
			fromHeadButton.setSelection(false);
		}
		if (toRevision != null) {
			toRevisionText.setText(toRevision);
			toRevisionText.setEnabled(true);
			toLogButton.setEnabled(true);
			toRevisionButton.setSelection(true);
			toHeadButton.setSelection(false);
		}				
		
		fileText.setFocus();
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();
			}			
		};
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                fromRevisionText.setEnabled(fromRevisionButton.getSelection());
                fromLogButton.setEnabled(fromRevisionButton.getSelection());
                toRevisionText.setEnabled(toRevisionButton.getSelection());
                toLogButton.setEnabled(toRevisionButton.getSelection());
				setOkButtonStatus();
				if (e.getSource() == fromRevisionButton && fromRevisionButton.getSelection()) {
                    fromRevisionText.selectAll();
                    fromRevisionText.setFocus();					
				}
				if (e.getSource() == toRevisionButton && toRevisionButton.getSelection()) {
                    toRevisionText.selectAll();
                    toRevisionText.setFocus();					
				}				
			}
		};
		
		fileText.addModifyListener(modifyListener);
		fromRevisionText.addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);
		fromHeadButton.addSelectionListener(selectionListener);
		fromRevisionButton.addSelectionListener(selectionListener);
		toHeadButton.addSelectionListener(selectionListener);
		toRevisionButton.addSelectionListener(selectionListener);
		
		// Set F1 Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHOW_UNIFIED_DIFF_DIALOG);	
		
		return composite;
	}
	
    protected void createButtonsForButtonBar(Composite parent) {
		Button toggleFromToButton = createButton(parent, 2, Policy.bind("ShowDifferencesAsUnifiedDiffDialog.swap"), false); //$NON-NLS-1$
		toggleFromToButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fromUrl = fromUrlText.getText().trim();
				boolean fromHeadRevision = fromHeadButton.getSelection();
				String fromRevision = fromRevisionText.getText().trim();
				String toUrl = toUrlText.getText().trim();
				boolean toHeadRevision = toHeadButton.getSelection();
				String toRevision = toRevisionText.getText().trim();	
				fromUrlText.setText(toUrl);
				toUrlText.setText(fromUrl);
				if (toHeadRevision) {
					fromHeadButton.setSelection(true);
					fromRevisionButton.setSelection(false);
				}
				else {
					fromHeadButton.setSelection(false);
					fromRevisionButton.setSelection(true);
				}
				if (fromHeadRevision) {
					toHeadButton.setSelection(true);
					toRevisionButton.setSelection(false);
				}
				else {
					toHeadButton.setSelection(false);
					toRevisionButton.setSelection(true);
				}
				fromRevisionText.setText(toRevision);
				toRevisionText.setText(fromRevision);
				if (fromResource == remoteResources[0]) fromResource = remoteResources[1];
				else fromResource = remoteResources[0];
				fromRevisionText.setEnabled(fromRevisionButton.getSelection());
				toRevisionText.setEnabled(toRevisionButton.getSelection());
				setOkButtonStatus();
			}			
		});
		super.createButtonsForButtonBar(parent);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
			okButton.setEnabled(false);
		}
        return button;
    }	
    
    protected void okPressed() {
    	success = true;
		final File file = new File(fileText.getText().trim());
		if (file.exists()) {
			if (!MessageDialog.openQuestion(getShell(), Policy.bind("HistoryView.showDifferences"), Policy.bind("HistoryView.overwriteOutfile", file.getName()))) return;
		}	
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					SVNUrl fromUrl = null;
					SVNUrl toUrl = null;
					SVNRevision fromRevision;
					SVNRevision toRevision;
					if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
					else {
						int fromRevisionInt = Integer.parseInt(fromRevisionText.getText().trim());
						long fromRevisionLong = fromRevisionInt;
						fromRevision = new SVNRevision.Number(fromRevisionLong);
					}
					if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
					else {
						int toRevisionInt = Integer.parseInt(toRevisionText.getText().trim());
						long toRevisionLong = toRevisionInt;
						toRevision = new SVNRevision.Number(toRevisionLong);
					}
					if (fromResource == remoteResources[0]) {
						fromUrl = remoteResources[0].getUrl();
						toUrl = remoteResources[1].getUrl();						
					} else {
						fromUrl = remoteResources[1].getUrl();
						toUrl = remoteResources[0].getUrl();								
					}
					new ShowDifferencesAsUnifiedDiffOperation(targetPart, fromUrl, fromRevision, toUrl, toRevision, file).run();
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("HistoryView.showDifferences"), e.getMessage());
					success = false;
				}
			}			
		});
		if (!success) return;
		super.okPressed();
	}

	private void setOkButtonStatus() {
    	boolean canFinish = true;
    	if (fileText.getText().trim().length() == 0) canFinish = false;
    	if (fromRevisionButton.getSelection() && fromRevisionText.getText().trim().length() == 0) canFinish = false;
    	if (toRevisionButton.getSelection() && toRevisionText.getText().trim().length() == 0) canFinish = false;
    	okButton.setEnabled(canFinish);
    	
    }
    
    private void showLog(Object sourceButton) {
    	HistoryDialog dialog = null;
    	if (sourceButton == fromLogButton) {
    		if (fromResource instanceof ISVNRemoteResource) {
		    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[0]);
		    	else dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[1]);
    		} else {
    	    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), ((ISVNResource)remoteResources[0]).getResource());
		    	else dialog = new HistoryDialog(getShell(), ((ISVNResource)remoteResources[1]).getResource());    			
    		}
	    	if (dialog.open() == HistoryDialog.CANCEL) return;
	        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
	        if (selectedEntries.length == 0) return;
	        fromRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
    	} else {
    		if (fromResource instanceof ISVNRemoteResource) {
		    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[1]);
		    	else dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[0]);
    		} else {
		    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), ((ISVNResource)remoteResources[1]).getResource());
		    	else dialog = new HistoryDialog(getShell(), ((ISVNRemoteResource)remoteResources[0]).getResource());    			
    		}
	    	if (dialog.open() == HistoryDialog.CANCEL) return;
	        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
	        if (selectedEntries.length == 0) return;
	        toRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));    		
    	}
        setOkButtonStatus();    	
    }

	public void setFromRevision(String fromRevision) {
		this.fromRevision = fromRevision;
	}

	public void setToRevision(String toRevision) {
		this.toRevision = toRevision;
	}

}
