/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
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
import java.net.MalformedURLException;
import java.text.ParseException;

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
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeDialog extends SvnDialog {
    
    private static final int REVISION_WIDTH_HINT = 40;
    
    private UrlCombo fromUrlCombo;
    private Button fromBrowseButton;
    private Text fromRevisionText;
    private Button fromHeadButton;

    private Button useFromUrlButton;
    private UrlCombo toUrlCombo;
    private Button toBrowseButton;
    private Text toRevisionText;
    private Button toHeadButton;
    
    private Button ignoreAncestryButton;
    private Button forceButton;
    
    private Button okButton;
    private Button diffButton;
    private Button dryRunButton;
    
    private IResource resource;
    
    private SVNUrl fromUrl;
    private SVNRevision fromRevision;
    private SVNUrl toUrl;
    private SVNRevision toRevision;
    private boolean force;
    private boolean ignoreAncestry;
    private ISVNLocalResource svnResource;
    private File diffFile;
    private File file;
    private ISVNClientAdapter svnClient;

    public MergeDialog(Shell parentShell, IResource resource) {
        super(parentShell, "MergeDialog");
        this.resource = resource;
    }
    
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("MergeDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(Policy.bind("MergeDialog.text")); //$NON-NLS-1$
		
		svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		String urlString = null;
		try {
			urlString = svnResource.getStatus().getUrlString();
        } catch (SVNException e1) {}
        
		Group fromGroup = new Group(composite, SWT.NULL);
		fromGroup.setText(Policy.bind("MergeDialog.from")); //$NON-NLS-1$
		GridLayout fromLayout = new GridLayout();
		fromLayout.numColumns = 2;
		fromGroup.setLayout(fromLayout);
		data = new GridData(GridData.FILL_BOTH);
		fromGroup.setLayoutData(data);
		
		fromUrlCombo = new UrlCombo(fromGroup, SWT.NONE);
		fromUrlCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		fromUrlCombo.init(resource.getProject().getName());
		if (urlString != null) {
		  fromUrlCombo.setText(urlString);
		}
		
		fromBrowseButton = new Button(fromGroup, SWT.PUSH);
		fromBrowseButton.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		
		Composite fromRevisionComposite = new Composite(fromGroup, SWT.NULL);
		GridLayout fromRevisionLayout = new GridLayout();
		fromRevisionLayout.numColumns = 3;
		fromRevisionComposite.setLayout(fromRevisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		fromRevisionComposite.setLayoutData(data);
		
		fromHeadButton = new Button(fromRevisionComposite, SWT.CHECK);
		fromHeadButton.setText(Policy.bind("MergeDialog.fromHead")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		fromHeadButton.setLayoutData(data);
		
		Label fromRevisionLabel = new Label(fromRevisionComposite, SWT.NONE);
		fromRevisionLabel.setText(Policy.bind("MergeDialog.revision")); //$NON-NLS-1$
		
		fromRevisionText = new Text(fromRevisionComposite, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		fromRevisionText.setLayoutData(data);
		
		Button fromLogButton = new Button(fromRevisionComposite, SWT.PUSH);
		fromLogButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		fromLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(fromRevisionText);
            }
		});
		
		SelectionListener fromListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fromRevisionText.setEnabled(!fromHeadButton.getSelection());
                setOkButtonStatus();
                if (!fromHeadButton.getSelection()) {
                    fromRevisionText.selectAll();
                    fromRevisionText.setFocus();
                }               
            }
		};
		
		fromHeadButton.addSelectionListener(fromListener);	
		
		Group toGroup = new Group(composite, SWT.NULL);
		toGroup.setText(Policy.bind("MergeDialog.to")); //$NON-NLS-1$
		GridLayout toLayout = new GridLayout();
		toLayout.numColumns = 2;
		toGroup.setLayout(toLayout);
		data = new GridData(GridData.FILL_BOTH);
		toGroup.setLayoutData(data);
		
		Composite useFromComposite = new Composite(toGroup, SWT.NULL);
		GridLayout useFromLayout = new GridLayout();
		useFromComposite.setLayout(useFromLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		useFromComposite.setLayoutData(data);
		
		useFromUrlButton = new Button(useFromComposite, SWT.CHECK);
		useFromUrlButton.setSelection(true);
		
		useFromUrlButton.setText(Policy.bind("MergeDialog.useFrom")); //$NON-NLS-1$
		
		toUrlCombo = new UrlCombo(toGroup, SWT.NONE);
		toUrlCombo.init(resource.getProject().getName());
		toUrlCombo.setText(fromUrlCombo.getText());
		toUrlCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		toUrlCombo.getCombo().setVisible(false);
		
		toBrowseButton = new Button(toGroup, SWT.PUSH);
		toBrowseButton.setText(Policy.bind("MergeDialog.browseTo")); //$NON-NLS-1$
		toBrowseButton.setVisible(false);
		
		Composite toRevisionComposite = new Composite(toGroup, SWT.NULL);
		GridLayout toRevisionLayout = new GridLayout();
		toRevisionLayout.numColumns = 3;
		toRevisionComposite.setLayout(toRevisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		toRevisionComposite.setLayoutData(data);
		
		toHeadButton = new Button(toRevisionComposite, SWT.CHECK);
		toHeadButton.setText(Policy.bind("MergeDialog.toHead")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		toHeadButton.setLayoutData(data);
		
		Label toRevisionLabel = new Label(toRevisionComposite, SWT.NONE);
		toRevisionLabel.setText(Policy.bind("MergeDialog.toRevision")); //$NON-NLS-1$
		
		toRevisionText = new Text(toRevisionComposite, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		toRevisionText.setLayoutData(data);
		
		Button toLogButton = new Button(toRevisionComposite, SWT.PUSH);
		toLogButton.setText(Policy.bind("MergeDialog.showToLog")); //$NON-NLS-1$
		toLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(toRevisionText);
            }
		});
		
		SelectionListener toListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                toRevisionText.setEnabled(!toHeadButton.getSelection());
                setOkButtonStatus();
                if (!toHeadButton.getSelection()) {
                    toRevisionText.selectAll();
                    toRevisionText.setFocus();
                }                             
            }
		};
		
		toHeadButton.addSelectionListener(toListener);
		
		SelectionListener browseListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
                if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                    if (e.getSource() == fromBrowseButton) {
                        fromUrlCombo.setText(dialog.getUrl());
                        if (useFromUrlButton.getSelection()) toUrlCombo.setText(dialog.getUrl());
                    } else toUrlCombo.setText(dialog.getUrl());
                    setOkButtonStatus();
                }               
            }
		};
		
		fromBrowseButton.addSelectionListener(browseListener);
		toBrowseButton.addSelectionListener(browseListener);
		
		useFromUrlButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (useFromUrlButton.getSelection()) toUrlCombo.setText(fromUrlCombo.getText());
                toBrowseButton.setVisible(!useFromUrlButton.getSelection());
                toUrlCombo.getCombo().setVisible(!useFromUrlButton.getSelection());
                setOkButtonStatus();
            }		    
		});
		
		fromUrlCombo.getCombo().addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (useFromUrlButton.getSelection()) toUrlCombo.setText(fromUrlCombo.getText());
            }		    
		});
		
		fromUrlCombo.getCombo().addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (useFromUrlButton.getSelection()) toUrlCombo.setText(fromUrlCombo.getText());
            }
		});
		
		ModifyListener modifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setOkButtonStatus();
            }		    
		};
		
		fromUrlCombo.getCombo().addModifyListener(modifyListener);
		fromRevisionText.addModifyListener(modifyListener);
		toUrlCombo.getCombo().addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);

		Composite ignoreComposite = new Composite(composite, SWT.NULL);
		GridLayout ignoreLayout = new GridLayout();
		ignoreLayout.numColumns = 2;
		ignoreComposite.setLayout(ignoreLayout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		ignoreComposite.setLayoutData(data);
		
		ignoreAncestryButton = new Button(ignoreComposite, SWT.CHECK);
		ignoreAncestryButton.setText(Policy.bind("MergeDialog.ignoreAncestry")); //$NON-NLS-1$
		forceButton = new Button(ignoreComposite, SWT.CHECK);
		forceButton.setText(Policy.bind("MergeDialog.force")); //$NON-NLS-1$
		
		Group workingGroup = new Group(composite, SWT.NULL);
		GridLayout workingLayout = new GridLayout();
		workingLayout.numColumns = 2;
		workingGroup.setLayout(workingLayout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		workingGroup.setLayoutData(data);
		
		Composite workingComposite = new Composite(workingGroup, SWT.NULL);
		workingComposite.setLayout(new GridLayout());
		data = new GridData(GridData.FILL_HORIZONTAL);
		workingComposite.setLayoutData(data);		
		
		Label workingLabel = new Label(workingComposite, SWT.NONE);
		workingLabel.setText(Policy.bind("MergeDialog.workingCopy")); //$NON-NLS-1$
		
		Text workingText = new Text(workingComposite, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		workingText.setLayoutData(data);
//		workingText.setText(resource.getWorkspace().getRoot().getLocation().toString());
		workingText.setText(resource.getLocation().toString());
		
		Label repositoryLabel = new Label(workingComposite, SWT.NONE);
		repositoryLabel.setText(Policy.bind("MergeDialog.repositoryUrl")); //$NON-NLS-1$	
		
		Text repositoryText = new Text(workingComposite, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		repositoryText.setLayoutData(data);	
		if (urlString != null)repositoryText.setText(urlString);
		
		Button workingLogButton = new Button(workingGroup, SWT.PUSH);
		workingLogButton.setText(Policy.bind("MergeDialog.showWorkingLog")); //$NON-NLS-1$
		workingLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(null);
            }
		});		
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		fromRevisionText.addFocusListener(focusListener);
		toRevisionText.addFocusListener(focusListener);
		
		fromUrlCombo.getCombo().setFocus();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.MERGE_DIALOG);

		return composite;
	}

    protected void createButtonsForButtonBar(Composite parent) {
        dryRunButton = createButton(parent, 2, Policy.bind("MergeDialog.dryRun"), false); //$NON-NLS-1$
        dryRunButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                dryRun();
            }
        });
        dryRunButton.setEnabled(false);
        diffButton = createButton(parent, 3, Policy.bind("MergeDialog.diff"), false); //$NON-NLS-1$
        diffButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                diff();
            }
        });   
        diffButton.setEnabled(false);
        super.createButtonsForButtonBar(parent);
    }
    
    private void dryRun() {
        try {
            svnClient = svnResource.getRepository().getSVNClient();
            file = resource.getLocation().toFile();
            fromUrl = new SVNUrl(fromUrlCombo.getText());
            toUrl = new SVNUrl(toUrlCombo.getText());
            if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
            else fromRevision = SVNRevision.getRevision(fromRevisionText.getText().trim());
            if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
            else toRevision = SVNRevision.getRevision(toRevisionText.getText().trim());                        
            BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                public void run() {
                    try {
                        svnClient.merge(fromUrl, fromRevision, toUrl, toRevision, file, forceButton.getSelection(), true, true, ignoreAncestryButton.getSelection());
                    } catch (SVNClientException e) {
                        MessageDialog.openError(getShell(), Policy.bind("MergeDialog.dryRun"), e.toString()); //$NON-NLS-1$
                    }
                }               
            });       
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.dryRun"), e.toString()); //$NON-NLS-1$            
        }
        finally {
        	svnResource.getRepository().returnSVNClient(svnClient);
        }
    }
    
    private void diff() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setText(Policy.bind("MergeDialog.diffTitle")); //$NON-NLS-1$
        String returnFile = dialog.open();
        if (returnFile == null) return;
        diffFile = new File(returnFile);
        try {
            svnClient = svnResource.getRepository().getSVNClient();
            fromUrl = new SVNUrl(fromUrlCombo.getText());
            toUrl = new SVNUrl(toUrlCombo.getText());
            if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
            else fromRevision = SVNRevision.getRevision(fromRevisionText.getText().trim());
            if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
            else toRevision = SVNRevision.getRevision(toRevisionText.getText().trim());            
            BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                public void run() {
                    try {
                        svnClient.diff(fromUrl, fromRevision, toUrl, toRevision, diffFile, true);
                    } catch (SVNClientException e) {
                        MessageDialog.openError(getShell(), Policy.bind("MergeDialog.diff"), e.toString()); //$NON-NLS-1$
                    }
                }               
            });
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.diff"), e.toString()); //$NON-NLS-1$
        }
        finally {
        	svnResource.getRepository().returnSVNClient(svnClient);
        }
    }
    
    private void showLog(Text text) {
        ISVNRemoteResource remoteResource = null;
        if (text == fromRevisionText) {
	        try {
	            fromUrl = new SVNUrl(fromUrlCombo.getText());
	            remoteResource = svnResource.getRepository().getRemoteFile(fromUrl);
	        } catch (Exception e) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
	            return;
	        }
	        if (remoteResource == null) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), Policy.bind("MergeDialog.urlError") + " " + fromUrlCombo.getText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	            return;	            
	        }
        }
        if (text == toRevisionText) {
	        try {
	            toUrl = new SVNUrl(toUrlCombo.getText());
	            remoteResource = svnResource.getRepository().getRemoteFile(toUrl);
	        } catch (Exception e) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
	            return;
	        }
	        if (remoteResource == null) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), Policy.bind("MergeDialog.urlError") + " " + toUrlCombo.getText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	            return;	            
	        }	        
        }   
        HistoryDialog dialog = null;
        if ((text == fromRevisionText) || (text == toRevisionText))
            dialog = new HistoryDialog(getShell(), remoteResource);
        else
            dialog = new HistoryDialog(getShell(), resource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        if ((text != null) && useFromUrlButton.getSelection()) {
            fromRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber() - 1));
            fromHeadButton.setSelection(false);
            toRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
            toHeadButton.setSelection(false);                  
            fromRevisionText.setEnabled(true);
            toRevisionText.setEnabled(true);
            return;
        }
        if ((text == fromRevisionText) || ((text == null) && (fromRevisionText.getText().trim().length() == 0))) {
            fromRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
            fromHeadButton.setSelection(false); 
            fromRevisionText.setEnabled(true);
        }
        if (text == toRevisionText) {
            toRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
            toHeadButton.setSelection(false);
            toRevisionText.setEnabled(true);
        } 
    }
	
    protected void okPressed() {
    	force = forceButton.getSelection();
    	ignoreAncestry = ignoreAncestryButton.getSelection();
        fromUrlCombo.saveUrl();
        if (!toUrlCombo.getText().equals(fromUrlCombo.getText())) toUrlCombo.saveUrl();
        try {
            fromUrl = new SVNUrl(fromUrlCombo.getText());
            if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
            else {
                try {
                    fromRevision = SVNRevision.getRevision(fromRevisionText.getText().trim());
                } catch (ParseException e1) {
                  MessageDialog.openError(getShell(), Policy.bind("MergeDialog.title"), Policy.bind("MergeDialog.invalidFrom")); //$NON-NLS-1$ //$NON-NLS-2$
                  return;   
                }
            }
            if (useFromUrlButton.getSelection()) toUrl = new SVNUrl(fromUrlCombo.getText());
            toUrl = new SVNUrl(toUrlCombo.getText());
            if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
            else {
                try {
                    toRevision = SVNRevision.getRevision(toRevisionText.getText().trim());
                } catch (ParseException e1) {
                  MessageDialog.openError(getShell(), Policy.bind("MergeDialog.title"), Policy.bind("MergeDialog.invalidTo")); //$NON-NLS-1$ //$NON-NLS-2$
                  return;   
                }
            }            
        } catch (MalformedURLException e) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.title"), e.getMessage()); //$NON-NLS-1$
            return;
        }
        super.okPressed();
    }
    
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
		    okButton = button;
		    okButton.setText(Policy.bind("MergeDialog.mergeButton")); //$NON-NLS-1$
		    okButton.setEnabled(false);
		}
        return button;
    }
    
    private void setOkButtonStatus() {
        boolean canFinish = true;
        if (!fromHeadButton.getSelection() && (fromRevisionText.getText().trim().length() == 0)) canFinish = false;
        else if (!toHeadButton.getSelection() && (toRevisionText.getText().trim().length() == 0)) canFinish = false;
        okButton.setEnabled(canFinish);
        diffButton.setEnabled(canFinish);
        dryRunButton.setEnabled(canFinish);
    }

    public SVNRevision getFromRevision() {
        return fromRevision;
    }
    public SVNUrl getFromUrl() {
        return fromUrl;
    }
    public SVNRevision getToRevision() {
        return toRevision;
    }
    public SVNUrl getToUrl() {
        return toUrl;
    }
	public boolean isForce() {
		return force;
	}
	public boolean isIgnoreAncestry() {
		return ignoreAncestry;
	}

}
