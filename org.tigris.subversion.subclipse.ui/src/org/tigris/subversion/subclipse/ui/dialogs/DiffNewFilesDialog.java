/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.util.TableSetter;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class DiffNewFilesDialog extends TrayDialog {
    
	private static final int WIDTH_HINT = 500;
	private final static int SELECTION_HEIGHT_HINT = 250;
    
    private IResource[] newResourcesToIncludeInDiff;
    private Object[] selectedResources;
    private CheckboxTableViewer listViewer;
    
    private IDialogSettings settings;
    private TableSetter setter;
    private int sorterColumn = 1;
    private boolean sorterReversed = false;

    public DiffNewFilesDialog(Shell parentShell, IResource[] newResourcesToIncludeInDiff) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
		this.newResourcesToIncludeInDiff = newResourcesToIncludeInDiff;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		setter = new TableSetter();
    }
    
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("DiffNewFilesDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		addResourcesArea(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REVERT_DIALOG);

		return composite;
	}
	
	private void addResourcesArea(Composite composite) {
	    
		// add a description label
		Label label = createWrappingLabel(composite);
		label.setText(Policy.bind("DiffNewFilesDialog.resources")); //$NON-NLS-1$
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
		listViewer.setLabelProvider(new ResourceWithStatusLabelProvider(null));
		
		int sort = setter.getSorterColumn("DiffNewFilesDialog"); //$NON-NLS-1$
		if (sort != -1) sorterColumn = sort;
		ResourceWithStatusSorter sorter = new ResourceWithStatusSorter(sorterColumn);
		sorter.setReversed(setter.getSorterReversed("DiffNewFilesDialog")); //$NON-NLS-1$		
		listViewer.setSorter(sorter);
		
		listViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return newResourcesToIncludeInDiff;
            }
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }	    
		});
		listViewer.setInput(new AdaptableResourceList(newResourcesToIncludeInDiff));
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
	
    private void saveLocation() {
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("DiffNewFilesDialog.location.x", x); //$NON-NLS-1$
        settings.put("DiffNewFilesDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("DiffNewFilesDialog.size.x", x); //$NON-NLS-1$
        settings.put("DiffNewFilesDialog.size.y", y); //$NON-NLS-1$
        TableSetter setter = new TableSetter();
        setter.saveColumnWidths(listViewer.getTable(), "DiffNewFilesDialog"); //$NON-NLS-1$
        setter.saveSorterColumn("DiffNewFilesDialog", sorterColumn); //$NON-NLS-1$  
        setter.saveSorterReversed("DiffNewFilesDialog", sorterReversed); //$NON-NLS-1$
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
		
		int[] widths = setter.getColumnWidths("DiffNewFilesDialog", 4); //$NON-NLS-1$

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
		layout.numColumns = 2;
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
	}
	
    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }

    protected void okPressed() {
        saveLocation();
        super.okPressed();
    }
    
    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("DiffNewFilesDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("DiffNewFilesDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("DiffNewFilesDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("DiffNewFilesDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialSize();
    }	    
    
	/**
	 * Returns the selected resources.
	 * @return IResource[]
	 */
	public IResource[] getSelectedResources() {
		if (selectedResources == null) {
			return newResourcesToIncludeInDiff;
		} else {
			List result = Arrays.asList(selectedResources);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
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
	    listViewer.setAllChecked(true);
		selectedResources = listViewer.getCheckedElements();
	}
}
