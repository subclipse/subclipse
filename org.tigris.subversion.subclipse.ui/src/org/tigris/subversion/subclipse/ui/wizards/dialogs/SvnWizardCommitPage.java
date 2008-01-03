package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.CompareDialog;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusUtil;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.ResourceSelectionTree;
import org.tigris.subversion.subclipse.ui.wizards.IClosableWizard;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SvnWizardCommitPage extends SvnWizardDialogPage {
	private SashForm sashForm;
	private CommitCommentArea commitCommentArea;
	private IResource[] resourcesToCommit;
//	private String url;
//	private ChangeSet changeSet;
	private ProjectProperties projectProperties;
	private Object[] selectedResources;
	private Text issueText;
	private String issue;
	private Button keepLocksButton;
	private boolean keepLocks;
	private IDialogSettings settings;
	private CommentProperties commentProperties;
	private SyncInfoSet syncInfoSet;
	private String removalError;

//	private boolean sharing;
	
	private HashMap statusMap;
	private ResourceSelectionTree resourceSelectionTree;

	public SvnWizardCommitPage(IResource[] resourcesToCommit, String url, ProjectProperties projectProperties, HashMap statusMap, ChangeSet changeSet) {
		super("CommitDialog", null); //$NON-NLS-1$		
		this.resourcesToCommit = resourcesToCommit;
//		this.url = url;
		this.projectProperties = projectProperties;
		this.statusMap = statusMap;
//		this.changeSet = changeSet;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		if (changeSet == null) {
			if (url == null) setTitle(Policy.bind("CommitDialog.commitTo") + " " + Policy.bind("CommitDialog.multiple")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			else setTitle(Policy.bind("CommitDialog.commitTo") + " " + url);  //$NON-NLS-1$//$NON-NLS-2$		
		} else {
			 setTitle(Policy.bind("CommitDialog.commitToChangeSet") + " " + changeSet.getName());  //$NON-NLS-1$//$NON-NLS-2$		
		}
		if (resourcesToCommit.length > 0) {
            try {
                commentProperties = CommentProperties.getCommentProperties(resourcesToCommit[0]);
            } catch (SVNException e) {}
		}		
		commitCommentArea = new CommitCommentArea(null, null, commentProperties);	
		commitCommentArea.setShowLabel(false);
		if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    setPageComplete(canFinish());
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener);
		}
	}

	public void createControls(Composite composite) {
	        sashForm = new SashForm(composite, SWT.VERTICAL);
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
	        
			try {
				int[] weights = new int[2];
				weights[0] = settings.getInt("CommitDialog.weights.0"); //$NON-NLS-1$
				weights[1] = settings.getInt("CommitDialog.weights.1"); //$NON-NLS-1$
				sashForm.setWeights(weights);
			} catch (Exception e) {
				sashForm.setWeights(new int[] {5, 4});			
			}
			
			if (projectProperties != null) {
			    addBugtrackingArea(cTop);
			}

			commitCommentArea.createArea(cTop);
			commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
    				if (event.getProperty() == CommitCommentArea.OK_REQUESTED && canFinish()) {
    					IClosableWizard wizard = (IClosableWizard)getWizard();
    					wizard.finishAndClose();
    				}					
				}
			});

			addResourcesArea(cBottom2);
					
			// set F1 help
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.COMMIT_DIALOG);	
		setPageComplete(canFinish());
	}
	
    private void addResourcesArea(Composite composite) {  
    	ResourceSelectionTree.IToolbarControlCreator toolbarControlCreator = new ResourceSelectionTree.IToolbarControlCreator() {
			public void createToolbarControls(Composite parent) {
				keepLocksButton = new Button(parent, SWT.CHECK);
				keepLocksButton.setText(Policy.bind("CommitDialog.keepLocks")); //$NON-NLS-1$ 	
			}
			public int getControlCount() {
				return 1;
			} 		
    	};
    	resourceSelectionTree = new ResourceSelectionTree(composite, SWT.NONE, Policy.bind("GenerateSVNDiff.Changes"), resourcesToCommit, statusMap, null, false, toolbarControlCreator, syncInfoSet); //$NON-NLS-1$    	
    	resourceSelectionTree.setRemoveFromViewValidator(new ResourceSelectionTree.IRemoveFromViewValidator() {
			public boolean canRemove(ArrayList resourceList, IStructuredSelection selection) {
				return removalOk(resourceList, selection);
			}
			public String getErrorMessage() {
				return removalError;
//				return Policy.bind("CommitDialog.unselectedPropChangeChildren"); //$NON-NLS-1$ 	
			}
    	});
    	resourceSelectionTree.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectedResources = resourceSelectionTree.getSelectedResources();
			}
		});
		resourceSelectionTree.getTreeViewer().addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				Object sel0 = sel.getFirstElement();
				if (sel0 instanceof IFile) {
					final ISVNLocalResource localResource= SVNWorkspaceRoot.getSVNResourceFor((IFile)sel0);
					try {
						new CompareDialog(getShell(),
							new SVNLocalCompareInput(localResource, SVNRevision.BASE, true)).open();
					} catch (SVNException e1) {
					}
				}
			}
		});		    	
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

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
        if (projectProperties != null) {
            issue = issueText.getText().trim();
            if (projectProperties.isWarnIfNoIssue() && (issueText.getText().trim().length() == 0)) {
                if (!MessageDialog.openQuestion(getShell(), Policy.bind("CommitDialog.title"), Policy.bind("CommitDialog.0", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
                    issueText.setFocus();
                    return false; //$NON-NLS-1$
                }
            }
            if (issueText.getText().trim().length() > 0) {
                String issueError = projectProperties.validateIssue(issueText.getText().trim());
                if (issueError != null) {
                    MessageDialog.openError(getShell(), Policy.bind("CommitDialog.title"), issueError); //$NON-NLS-1$
                    issueText.selectAll();
                    issueText.setFocus();
                    return false;
                }
            }
        }
        keepLocks = keepLocksButton.getSelection();
		return true;
	}
	
    private boolean removalOk(ArrayList resourceList, IStructuredSelection selection) {
    	ArrayList clonedList = (ArrayList)resourceList.clone();
    	List deletedFolders = new ArrayList();
    	Iterator iter = selection.iterator();
    	while (iter.hasNext()) clonedList.remove(iter.next());
    	ArrayList folderPropertyChanges = new ArrayList();
    	boolean folderDeletionSelected = false;
    	iter = clonedList.iterator();
    	while (iter.hasNext()) {
    		IResource resource = (IResource)iter.next();
    		if (resource instanceof IContainer) {
				if (ResourceWithStatusUtil.getStatus(resource).equals(Policy.bind("CommitDialog.deleted"))) { //$NON-NLS-1$
					folderDeletionSelected = true;
					deletedFolders.add(resource);
				}
				String propertyStatus = ResourceWithStatusUtil.getPropertyStatus(resource);
				if (propertyStatus != null && propertyStatus.length() > 0)
					folderPropertyChanges.add(resource);
    		}    		
    	}
    	if (folderDeletionSelected) {
    		iter = selection.iterator();
    		while (iter.hasNext()) {
    			IResource resource = (IResource)iter.next();
    			Iterator iter2 = deletedFolders.iterator();
    			while (iter2.hasNext()) {
    				IContainer deletedFolder = (IContainer)iter2.next();
    				if (isChild(resource, deletedFolder)) {
    					removalError = Policy.bind("CommitDialog.parentDeleted"); //$NON-NLS-1$ 	
    					return false;
    				}
    			}
    		}
    	}
    	if (!folderDeletionSelected || folderPropertyChanges.size() == 0) return true;
    	boolean unselectedPropChangeChildren = false;
    	iter = folderPropertyChanges.iterator();
        outer:
    	while (iter.hasNext()) {
    		IContainer container = (IContainer)iter.next();
    		for (int i = 0; i < resourcesToCommit.length; i++) {
    			if (!clonedList.contains(resourcesToCommit[i])) {
    				if (isChild(resourcesToCommit[i], container)) {
    					unselectedPropChangeChildren = true;
    					removalError = Policy.bind("CommitDialog.unselectedPropChangeChildren"); //$NON-NLS-1$ 	
    					break outer;
    				}
    			}
    		}
    	}
    	return !unselectedPropChangeChildren;
    }	
	
//    private boolean checkForUnselectedPropChangeChildren() {
//        if (selectedResources == null) return true;
//    	ArrayList folderPropertyChanges = new ArrayList();
//    	boolean folderDeletionSelected = false;
//    	for (int i = 0; i < selectedResources.length; i++) {
//    		IResource resource = (IResource)selectedResources[i];
//    		if (resource instanceof IContainer) {
//    			if (ResourceWithStatusUtil.getStatus(resource).equals(Policy.bind("CommitDialog.deleted"))) //$NON-NLS-1$
//    				folderDeletionSelected = true;
//    			String propertyStatus = ResourceWithStatusUtil.getPropertyStatus(resource);
//    			if (propertyStatus != null && propertyStatus.length() > 0)
//    				folderPropertyChanges.add(resource);
//    		}
//    	}
//    	boolean unselectedPropChangeChildren = false;
//    	if (folderDeletionSelected) {
//    		Iterator iter = folderPropertyChanges.iterator();
//    	whileLoop:
//    		while (iter.hasNext()) {
//    			IContainer container = (IContainer)iter.next();
//    			TableItem[] items = listViewer.getTable().getItems();   
//    			for (int i = 0; i < items.length; i++) {
//    				if (!items[i].getChecked()) {
//    					IResource resource = (IResource)items[i].getData();
//    					if (isChild(resource, container)) {
//    						unselectedPropChangeChildren = true;
//    						break whileLoop;
//    					}
//    				}
//    			}
//    		}
//    	}
//    	if (unselectedPropChangeChildren) {
//    		MessageDialog.openError(getShell(), Policy.bind("CommitDialog.title"), Policy.bind("CommitDialog.unselectedPropChangeChildren")); //$NON-NLS-1$
//    		return false;
//    	}
//    	return true;
//    }
    
    private boolean isChild(IResource resource, IContainer folder) {
    	IContainer container = resource.getParent();
    	while (container != null) {
    		if (container.getFullPath().toString().equals(folder.getFullPath().toString()))
    			return true;
    		container = container.getParent();
    	}
    	return false;
    }    

	public void setMessage() {
		setMessage(Policy.bind("CommitDialog.message")); //$NON-NLS-1$
	}

	private boolean canFinish() {
		if (commentProperties == null)
			return true;
		else
			return commitCommentArea.getCommentLength() >= commentProperties
					.getMinimumLogMessageSize();
	}
	
	public String getComment() {
		String comment = null;
	    if ((projectProperties != null) && (issue != null) && (issue.length() > 0)) {
	        if (projectProperties.isAppend()) 
	            comment = commitCommentArea.getComment() + "\n" + projectProperties.getResolvedMessage(issue) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	        else
	            comment = projectProperties.getResolvedMessage(issue) + "\n" + commitCommentArea.getComment(); //$NON-NLS-1$
	    }
		comment = commitCommentArea.getComment();
		commitCommentArea.addComment(comment);
		return comment;
	}
	
	public IResource[] getSelectedResources() {
		if (selectedResources == null) {
			return resourcesToCommit;
		} else {
			List result = Arrays.asList(selectedResources);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
	}	
	
    public boolean isKeepLocks() {
        return keepLocks;
    }

	public void setComment(String proposedComment) {
		commitCommentArea.setProposedComment(proposedComment);
	}

//	public void setSharing(boolean sharing) {
//		this.sharing = sharing;
//	}	
	
	public void saveSettings() {
	}
	
	public String getWindowTitle() {
		return Policy.bind("CommitDialog.title"); //$NON-NLS-1$
	}

	public void setSyncInfoSet(SyncInfoSet syncInfoSet) {
		this.syncInfoSet = syncInfoSet;
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}

}
