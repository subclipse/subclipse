package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;

public class BranchTagWizardCommentPage extends SVNWizardPage {
	protected CommitCommentArea commitCommentArea;
    private Text issueText;
    private CommentProperties commentProperties;
    private ProjectProperties projectProperties;
    protected Button switchAfterBranchTagCheckBox;
    private IResource resource;
    private boolean visited = false;

	public BranchTagWizardCommentPage() {
		super("commentPage", //$NON-NLS-1$
				Policy.bind("BranchTagWizardCommentPage.heading"), //$NON-NLS-1$
				SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN),
				Policy.bind("BranchTagWizardCommentPage.message")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		resource = ((BranchTagWizard)getWizard()).getResource();
		if (resource == null) {
			commitCommentArea = new CommitCommentArea(null, null, Policy.bind("BranchTagDialog.enterComment"), commentProperties); //$NON-NLS-1$
		} else {
	        try {
	            commentProperties = CommentProperties.getCommentProperties(resource);
	            projectProperties = ProjectProperties.getProjectProperties(resource);
	        } catch (SVNException e) {}
	        commitCommentArea = new CommitCommentArea(null, null, Policy.bind("BranchTagDialog.enterComment"), commentProperties); //$NON-NLS-1$
	        if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
			    ModifyListener modifyListener = new ModifyListener() {
	                public void modifyText(ModifyEvent e) {
	                	setPageComplete(canFinish());   
	                }		        
			    };
			    commitCommentArea.setModifyListener(modifyListener); 
			}			
		}		
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerLayout.marginHeight = 0;
		outerLayout.marginWidth = 0;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		if (projectProperties != null) {
			if (projectProperties.getMessage() != null)
			{
				addBugtrackingArea(outerContainer);
			}
		}
		
		commitCommentArea.createArea(outerContainer);
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED && canFinish()) {
					IClosableWizard wizard = (IClosableWizard)getWizard();
					wizard.finishAndClose();
				}	
			}
		});	
		
		if (resource != null) {
			switchAfterBranchTagCheckBox = new Button(outerContainer, SWT.CHECK);
			switchAfterBranchTagCheckBox.setText(Policy.bind("BranchTagDialog.switchAfterTagBranch"));
		}

		setPageComplete(canFinish());
		
		if (issueText != null) {
			FocusListener focusListener = new FocusListener() {
				public void focusGained(FocusEvent e) {
					((Text)e.getSource()).selectAll();
				}
				public void focusLost(FocusEvent e) {
					((Text)e.getSource()).setText(((Text)e.getSource()).getText());
				}		
			};
			issueText.addFocusListener(focusListener);
		}
		
		setControl(outerContainer);
	}
	
    public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) visited = true;
		setPageComplete(canFinish());
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
	
	private boolean canFinish() {
		if (!visited) return false;
        if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
            if (commitCommentArea.getCommentLength() < commentProperties.getMinimumLogMessageSize())
            	return false;
        }
        return true;
	}

	public ProjectProperties getProjectProperties() {
		return projectProperties;
	}
	
	public String getIssue() {
		if (issueText == null) return null;
		return issueText.getText().trim();
	}
	
    public String getComment() {
    	String comment = null;
	    if ((projectProperties != null) && (getIssue() != null) && (getIssue().length() > 0)) {
	        if (projectProperties.isAppend()) 
	            comment = commitCommentArea.getComment() + "\n" + projectProperties.getResolvedMessage(getIssue()) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	        else
	            comment = projectProperties.getResolvedMessage(getIssue()) + "\n" + commitCommentArea.getComment(); //$NON-NLS-1$
	    }        
	    else comment = commitCommentArea.getComment();
	    commitCommentArea.addComment(commitCommentArea.getComment());
	    return comment;
    }	

}
