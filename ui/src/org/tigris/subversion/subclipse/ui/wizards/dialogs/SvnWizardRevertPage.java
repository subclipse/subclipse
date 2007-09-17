package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.AdaptableResourceList;
import org.tigris.subversion.subclipse.ui.dialogs.CompareDialog;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusLabelProvider;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusSorter;
import org.tigris.subversion.subclipse.ui.util.TableSetter;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SvnWizardRevertPage extends SvnWizardDialogPage {
	
	private static final int WIDTH_HINT = 500;
	private final static int SELECTION_HEIGHT_HINT = 250;	
	
    private IResource[] resourcesToRevert;
    private String url;
    private Object[] selectedResources;
    private CheckboxTableViewer listViewer;  
    private TableSetter setter;
    private int sorterColumn = 1;
    private boolean sorterReversed = false;
	
	public SvnWizardRevertPage(IResource[] resourcesToRevert, String url) {
		super("RevertDialog", Policy.bind("RevertDialog.title")); //$NON-NLS-1$
		this.resourcesToRevert = resourcesToRevert;
		this.url = url;
		setter = new TableSetter();
	}

	public void createControls(Composite outerContainer) {
		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Label label = createWrappingLabel(composite);
		if (url == null) label.setText(Policy.bind("RevertDialog.url") + " " + Policy.bind("RevertDialog.multiple")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else label.setText(Policy.bind("RevertDialog.url") + " " + url); //$NON-NLS-1$ //$NON-NLS-2$
		
		addResourcesArea(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REVERT_DIALOG);		

	}

	private void addResourcesArea(Composite composite) {
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
		listViewer.setLabelProvider(new ResourceWithStatusLabelProvider(this.url));
		
		int sort = setter.getSorterColumn("RevertDialog"); //$NON-NLS-1$
		if (sort != -1) sorterColumn = sort;
		ResourceWithStatusSorter sorter = new ResourceWithStatusSorter(sorterColumn);
		sorter.setReversed(setter.getSorterReversed("RevertDialog")); //$NON-NLS-1$		
		listViewer.setSorter(sorter);
		
		listViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return resourcesToRevert;
            }
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }	    
		});
		listViewer.setInput(new AdaptableResourceList(resourcesToRevert));
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

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		return true;
	}

	public void setMessage() {
		setMessage(Policy.bind("RevertDialog.resources")); //$NON-NLS-1$
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
		
		int[] widths = setter.getColumnWidths("RevertDialog", 4); //$NON-NLS-1$

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
		selectedResources = listViewer.getCheckedElements();
	}	
	
	private void addSelectionButtons(Composite composite) {
		
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
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
	}

	public void saveSettings() {
        setter.saveColumnWidths(listViewer.getTable(), "RevertDialog"); //$NON-NLS-1$
        setter.saveSorterColumn("RevertDialog", sorterColumn); //$NON-NLS-1$  
        setter.saveSorterReversed("RevertDialog", sorterReversed); //$NON-NLS-1$
	}
	
	public IResource[] getSelectedResources() {
		if (selectedResources == null) {
			return resourcesToRevert;
		} else {
			List result = Arrays.asList(selectedResources);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
	}	

}
