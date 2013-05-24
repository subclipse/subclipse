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
package org.tigris.subversion.subclipse.ui.wizards;

import java.util.Arrays;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.repo.RepositoryComparator;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.util.AdaptableList;

public class CheckoutWizardLocationPage extends WizardPage {
	private TableViewer table;
	private Button newButton;
	private Button existingButton;
	
	private ISVNRepositoryLocation result;

	public CheckoutWizardLocationPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		final CheckoutWizard wizard = (CheckoutWizard)getWizard();
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		newButton = new Button(outerContainer, SWT.RADIO);
		newButton.setText(Policy.bind("CheckoutWizardLocationPage.new")); //$NON-NLS-1$
		
		existingButton = new Button(outerContainer, SWT.RADIO);
		existingButton.setText(Policy.bind("CheckoutWizardLocationPage.existing")); //$NON-NLS-1$

		table = createTable(outerContainer, 1);
		table.setContentProvider(new WorkbenchContentProvider());
		table.setLabelProvider(new WorkbenchLabelProvider());
		table.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				result = (ISVNRepositoryLocation)((IStructuredSelection)table.getSelection()).getFirstElement();
				wizard.setLocation(result);
				setPageComplete(true);
			}
		});
		
		existingButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (newButton.getSelection()) {
					table.getTable().setEnabled(false);
					result = null;
				} else {
					table.getTable().setEnabled(true);
					result = (ISVNRepositoryLocation)((IStructuredSelection)table.getSelection()).getFirstElement();
					wizard.setLocation(result);
				}
				setPageComplete(newButton.getSelection() || !table.getSelection().isEmpty());
			}
		});
		
        Composite cloudForgeComposite = new CloudForgeComposite(outerContainer, SWT.NONE);
        GridData data = new GridData(GridData.VERTICAL_ALIGN_END | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        cloudForgeComposite.setLayoutData(data);
		
		setMessage(Policy.bind("CheckoutWizardLocationPage.text")); //$NON-NLS-1$
		
		setControl(outerContainer);
		
		refreshLocations();
	}
	
	public void refreshLocations() {
        ISVNRepositoryLocation[] locations = SVNUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations(null);
        Arrays.sort(locations, new RepositoryComparator());
        AdaptableList input = new AdaptableList(locations);
        table.setInput(input);
        if (locations.length == 0) {
            newButton.setSelection(true); 
            existingButton.setSelection(false);
            table.getTable().setEnabled(false);
            setPageComplete(true);
        } else {
            existingButton.setSelection(true); 
            newButton.setSelection(false);
            table.getTable().setEnabled(true);
        }		
	}
	
	   /**
     * Creates the table for the repositories
     */
	protected TableViewer createTable(Composite parent, int span) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = span;
		table.setLayoutData(data);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, true));
		table.setLayout(layout);
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
	
		return new TableViewer(table);
	}
	
	public ISVNRepositoryLocation getLocation() {
		return result;
	}
    
    /*
     *  (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
     */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
//		if (visible) {
//			existingButton.setFocus();
//		}
	}
	
	public boolean createNewLocation() {
		return newButton.getSelection();
	}

	public boolean canFlipToNextPage() {
		CheckoutWizard wizard = (CheckoutWizard)getWizard();
		if (wizard != null) {
			return isPageComplete() && wizard.getNextPage(this, false) != null;
		}
		return super.canFlipToNextPage();
	}

}
