/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.sharing;


import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.repo.RepositoryComparator;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.util.AdaptableList;
import org.tigris.subversion.subclipse.ui.wizards.CloudForgeComposite;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

/**
 * First wizard page for importing a project into a SVN repository.
 * This page prompts the user to select an existing repo or create a new one.
 * If the user selected an existing repo, then getLocation() will return it.
 */
public class RepositorySelectionPage extends SVNWizardPage {
	private TableViewer table;
	private Button useExistingRepo;
	private Button useNewRepo;
	
	private ISVNRepositoryLocation result;
	private ISVNRepositoryLocation[] locations;
	
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	
	private static final String LAST_LOCATION = "RepositorySelectionPage.lastRepository";
	
	/**
	 * RepositorySelectionPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public RepositorySelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
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
	/**
	 * Creates the UI part of the page.
	 * 
	 * @param parent  the parent of the created widgets
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_SELECT_REPOSITORY_PAGE);
		
		createWrappingLabel(composite, Policy.bind("RepositorySelectionPage.description"), 0 /* indent */, 1 /* columns */); //$NON-NLS-1$
		
		useNewRepo = createRadioButton(composite, Policy.bind("RepositorySelectionPage.useNew"), 1); //$NON-NLS-1$
		
		useExistingRepo = createRadioButton(composite, Policy.bind("RepositorySelectionPage.useExisting"), 1); //$NON-NLS-1$
		table = createTable(composite, 1);
		table.setContentProvider(new WorkbenchContentProvider());
		table.setLabelProvider(new WorkbenchLabelProvider());
		table.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				result = (ISVNRepositoryLocation)((IStructuredSelection)table.getSelection()).getFirstElement();
				settings.put(LAST_LOCATION, result.getLocation());
				setPageComplete(canFinish());
			}
		});
		
		SelectionListener selectionListener = new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				if (useNewRepo.getSelection()) {
					table.getTable().setEnabled(false);
					result = null;
				} else {
					table.getTable().setEnabled(true);
					result = (ISVNRepositoryLocation)((IStructuredSelection)table.getSelection()).getFirstElement();
				}
				setPageComplete(canFinish());
			}
		};
		
		useNewRepo.addSelectionListener(selectionListener);
		useExistingRepo.addSelectionListener(selectionListener);
		
	    Composite cloudForgeComposite = new CloudForgeComposite(composite, SWT.NONE);
	    GridData data = new GridData(GridData.VERTICAL_ALIGN_END | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
	    cloudForgeComposite.setLayoutData(data);

		setControl(composite);
		
       	IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            	locations = SVNUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations(monitor);			}
    	};
        try {
			new ProgressMonitorDialog(getShell()).run(true, false, runnable);
		} catch (Exception e) {
            SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
		}

        Arrays.sort(locations, new RepositoryComparator());
        AdaptableList input = new AdaptableList(locations);
        table.setInput(input);
        if (locations.length == 0) {
            useNewRepo.setSelection(true);  
        } else {
            useExistingRepo.setSelection(true); 
            int selectionIndex = 0;
            String lastLocation = settings.get(LAST_LOCATION);
            if (lastLocation != null) {
            	for (int i = 0; i < locations.length; i++) {
            		ISVNRepositoryLocation location = locations[i];
            		if (lastLocation.equals(location.getLocation())) {
            			selectionIndex = i;
            			break;
            		}
            	}
            }
            table.setSelection(new StructuredSelection(locations[selectionIndex]));
            result = locations[selectionIndex];
        }
        setPageComplete(canFinish());
	}
	
	public ISVNRepositoryLocation getLocation() {
		return result;
	}
 
	private boolean canFinish() {
		if (useNewRepo.getSelection()) {
			return true;
		}
		else {
			return !table.getSelection().isEmpty();
		}
	}
}
