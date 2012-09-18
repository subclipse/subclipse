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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.LinkList;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SetCommitPropertiesDialog extends SubclipseTrayDialog {
    
	private CommitCommentArea commitCommentArea;
    private ProjectProperties projectProperties;
    private Text issueText;
    private Text committerText;
    private String issue;
    private String author;
    private IDialogSettings settings;
    private SVNRevision revision;
    
    private Button okButton;
    private CommentProperties commentProperties;

    public SetCommitPropertiesDialog(Shell parentShell, SVNRevision revision, IResource theResource, ProjectProperties projectProperties) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);

        try {
        	if (theResource != null)
        		commentProperties = CommentProperties.getCommentProperties(theResource);
		} catch (SVNException e1) {
			// So what!
		}

		commitCommentArea = new CommitCommentArea(this, null, commentProperties);
		if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    okButton.setEnabled(commitCommentArea.getComment().trim().length() >= commentProperties.getMinimumLogMessageSize());
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener);
		}
		this.revision = revision;
		this.projectProperties = projectProperties;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
    }
    
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
	    
		getShell().setText(Policy.bind("SetCommitPropertiesDialog.revisionNumber", revision.toString()));  //$NON-NLS-1$//$NON-NLS-2$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		if (projectProperties != null) {
			if (projectProperties.getMessage() != null)
			{
				addBugtrackingArea(composite);
			}
		}

		commitCommentArea.createArea(composite);
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED)
					okPressed();
			}
		});
	    addCommitterName(composite);
	    if (author != null) committerText.setText(author);
	    
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		committerText.addFocusListener(focusListener);
		if (issueText != null) issueText.addFocusListener(focusListener);

		// set F1 help
	    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CHANGE_REVPROPS);	
		
		return composite;
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

    private void addCommitterName(Composite composite) {
		Composite committerComposite = new Composite(composite, SWT.NULL);
		GridLayout committerLayout = new GridLayout();
		committerLayout.numColumns = 2;
		committerComposite.setLayout(committerLayout);
		
		Label label = new Label(committerComposite, SWT.NONE);
		label.setText(Policy.bind("SetCommitPropertiesDialog.user"));
		committerText = new Text(committerComposite, SWT.BORDER);
		GridData data = new GridData();
		data.widthHint = 150;
		committerText.setLayoutData(data);
    }
	
    protected void okPressed() {
        saveLocation();
        if (confirmUserData() == false) {
        	return;
        }
        if (committerText.getText().trim().length() == 0) {
            MessageDialog.openError(getShell(), Policy.bind("SetCommitPropertiesDialog.title"), Policy.bind("SetCommitPropertiesDialog.noAuthor"));  //$NON-NLS-1$ //$NON-NLS-2$
        	committerText.selectAll();
        	committerText.setFocus();
            return; //$NON-NLS-1$
        }
        author = committerText.getText().trim();

        super.okPressed();
    }
    
	private boolean confirmUserData() {
		
        if (projectProperties != null)  {
        	int issueCount = 0;
        	if (projectProperties.getMessage() != null) {
        		
        		issue = issueText.getText().trim();
        		if (issue.length() > 0) {
        		    String issueError = projectProperties.validateIssue(issue);
        		    if (issueError != null) {
        		        MessageDialog.openError(getShell(), Policy.bind("SetCommitPropertiesDialog.title"), issueError); //$NON-NLS-1$
        		        issueText.selectAll();
        		        issueText.setFocus();
        		        return false;
        		    }
        		    else {
        		    	issueCount++;
        		    }
        		}
        	}
        	if (projectProperties.getLogregex() != null) {        		

        		try {
        			LinkList linkList = projectProperties.getLinkList(commitCommentArea.getComment());
        			String[] urls = linkList.getUrls();
        			issueCount += urls.length;

        		} catch (Exception e) {
        			handle(e, null, null);
        		}
        	}
    		if(projectProperties.isWarnIfNoIssue()) {

    			if (issueCount == 0) {
	    			if ((projectProperties.getMessage() != null) && (projectProperties.getLogregex() == null)) {
	        		    if (!MessageDialog.openQuestion(getShell(), Policy.bind("SetCommitPropertiesDialog.title"), Policy.bind("SetCommitPropertiesDialog.0", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
	        		        issueText.setFocus();
	        		        return false; //$NON-NLS-1$
	        		    }	
	    			}
	    			else if ((projectProperties.getMessage() == null) && (projectProperties.getLogregex() != null)) {
	    		        if (!MessageDialog.openQuestion(getShell(), Policy.bind("SetCommitPropertiesDialog.title"), Policy.bind("SetCommitPropertiesDialog.1", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
	    		        	commitCommentArea.setFocus();
	    		            return false; //$NON-NLS-1$
	    		        }	    				
	    			}
	    			else if ((projectProperties.getMessage() != null) && (projectProperties.getLogregex() != null)) {
	    		        if (!MessageDialog.openQuestion(getShell(), Policy.bind("SetCommitPropertiesDialog.title"), Policy.bind("SetCommitPropertiesDialog.2", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
	    		        	commitCommentArea.setFocus();
	    		            return false; //$NON-NLS-1$
	    		        }	    					    				
	    			}
    			}
    		}
        }
		return true;
	}

    
    
    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }

    private void saveLocation() {
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("SetCommitPropertiesDialog.location.x", x); //$NON-NLS-1$
        settings.put("SetCommitPropertiesDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("SetCommitPropertiesDialog.size.x", x); //$NON-NLS-1$
        settings.put("SetCommitPropertiesDialog.size.y", y); //$NON-NLS-1$   
    }

    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("SetCommitPropertiesDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("SetCommitPropertiesDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("SetCommitPropertiesDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("SetCommitPropertiesDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialSize();
    }	

    /**
	 * Returns the comment.
	 * @return String
	 */
	public String getComment() {
		String comment;
	    if ((projectProperties != null) && (issue != null) && (issue.length() > 0)) {
	        if (projectProperties.isAppend()) 
	            comment = commitCommentArea.getComment() + "\n" + projectProperties.getResolvedMessage(issue) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	        else
	            comment = projectProperties.getResolvedMessage(issue) + "\n" + commitCommentArea.getComment(); //$NON-NLS-1$
	    }
	    else comment = commitCommentArea.getComment();
	    commitCommentArea.addComment(commitCommentArea.getComment());
		return comment;
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

	public void setOldComment(String comment) {
		commitCommentArea.setProposedComment(comment);
	}

	public void setOldAuthor(String oldAuthor) {
		this.author = oldAuthor;
	}

	public String getAuthor() {
		return author;
	}
	
	protected void handle(Exception exception, String title, String message) {
		SVNUIPlugin.openError(getShell(), title, message, exception, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS);
	}	
}
