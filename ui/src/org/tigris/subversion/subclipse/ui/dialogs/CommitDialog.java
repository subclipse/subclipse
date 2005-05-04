package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.TableSetter;

public class CommitDialog extends Dialog {
    
	private static final int WIDTH_HINT = 500;
	private final static int SELECTION_HEIGHT_HINT = 100;
    
    private CommitCommentArea commitCommentArea;
    private IResource[] resourcesToCommit;
    private String url;
    private boolean unaddedResources;
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
    
    private Button okButton;
    private CommentProperties commentProperties;

    public CommitDialog(Shell parentShell, IResource[] resourcesToCommit, String url, boolean unaddedResources, ProjectProperties projectProperties) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
		if (resourcesToCommit.length > 0) {
            try {
                commentProperties = CommentProperties.getCommentProperties(resourcesToCommit[0]);
            } catch (SVNException e) {}
		}
		commitCommentArea = new CommitCommentArea(this, null, commentProperties);
		if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    okButton.setEnabled(commitCommentArea.getText().getText().trim().length() >= commentProperties.getMinimumLogMessageSize());
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener);
		}
		this.resourcesToCommit = resourcesToCommit;
		this.url = url;
		this.unaddedResources = unaddedResources;
		this.projectProperties = projectProperties;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		setter = new TableSetter();
    }
    
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
	    
		if (url == null) getShell().setText(Policy.bind("CommitDialog.commitTo") + " " + Policy.bind("CommitDialog.multiple")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else getShell().setText(Policy.bind("CommitDialog.commitTo") + " " + url);  //$NON-NLS-1$//$NON-NLS-2$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		if (projectProperties != null) {
		    addBugtrackingArea(composite);
		}

		commitCommentArea.createArea(composite);
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED)
					okPressed();
			}
		});

		addResourcesArea(composite);
				
		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.RELEASE_COMMENT_DIALOG);	
		
		return composite;
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
		
		addSelectionButtons(composite);
		
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
        saveLocation();
        if (projectProperties != null) {
            issue = issueText.getText().trim();
            if (projectProperties.isWarnIfNoIssue() && (issueText.getText().trim().length() == 0)) {
                if (!MessageDialog.openQuestion(getShell(), Policy.bind("CommitDialog.title"), Policy.bind("CommitDialog.0", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
                    issueText.setFocus();
                    return; //$NON-NLS-1$
                }
            }
            if (issueText.getText().trim().length() > 0) {
                String issueError = projectProperties.validateIssue(issueText.getText().trim());
                if (issueError != null) {
                    MessageDialog.openError(getShell(), Policy.bind("CommitDialog.title"), issueError); //$NON-NLS-1$
                    issueText.selectAll();
                    issueText.setFocus();
                    return;
                }
            }
        }
        keepLocks = keepLocksButton.getSelection();
        super.okPressed();
    }
    
    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }

    private void saveLocation() {
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("CommitDialog.location.x", x); //$NON-NLS-1$
        settings.put("CommitDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("CommitDialog.size.x", x); //$NON-NLS-1$
        settings.put("CommitDialog.size.y", y); //$NON-NLS-1$   
        TableSetter setter = new TableSetter();
        setter.saveColumnWidths(listViewer.getTable(), "CommitDialog"); //$NON-NLS-1$
        setter.saveSorterColumn("CommitDialog", sorterColumn); //$NON-NLS-1$
        setter.saveSorterReversed("CommitDialog", sorterReversed); //$NON-NLS-1$
    }

    /**
	 * Method createColumns.
	 * @param table
	 * @param layout
	 * @param viewer
	 */
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
	
	/**
	 * Add the selection and deselection buttons to the dialog.
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
	
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		buttonComposite.setLayout(layout);
		GridData data =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		composite.setData(data);
	
		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, Policy.bind("ReleaseCommentDialog.selectAll"), false); //$NON-NLS-1$
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
				selectedResources = null;
			}
		};
		selectButton.addSelectionListener(listener);
	
		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, Policy.bind("ReleaseCommentDialog.deselectAll"), false); //$NON-NLS-1$
		listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);
				selectedResources = new Object[0];
			}
		};
		deselectButton.addSelectionListener(listener);
		
		keepLocksButton = new Button(buttonComposite, SWT.CHECK);
		keepLocksButton.setText(Policy.bind("CommitDialog.keepLocks")); //$NON-NLS-1$

	}
	
    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("CommitDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("CommitDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("CommitDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("CommitDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialSize();
    }	

    /**
	 * Returns the comment.
	 * @return String
	 */
	public String getComment() {
	    if ((projectProperties != null) && (issue != null) && (issue.length() > 0)) {
	        if (projectProperties.isAppend()) 
	            return commitCommentArea.getComment() + "\n" + projectProperties.getResolvedMessage(issue) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	        else
	            return projectProperties.getResolvedMessage(issue) + "\n" + commitCommentArea.getComment(); //$NON-NLS-1$
	    }
		return commitCommentArea.getComment();
	}
	
	/**
	 * Returns the selected resources.
	 * @return IResource[]
	 */
	public IResource[] getSelectedResources() {
		if (selectedResources == null) {
			return resourcesToCommit;
		} else {
			List result = Arrays.asList(selectedResources);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
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
	
	private void setChecks() {
	    boolean selectUnadded = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT);
	    listViewer.setAllChecked(true);
	    if (!selectUnadded) deselectUnadded();
		selectedResources = listViewer.getCheckedElements();
	}

    private void deselectUnadded() {
        TableItem[] items = listViewer.getTable().getItems();
        for (int i = 0; i < items.length; i++) {
           IResource resource = (IResource)items[i].getData();
           ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
           try {
            if (!svnResource.isManaged()) items[i].setChecked(false);
           } catch (SVNException e1) {}
        }
    }
    public boolean isKeepLocks() {
        return keepLocks;
    }
}
