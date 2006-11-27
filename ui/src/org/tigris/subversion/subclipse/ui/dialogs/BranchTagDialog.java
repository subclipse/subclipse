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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagDialog extends TrayDialog {
    
    private static final int URL_WIDTH_HINT = 450;
    private static final int REVISION_WIDTH_HINT = 40;
    
    private IResource resource;
    private ISVNRemoteResource remoteResource;
    private ISVNLocalResource svnResource;
    
    private UrlCombo toUrlCombo;
    private Button serverButton;
    private Button revisionButton;
    private Text revisionText;
    private Button logButton;
    private Button workingCopyButton;
    private CommitCommentArea commitCommentArea;
    
    private SVNRevision revision;
    private SVNUrl url;
    private SVNUrl toUrl;
    private boolean createOnServer;
    private boolean specificRevision;
    private String comment;
    private Text issueText;
    private String issue;
 
    private Button okButton;
    private CommentProperties commentProperties;
    private ProjectProperties projectProperties;
    
    private long revisionNumber = 0;
    private Alias newAlias;

    private Button switchAfterBranchTagCheckBox;
    private boolean switchAfterBranchTag;
    
    public void setRevisionNumber(long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	public BranchTagDialog(Shell parentShell, IResource resource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        try {
            commentProperties = CommentProperties.getCommentProperties(resource);
            projectProperties = ProjectProperties.getProjectProperties(resource);
        } catch (SVNException e) {}
        commitCommentArea = new CommitCommentArea(this, null, Policy.bind("BranchTagDialog.enterComment"), commentProperties); //$NON-NLS-1$
		if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    setOkButtonStatus();
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener); 
		}
        this.resource = resource;
    }
	
	public BranchTagDialog(Shell parentShell, ISVNRemoteResource remoteResource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        commitCommentArea = new CommitCommentArea(this, null, Policy.bind("BranchTagDialog.enterComment"), commentProperties); //$NON-NLS-1$
        this.remoteResource = remoteResource;
    }
    
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("BranchTagDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Composite top = new Composite(composite, SWT.NULL);
        top.setLayout(new GridLayout());
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group repositoryGroup = new Group(top, SWT.NULL);
		repositoryGroup.setText(Policy.bind("BranchTagDialog.repository")); //$NON-NLS-1$
		repositoryGroup.setLayout(new GridLayout());
		GridData data = new GridData(GridData.FILL_BOTH);
		repositoryGroup.setLayoutData(data);
		
		Label fromUrlLabel = new Label(repositoryGroup, SWT.NONE);
		if (resource == null) fromUrlLabel.setText(Policy.bind("BranchTagDialog.fromUrl")); //$NON-NLS-1$
		else fromUrlLabel.setText(Policy.bind("BranchTagDialog.url")); //$NON-NLS-1$
		
		Text urlText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = URL_WIDTH_HINT;
		urlText.setLayoutData(data);
		
		if (resource == null) {
			url = remoteResource.getUrl();
			urlText.setText(url.toString());
		} else {
			svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			try {
	            url = svnResource.getStatus().getUrl();
	            if (url != null) urlText.setText(svnResource.getStatus().getUrlString());
	        } catch (SVNException e1) {}
		}
		
        urlText.setEditable(false);
        
		Label toUrlLabel = new Label(repositoryGroup, SWT.NONE);
		toUrlLabel.setText(Policy.bind("BranchTagDialog.toUrl")); //$NON-NLS-1$   
		
		Composite urlComposite = new Composite(repositoryGroup, SWT.NULL);
		GridLayout urlLayout = new GridLayout();
		urlLayout.numColumns = 2;
		urlLayout.marginWidth = 0;
		urlLayout.marginHeight = 0;
		urlComposite.setLayout(urlLayout);
		data = new GridData(GridData.FILL_BOTH);
		urlComposite.setLayoutData(data);
		
		if (resource == null) toUrlCombo = new UrlCombo(urlComposite, "repositoryBrowser"); //$NON-NLS-1$
		else toUrlCombo = new UrlCombo(urlComposite, resource.getProject().getName());
		toUrlCombo.setText(urlText.getText());
		
		Button browseButton = new Button(urlComposite, SWT.PUSH);
		browseButton.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
                if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                    toUrlCombo.setText(dialog.getUrl());
                }
            }
		});	
		
		Group serverComposite = new Group(repositoryGroup, SWT.NULL);
		serverComposite.setText(Policy.bind("BranchTagDialog.createCopy")); //$NON-NLS-1$
		GridLayout serverLayout = new GridLayout();
		serverLayout.numColumns = 3;
		serverComposite.setLayout(serverLayout);
		data = new GridData(GridData.FILL_BOTH);
		serverComposite.setLayoutData(data);	
		
		serverButton = new Button(serverComposite, SWT.RADIO);
		serverButton.setText(Policy.bind("BranchTagDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		serverButton.setLayoutData(data);
		
		revisionButton = new Button(serverComposite, SWT.RADIO);
		revisionButton.setText(Policy.bind("BranchTagDialog.revision")); //$NON-NLS-1$
		
		revisionText = new Text(serverComposite, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		if (revisionNumber == 0) revisionText.setEnabled(false);
		else revisionText.setText("" + revisionNumber);
		revisionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setOkButtonStatus();
            }		   
		});
		logButton = new Button(serverComposite, SWT.PUSH);
		logButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		if (revisionNumber == 0)
			logButton.setEnabled(false);
		logButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog();
            }
		});		
		
		workingCopyButton = new Button(serverComposite, SWT.RADIO);
		workingCopyButton.setText(Policy.bind("BranchTagDialog.working")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		workingCopyButton.setLayoutData(data);	
		if (resource == null) workingCopyButton.setVisible(false);
		
		if (revisionNumber == 0) serverButton.setSelection(true);
		else revisionButton.setSelection(true);
		
		SelectionListener selectionListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                revisionText.setEnabled(revisionButton.getSelection());
                logButton.setEnabled(revisionButton.getSelection());
                if (revisionButton.getSelection()) revisionText.setFocus();               
                setOkButtonStatus();
            }
		};
		
		serverButton.addSelectionListener(selectionListener);
		revisionButton.addSelectionListener(selectionListener);
		workingCopyButton.addSelectionListener(selectionListener);
		

		if (projectProperties != null) {
		    addBugtrackingArea(top);
		}
		
		commitCommentArea.createArea(composite);
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED)
					okPressed();
			}
		});
		
		toUrlCombo.getCombo().setFocus();

		if (resource != null) {
			switchAfterBranchTagCheckBox = new Button(composite, SWT.CHECK);
			switchAfterBranchTagCheckBox.setText(Policy.bind("BranchTagDialog.switchAfterTagBranch"));
		}
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.BRANCH_TAG_DIALOG);
		
		return composite;
	}
	
	protected void showLog() {
	    ISVNRemoteResource remoteResource = null;
	    if (resource == null) remoteResource = this.remoteResource;
	    else {
	        try {
	            remoteResource = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getRemoteFile(url);
	        } catch (Exception e) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
	            return;
	        }
	    }
        if (remoteResource == null) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), Policy.bind("MergeDialog.urlError") + " " + toUrlCombo.getText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return;	            
        }	
        HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setOkButtonStatus();
    }

    private void addBugtrackingArea(Composite composite) {
		Composite bugtrackingComposite = new Composite(composite, SWT.NULL);
		GridLayout bugtrackingLayout = new GridLayout();
		bugtrackingLayout.numColumns = 2;
		bugtrackingComposite.setLayout(bugtrackingLayout);
		
		Label label = new Label(bugtrackingComposite, SWT.NONE);
		label.setText(projectProperties.getLabel());
		issueText = new Text(bugtrackingComposite, SWT.BORDER);
		GridData data = new GridData();
		data.widthHint = 150;
		issueText.setLayoutData(data);
    }	
	
    protected void okPressed() {
        if (projectProperties != null) {
            issue = issueText.getText().trim();
            if (projectProperties.isWarnIfNoIssue() && (issueText.getText().trim().length() == 0)) {
                if (!MessageDialog.openQuestion(getShell(), Policy.bind("BranchTagDialog.title"), Policy.bind("BranchTagDialog.0", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
                    issueText.setFocus();
                    return;
                }
            }
            if (issueText.getText().trim().length() > 0) {
                String issueError = projectProperties.validateIssue(issueText.getText().trim());
                if (issueError != null) {
                    MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), issueError); //$NON-NLS-1$
                    issueText.selectAll();
                    issueText.setFocus();
                    return;
                }
            }
        }        
        toUrlCombo.saveUrl();
        createOnServer = !workingCopyButton.getSelection();
        specificRevision = revisionButton.getSelection();
        if(switchAfterBranchTagCheckBox != null) {
        	switchAfterBranchTag = switchAfterBranchTagCheckBox.getSelection();
        }
        
        comment = commitCommentArea.getComment();
        if (serverButton.getSelection()) revision = SVNRevision.HEAD;
        try {
            toUrl = new SVNUrl(toUrlCombo.getText());
            if (revisionButton.getSelection()) revision = SVNRevision.getRevision(revisionText.getText().trim());
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), e.getMessage()); //$NON-NLS-1$
            return;
        }
        if (resource != null) updateTagsProperty(toUrl);
        super.okPressed();
    }
    
    private void updateTagsProperty(SVNUrl toUrl) {
    	try {
			ISVNProperty property = null;
			property = svnResource.getSvnProperty("subclipse:tags"); //$NON-NLS-1$
			if (property == null) return;
			newAlias = new Alias();
			newAlias.setBranch(toUrl.toString().toUpperCase().indexOf("TAGS") == -1); //$NON-NLS-1$
			String relativePath = toUrl.toString().substring(svnResource.getRepository().getUrl().toString().length());
			newAlias.setRelativePath(relativePath);
			SVNRevision revision = null;
			if (revisionButton.getSelection()) revision = SVNRevision.getRevision(revisionText.getText().trim());
			else {
				ISVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
				ISVNInfo svnInfo = svnClient.getInfo(url);
				revision = SVNRevision.getRevision(svnInfo.getRevision().toString());
			}
			newAlias.setRevision(Integer.parseInt(revision.toString()));
			newAlias.setName(toUrl.getLastPathSegment());
			BranchTagPropertyUpdateDialog dialog = new BranchTagPropertyUpdateDialog(getShell(), resource, newAlias);
			if (dialog.open() == BranchTagPropertyUpdateDialog.OK) 
				newAlias = dialog.getNewAlias();
			else
				newAlias = null;
    	} catch (Exception e) {}
    }
    
    private void setOkButtonStatus() {
        if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
            if (commitCommentArea.getComment().trim().length() < commentProperties.getMinimumLogMessageSize()) {
                okButton.setEnabled(false);
                return;
            }
        }
        if (revisionButton.getSelection() && (revisionText.getText().trim().length() == 0)) {
            okButton.setEnabled(false);
            return;
        }
        okButton.setEnabled(true);
    }
    
	protected Button createButton(
			Composite parent,
			int id,
			String label,
			boolean defaultButton) {
			Button button = super.createButton(parent, id, label, defaultButton);
			if (id == IDialogConstants.OK_ID) {
				okButton = button;
				if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
					okButton.setEnabled(false);
				}
			}
			return button;
		}	   
    
	protected static final int LABEL_WIDTH_HINT = 400;
	protected Label createWrappingLabel(Composite parent) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}

    public String getComment() {
	    if ((projectProperties != null) && (issue != null) && (issue.length() > 0)) {
	        if (projectProperties.isAppend()) 
	            return comment + "\n" + projectProperties.getResolvedMessage(issue) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	        else
	            return projectProperties.getResolvedMessage(issue) + "\n" + comment; //$NON-NLS-1$
	    }        
        return comment;
    }
    public boolean isCreateOnServer() {
        return createOnServer;
    }
    public boolean isSpecificRevision() {
        return specificRevision;
    }   
    public SVNUrl getToUrl() {
        return toUrl;
    }
    public SVNUrl getUrl() {
        return url;
    }

    public SVNRevision getRevision() {
        return revision;
    }

	public Alias getNewAlias() {
		return newAlias;
	}

	public boolean switchAfterTagBranch() {
		return switchAfterBranchTag;
	}
}
