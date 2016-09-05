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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperationWC;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ShowDifferencesAsUnifiedDiffDialogWC extends SvnDialog {
	private IResource resource;
	private IWorkbenchPart targetPart;
	private String selectedResourceUrl;
	private Button compareButton;
	private Button diffButton;
	private Text fileText;
	private Button browseButton;
	private UrlCombo toUrlText;
	private Button toHeadButton;
	private Text toRevisionText;
	private Button toLogButton;
	private Button okButton;
	private boolean success;
	private File file;
	
	private SVNUrl toUrl;
	private SVNRevision toRevision;
	private SVNRevision pegRevision;
	private ISVNLocalResource svnResource;
	private boolean diffToOutputFile;
	
	private ShowDifferencesAsUnifiedDiffOperationWC operation;

	public ShowDifferencesAsUnifiedDiffDialogWC(Shell parentShell, IResource resource, IWorkbenchPart targetPart) {
		super(parentShell, "ShowDifferencesAsUnifiedDiffDialogWC"); //$NON-NLS-1$
		this.resource = resource;
		this.targetPart = targetPart;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.branchTag")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group fromGroup = new Group(composite, SWT.NULL);
		fromGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareFrom")); //$NON-NLS-1$
		GridLayout fromLayout = new GridLayout();
		fromLayout.numColumns = 2;
		fromGroup.setLayout(fromLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		fromGroup.setLayoutData(data);
		
		Label pathLabel = new Label(fromGroup, SWT.NONE);
		pathLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.path")); //$NON-NLS-1$
		Text pathText = new Text(fromGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		pathText.setLayoutData(data);
		pathText.setEditable(false);
		pathText.setText(resource.getFullPath().toString());
		
		Group toGroup = new Group(composite, SWT.NULL);
		toGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareTo")); //$NON-NLS-1$
		GridLayout toLayout = new GridLayout();
		toLayout.numColumns = 3;
		toGroup.setLayout(toLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		toGroup.setLayoutData(data);
		
		Label toUrlLabel = new Label(toGroup, SWT.NONE);
		toUrlLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.url")); //$NON-NLS-1$
		toUrlText = new UrlCombo(toGroup, SWT.NONE);
		toUrlText.init(resource.getProject().getName());
		toUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		
		ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		
		if (localResource != null && localResource.getUrl() != null) {
			selectedResourceUrl = localResource.getUrl().toString();
			toUrlText.setText(selectedResourceUrl);
		}
		
		Button urlBrowseButton = new Button(toGroup, SWT.PUSH);
		urlBrowseButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.browse")); //$NON-NLS-1$
		urlBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
				if (resource instanceof IContainer) dialog.setFoldersOnly(true);
				if (dialog.open() == ChooseUrlDialog.CANCEL) return;
				String url = dialog.getUrl();
				if (url != null) toUrlText.setText(url);
				setOkButtonStatus();
			}
		});
		
		Composite toRevisionGroup = new Composite(toGroup, SWT.NULL);
		GridLayout toRevisionLayout = new GridLayout();
		toRevisionLayout.numColumns = 3;
		toRevisionLayout.marginWidth = 0;
		toRevisionLayout.marginHeight = 0;
		toRevisionGroup.setLayout(toRevisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		toRevisionGroup.setLayoutData(data);
		
		toHeadButton = new Button(toRevisionGroup, SWT.CHECK);
		toHeadButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		toHeadButton.setLayoutData(data);

		toHeadButton.setSelection(true);
		
		Label revisionLabel = new Label(toRevisionGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$
		
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
		
		Group fileGroup = new Group(composite, SWT.NULL);
		fileGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareType")); //$NON-NLS-1$
		GridLayout fileLayout = new GridLayout();
		fileLayout.numColumns = 3;
		fileGroup.setLayout(fileLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		fileGroup.setLayoutData(data);
		
		compareButton = new Button(fileGroup, SWT.RADIO);
		compareButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.graphical")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		compareButton.setLayoutData(data);
		
		diffButton = new Button(fileGroup, SWT.RADIO);
		diffButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.diff")); //$NON-NLS-1$
		
		compareButton.setSelection(true);
		
		fileText = new Text(fileGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		fileText.setLayoutData(data);
		fileText.setEnabled(false);
		
		browseButton = new Button(fileGroup, SWT.PUSH);
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
		
		toUrlText.setFocus();

		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();
			}			
		};
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toRevisionText.setEnabled(!toHeadButton.getSelection());
				toLogButton.setEnabled(!toHeadButton.getSelection() && toUrlText.getText().trim().length() > 0);
				setOkButtonStatus();
				if (e.getSource() == toHeadButton && !toHeadButton.getSelection()) {
                    toRevisionText.selectAll();
                    toRevisionText.setFocus();					
				}				
			}
		};
		
		SelectionListener compareTypeListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (diffButton.getSelection()) {
					fileText.setEnabled(true);
					browseButton.setEnabled(true);
					fileText.selectAll();
					fileText.setFocus();
				} else {
					fileText.setEnabled(false);
					browseButton.setEnabled(false);					
				}
				setOkButtonStatus();
			}
		};
		
		fileText.addModifyListener(modifyListener);
		toUrlText.getCombo().addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);
		toHeadButton.addSelectionListener(selectionListener);
		compareButton.addSelectionListener(compareTypeListener);
		diffButton.addSelectionListener(compareTypeListener);
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		toRevisionText.addFocusListener(focusListener);
		fileText.addFocusListener(focusListener);
		
		return composite;
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
			if (toUrlText != null && toUrlText.getText().trim().length() == 0) {
				okButton.setEnabled(false);
			}
		}
        return button;
    }
	
    protected void okPressed() {
    	success = true;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				ISVNRepositoryLocation repository = null;
				ISVNClientAdapter svnClient = null;
				try {
					if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
					else {
						int toRevisionInt = Integer.parseInt(toRevisionText.getText().trim());
						long toRevisionLong = toRevisionInt;
						toRevision = new SVNRevision.Number(toRevisionLong);
					}
					toUrl = new SVNUrl(toUrlText.getText().trim());	
					File path = new File(resource.getLocation().toString());
					svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
					pegRevision = null;
					if (toUrlText.getText().equals(selectedResourceUrl)) {
						ISVNRemoteResource baseResource = svnResource.getBaseResource();
						if (baseResource != null) {
							pegRevision = baseResource.getLastChangedRevision();
						}
					}
					if (pegRevision == null) {
						pegRevision = toRevision;
					}
					repository = svnResource.getRepository();
					svnClient = repository.getSVNClient();
					ISVNInfo svnInfo = svnClient.getInfo(toUrl, toRevision, pegRevision);
					SVNNodeKind nodeKind = svnInfo.getNodeKind();
					if (resource instanceof IContainer) {
						if (nodeKind.toInt() == SVNNodeKind.FILE.toInt()) {
							MessageDialog.openError(getShell(), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.branchTag"), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.fileToFolder"));
							success = false;
							return;
						}						
					} else {
						if (nodeKind.toInt() == SVNNodeKind.DIR.toInt()) {
							MessageDialog.openError(getShell(), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.branchTag"), Policy.bind("ShowDifferencesAsUnifiedDiffDialog.fileToFolder"));
							success = false;
							return;
						}						
					}
					if (diffButton.getSelection()) {
						diffToOutputFile = true;
						file = new File(fileText.getText().trim());
						if (file.exists()) {
							if (!MessageDialog.openQuestion(getShell(), Policy.bind("HistoryView.showDifferences"), Policy.bind("HistoryView.overwriteOutfile", file.getName()))) return;
						}
						operation = new ShowDifferencesAsUnifiedDiffOperationWC(targetPart, path, toUrl, toRevision, file);
						operation.setGraphicalCompare(true);
					} else {
						diffToOutputFile = false;
						success = true;
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("HistoryView.showDifferences"), e.getMessage());
					success = false;
				}
				finally {
					if (repository != null) {
						repository.returnSVNClient(svnClient);
					}
				}
			}			
		});
		if (!success) return;
		toUrlText.saveUrl();
		super.okPressed();
	}
	
	private void setOkButtonStatus() {
    	boolean canFinish = true;
    	if (diffButton.getSelection() && fileText.getText().trim().length() == 0) canFinish = false;
    	if (toUrlText.getText().trim().length() == 0) canFinish = false;
    	if (!toHeadButton.getSelection() && toRevisionText.getText().trim().length() == 0) canFinish = false;
    	okButton.setEnabled(canFinish);   	
    }
	
	private void showLog(Object sourceButton) {
		try {
			SVNUrl url = new SVNUrl(toUrlText.getText().trim());
			ISVNRemoteResource remoteResource = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getRemoteFile(url);
			HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
	        if (dialog.open() == HistoryDialog.CANCEL) return;
	        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
	        if (selectedEntries.length == 0) return;
	        toRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy.bind("HistoryView.showDifferences"), e.getMessage()); //$NON-NLS-1$
		}
		setOkButtonStatus();
	}

	public ShowDifferencesAsUnifiedDiffOperationWC getOperation() {
		return operation;
	}

	public SVNUrl getToUrl() {
		return toUrl;
	}

	public SVNRevision getToRevision() {
		return toRevision;
	}

	public SVNRevision getPegRevision() {
		return pegRevision;
	}

	public ISVNLocalResource getSvnResource() {
		return svnResource;
	}

	public boolean isDiffToOutputFile() {
		return diffToOutputFile;
	}

	public File getFile() {
		return file;
	}

}
