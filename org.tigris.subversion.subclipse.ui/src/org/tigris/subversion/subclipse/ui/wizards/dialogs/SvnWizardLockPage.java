package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.util.ResourceSelectionTree;
import org.tigris.subversion.subclipse.ui.wizards.IClosableWizard;

public class SvnWizardLockPage extends SvnWizardDialogPage {
    private CommitCommentArea commitCommentArea;
    private Button stealButton;
    private String comment;
    private boolean stealLock;
    private IResource[] files;
    private CommentProperties commentProperties;
    private ResourceSelectionTree resourceSelectionTree;

	public SvnWizardLockPage(IResource[] files) {
		super("LockDialog", Policy.bind("LockDialog.title")); //$NON-NLS-1$	/$NON-NLS-2$	  
		this.files = files;
		if (files.length > 0) {
            try {
                commentProperties = CommentProperties.getCommentProperties(files[0]);
                commentProperties.setMinimumLogMessageSize(commentProperties.getMinimumLockMessageSize());
            } catch (SVNException e) {}
            if (commentProperties != null) {
                commentProperties.setLogTemplate(null);
            }
		}		
        commitCommentArea = new CommitCommentArea(null, null, commentProperties); //$NON-NLS-1$
        commitCommentArea.setShowLabel(false);		
        if ((commentProperties != null) && (commentProperties.getMinimumLockMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    setPageComplete(canFinish());                
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener);
		}   
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED && canFinish()) {
					IClosableWizard wizard = (IClosableWizard)getWizard();
					wizard.finishAndClose();
				}					
			}
		});
	}

	public void createControls(Composite composite) {
        SashForm sashForm = new SashForm(composite, SWT.VERTICAL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        sashForm.setLayout(gridLayout);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
                
        Composite cTop = new Composite(sashForm, SWT.NULL);
        GridLayout topLayout = new GridLayout();
        topLayout.marginHeight = 0;
        topLayout.marginWidth = 0;
        cTop.setLayout(topLayout);
        cTop.setLayoutData(new GridData(GridData.FILL_BOTH));
                
        Composite cBottom1 = new Composite(sashForm, SWT.NULL);
        GridLayout bottom1Layout = new GridLayout();
        bottom1Layout.marginHeight = 0;
        bottom1Layout.marginWidth = 0;
        cBottom1.setLayout(bottom1Layout);
        cBottom1.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Composite cBottom2 = new Composite(cBottom1, SWT.NULL);
        GridLayout bottom2Layout = new GridLayout();
        bottom2Layout.marginHeight = 0;
        bottom2Layout.marginWidth = 0;	        
        cBottom2.setLayout(bottom2Layout);
        cBottom2.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		commitCommentArea.createArea(cTop);
        
        addResourcesArea(cBottom2);
        
        setPageComplete(canFinish());

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LOCK_DIALOG);	
	}
	
    private void addResourcesArea(Composite composite) {  
    	ResourceSelectionTree.IToolbarControlCreator toolbarControlCreator = new ResourceSelectionTree.IToolbarControlCreator() {
      public void createToolbarControls(ToolBarManager toolbarManager) {
        toolbarManager.add(new ControlContribution("stealLock") {
          protected Control createControl(Composite parent) {
            stealButton = new Button(parent, SWT.CHECK);
            stealButton.setText(Policy.bind("LockDialog.stealLock")); //$NON-NLS-1$		
            return stealButton;
          }
        });
      }
      public int getControlCount() {
        return 1;
      }
    };
    	resourceSelectionTree = new ResourceSelectionTree(composite, SWT.NONE, "These files will be locked:", files, new HashMap(), null, false, toolbarControlCreator, null); //$NON-NLS-1$    	
    	resourceSelectionTree.setShowRemoveFromViewAction(false);
    }	

	public String getWindowTitle() {
		return Policy.bind("LockDialog.title"); //$NON-NLS-1$	 
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
        stealLock = stealButton.getSelection();
        comment = commitCommentArea.getComment(true);
        return true;
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("LockDialog.message")); //$NON-NLS-1$	 
	}
	
    public String getComment() {
        return comment;
    }
    
    public boolean isStealLock() {
        return stealLock;
    }
    
	private boolean canFinish() {
		if (commentProperties == null)
			return true;
		else
			return commitCommentArea.getCommentLength() >= commentProperties
					.getMinimumLogMessageSize();
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}    

}
