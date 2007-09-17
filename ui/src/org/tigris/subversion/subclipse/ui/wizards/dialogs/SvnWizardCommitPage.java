package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.AdaptableResourceList;
import org.tigris.subversion.subclipse.ui.dialogs.CompareDialog;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusLabelProvider;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusSorter;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusUtil;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.TableSetter;
import org.tigris.subversion.subclipse.ui.wizards.IClosableWizard;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SvnWizardCommitPage extends SvnWizardDialogPage {
	private static final int WIDTH_HINT = 500;
	private final static int SELECTION_HEIGHT_HINT = 100;

	private SashForm sashForm;
	private CommitCommentArea commitCommentArea;
	private IResource[] resourcesToCommit;
	private String url;
	private ProjectProperties projectProperties;
	private Object[] selectedResources;
	private CheckboxTableViewer listViewer;
	private Text issueText;
	private String issue;
	private Button keepLocksButton;
	private boolean keepLocks;
	private IDialogSettings settings;
	private TableSetter setter;
	private int sorterColumn = 1;
	private boolean sorterReversed = false;
	private CommentProperties commentProperties;

	private boolean sharing;

	public SvnWizardCommitPage(IResource[] resourcesToCommit, String url, ProjectProperties projectProperties) {
		super("CommitDialog", null); //$NON-NLS-1$		
		this.resourcesToCommit = resourcesToCommit;
		this.url = url;
		this.projectProperties = projectProperties;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		setter = new TableSetter();
		if (url == null) setTitle(Policy.bind("CommitDialog.commitTo") + " " + Policy.bind("CommitDialog.multiple")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else setTitle(Policy.bind("CommitDialog.commitTo") + " " + url);  //$NON-NLS-1$//$NON-NLS-2$		
		if (resourcesToCommit.length > 0) {
            try {
                commentProperties = CommentProperties.getCommentProperties(resourcesToCommit[0]);
            } catch (SVNException e) {}
		}		
		commitCommentArea = new CommitCommentArea(null, null, commentProperties);	
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
	        sashForm.setLayout(new GridLayout());
	        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
	                
	        Composite cTop = new Composite(sashForm, SWT.NULL);
	        cTop.setLayout(new GridLayout());
	        cTop.setLayoutData(new GridData(GridData.FILL_BOTH));
	                
	        Composite cBottom1 = new Composite(sashForm, SWT.NULL);
	        cBottom1.setLayout(new GridLayout());
	        cBottom1.setLayoutData(new GridData(GridData.FILL_BOTH));
	        
	        Composite cBottom2 = new Composite(cBottom1, SWT.NULL);
	        cBottom2.setLayout(new GridLayout());
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
	    
		// add a description label
		Label label = createWrappingLabel(composite);
		label.setText(Policy.bind("CommitDialog.resources")); //$NON-NLS-1$
		// add the selectable checkbox list
		Table table = new Table(composite, 
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | 
                SWT.MULTI | SWT.CHECK | SWT.BORDER);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		
		listViewer = new CheckboxTableViewer(table);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SELECTION_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		listViewer.getTable().setLayoutData(data);
		createColumns(table, layout);
		// set the contents of the list
		listViewer.setLabelProvider(new ResourceWithStatusLabelProvider(url));

		int sort = setter.getSorterColumn("CommitDialog"); //$NON-NLS-1$
		if (sort != -1) sorterColumn = sort;
		ResourceWithStatusSorter sorter = new ResourceWithStatusSorter(sorterColumn);
		sorter.setReversed(setter.getSorterReversed("CommitDialog")); //$NON-NLS-1$
		listViewer.setSorter(sorter);
		
		listViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return resourcesToCommit;
            }
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }	    
		});
		listViewer.setInput(new AdaptableResourceList(resourcesToCommit));
		if (selectedResources == null) {
		    setChecks();
		} else {
			listViewer.setCheckedElements(selectedResources);
		}
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectedResources = listViewer.getCheckedElements();
			}
		});
		listViewer.addDoubleClickListener(new IDoubleClickListener(){
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
		addSelectionButtons(composite);
    }
    
	private void createColumns(Table table, TableLayout layout) {
	    // sortable table
		SelectionListener headerListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// column selected - need to sort
				int column = listViewer.getTable().indexOf((TableColumn) e.widget);
				ResourceWithStatusSorter oldSorter = (ResourceWithStatusSorter) listViewer.getSorter();
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
				    oldSorter.setReversed(!oldSorter.isReversed());
				    sorterReversed = oldSorter.isReversed();
				    listViewer.refresh();
				} else {
					listViewer.setSorter(new ResourceWithStatusSorter(column));
					sorterColumn = column;
				}
			}
		};
		
		int[] widths = setter.getColumnWidths("CommitDialog", 4); //$NON-NLS-1$

		TableColumn col;
		// check
		col = new TableColumn(table, SWT.NONE);
    	col.setResizable(false);
		layout.addColumnData(new ColumnPixelData(20, false));
		col.addSelectionListener(headerListener);

		// resource
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("PendingOperationsView.resource")); //$NON-NLS-1$
		layout.addColumnData(new ColumnPixelData(widths[1], true));
		col.addSelectionListener(headerListener);

		// text status
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("CommitDialog.status")); //$NON-NLS-1$
		layout.addColumnData(new ColumnPixelData(widths[2], true));
		col.addSelectionListener(headerListener);
		
		// property status
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("CommitDialog.property")); //$NON-NLS-1$
		layout.addColumnData(new ColumnPixelData(widths[3], true));
		col.addSelectionListener(headerListener);		
	}
	
	private void setChecks() {
	    listViewer.setAllChecked(true);
	    deselect();
		selectedResources = listViewer.getCheckedElements();
	}
	
    private void deselect() {
   	 boolean selectUnadded = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT);    	
   	 TableItem[] items = listViewer.getTable().getItems();         
   	 for (int i = 0; i < items.length; i++) {
            IResource resource = (IResource)items[i].getData();
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
            try {
             if (!sharing && !selectUnadded && !svnResource.isManaged()) items[i].setChecked(false);
             else if (svnResource.getStatus().isMissing()) items[i].setChecked(false);
            } catch (SVNException e1) {}
         }    
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
	
	private void addSelectionButtons(Composite composite) {	
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		buttonComposite.setLayout(layout);
		GridData data =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		composite.setData(data);
	
		Button selectButton = new Button(buttonComposite, SWT.PUSH);
		selectButton.setText(Policy.bind("ReleaseCommentDialog.selectAll")); //$NON-NLS-1$
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
				selectedResources = null;
			}
		};
		selectButton.addSelectionListener(listener);
	
		Button deselectButton = new Button(buttonComposite, SWT.PUSH);
		deselectButton.setText(Policy.bind("ReleaseCommentDialog.deselectAll")); //$NON-NLS-1$
		listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);
				selectedResources = new Object[0];
			}
		};
		deselectButton.addSelectionListener(listener);
		
		data = new GridData();
		data.widthHint = deselectButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		selectButton.setLayoutData(data);
		
		keepLocksButton = new Button(buttonComposite, SWT.CHECK);
		keepLocksButton.setText(Policy.bind("CommitDialog.keepLocks")); //$NON-NLS-1$

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
        if (!checkForUnselectedPropChangeChildren()) return false;
		return true;
	}
	
    private boolean checkForUnselectedPropChangeChildren() {
        if (selectedResources == null) return true;
    	ArrayList folderPropertyChanges = new ArrayList();
    	boolean folderDeletionSelected = false;
    	for (int i = 0; i < selectedResources.length; i++) {
    		IResource resource = (IResource)selectedResources[i];
    		if (resource instanceof IContainer) {
    			if (ResourceWithStatusUtil.getStatus(resource).equals(Policy.bind("CommitDialog.deleted"))) //$NON-NLS-1$
    				folderDeletionSelected = true;
    			String propertyStatus = ResourceWithStatusUtil.getPropertyStatus(resource);
    			if (propertyStatus != null && propertyStatus.length() > 0)
    				folderPropertyChanges.add(resource);
    		}
    	}
    	boolean unselectedPropChangeChildren = false;
    	if (folderDeletionSelected) {
    		Iterator iter = folderPropertyChanges.iterator();
    	whileLoop:
    		while (iter.hasNext()) {
    			IContainer container = (IContainer)iter.next();
    			TableItem[] items = listViewer.getTable().getItems();   
    			for (int i = 0; i < items.length; i++) {
    				if (!items[i].getChecked()) {
    					IResource resource = (IResource)items[i].getData();
    					if (isChild(resource, container)) {
    						unselectedPropChangeChildren = true;
    						break whileLoop;
    					}
    				}
    			}
    		}
    	}
    	if (unselectedPropChangeChildren) {
    		MessageDialog.openError(getShell(), Policy.bind("CommitDialog.title"), Policy.bind("CommitDialog.unselectedPropChangeChildren")); //$NON-NLS-1$
    		return false;
    	}
    	return true;
    }
    
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
			return commitCommentArea.getComment().length() >= commentProperties
					.getMinimumLogMessageSize();
	}
	
	public String getComment() {
	    if ((projectProperties != null) && (issue != null) && (issue.length() > 0)) {
	        if (projectProperties.isAppend()) 
	            return commitCommentArea.getComment() + "\n" + projectProperties.getResolvedMessage(issue) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	        else
	            return projectProperties.getResolvedMessage(issue) + "\n" + commitCommentArea.getComment(); //$NON-NLS-1$
	    }
		return commitCommentArea.getComment();
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

	public void setSharing(boolean sharing) {
		this.sharing = sharing;
	}	
	
	public void saveSettings() {
        setter.saveColumnWidths(listViewer.getTable(), "CommitDialog"); //$NON-NLS-1$
        setter.saveSorterColumn("CommitDialog", sorterColumn); //$NON-NLS-1$  
        setter.saveSorterReversed("CommitDialog", sorterReversed); //$NON-NLS-1$
	}	

}
