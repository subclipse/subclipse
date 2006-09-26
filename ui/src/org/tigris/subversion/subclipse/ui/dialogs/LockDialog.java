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
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.util.TableSetter;

public class LockDialog extends TrayDialog {
	private static final int WIDTH_HINT = 500;
	private final static int SELECTION_HEIGHT_HINT = 100;
    
    private CommitCommentArea commitCommentArea;
    private Button stealButton;
    private String comment;
    private boolean stealLock;
    private IResource[] files;
    private Button okButton;
    private CommentProperties commentProperties;
    private TableViewer listViewer;
    
    private IDialogSettings settings;
    private TableSetter setter;

    public LockDialog(Shell parentShell, IResource[] files) {
        super(parentShell);
        this.files = files;
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
		if (files.length > 0) {
            try {
                commentProperties = CommentProperties.getCommentProperties(files[0]);
                commentProperties.setMinimumLogMessageSize(commentProperties.getMinimumLockMessageSize());
            } catch (SVNException e) {}
            if (commentProperties != null) {
                commentProperties.setLogTemplate(null);
            }
		}		
        commitCommentArea = new CommitCommentArea(this, null, Policy.bind("LockDialog.enterComment"), commentProperties); //$NON-NLS-1$
		if ((commentProperties != null) && (commentProperties.getMinimumLockMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    okButton.setEnabled(commitCommentArea.getComment().trim().length() >= commentProperties.getMinimumLockMessageSize());
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener);
		}   
		commitCommentArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommitCommentArea.OK_REQUESTED)
					okPressed();
			}
		});
        settings = SVNUIPlugin.getPlugin().getDialogSettings();
        setter = new TableSetter();
    }
    
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("LockDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        SashForm sashForm = new SashForm(composite, SWT.VERTICAL);
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
		
		commitCommentArea.createArea(cTop);
        
        addResourcesArea(cBottom2);
		
		stealButton = new Button(cBottom2, SWT.CHECK);
		stealButton.setText(Policy.bind("LockDialog.stealLock")); //$NON-NLS-1$
		

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LOCK_DIALOG);	
		
		return composite;
	}
	
    private void addResourcesArea(Composite composite) {
		Label label = createWrappingLabel(composite);
		label.setText(Policy.bind("LockDialog.resources")); //$NON-NLS-1$ 
		Table table = new Table(composite, 
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | 
                SWT.MULTI | SWT.BORDER);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		
		listViewer = new TableViewer(table);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SELECTION_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		listViewer.getTable().setLayoutData(data);
		createColumns(table, layout);
		// set the contents of the list
		listViewer.setLabelProvider(new ResourceLabelProvider());
		listViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return files;
            }
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }	    
		});
		listViewer.setInput(new AdaptableResourceList(files));		
    }
    
    private void createColumns(Table table, TableLayout layout) {
        int[] widths = setter.getColumnWidths("LockDialog", 1); //$NON-NLS-1$        
        TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("PendingOperationsView.resource")); //$NON-NLS-1$
		if ((widths[0] == 0) || (widths[0] == 150)) widths[0] = 500;
		layout.addColumnData(new ColumnPixelData(widths[0], true));        
    }

    protected void okPressed() {
        saveLocation();
        stealLock = stealButton.getSelection();
        comment = commitCommentArea.getComment();
        super.okPressed();
    }
    
    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }
    
	protected Button createButton(
			Composite parent,
			int id,
			String label,
			boolean defaultButton) {
			Button button = super.createButton(parent, id, label, defaultButton);
			if (id == IDialogConstants.OK_ID) {
				okButton = button;
				if ((commentProperties != null) && (commentProperties.getMinimumLockMessageSize() != 0)) {
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
    
    private void saveLocation() {
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("LockDialog.location.x", x); //$NON-NLS-1$
        settings.put("LockDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("LockDialog.size.x", x); //$NON-NLS-1$
        settings.put("LockDialog.size.y", y); //$NON-NLS-1$  
        TableSetter setter = new TableSetter();
        setter.saveColumnWidths(listViewer.getTable(), "LockDialog"); //$NON-NLS-1$        
    }
    
    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("LockDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("LockDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("LockDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("LockDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialSize();
    }	   
    
    public String getComment() {
        return comment;
    }
    
    public boolean isStealLock() {
        return stealLock;
    }
}
