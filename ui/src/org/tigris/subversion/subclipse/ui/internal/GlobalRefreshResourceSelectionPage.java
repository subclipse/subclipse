/*******************************************************************************
 * copied from: org.eclipse.team.internal.ui.synchronize.GlobalRefreshResourceSelectionPage
 * 
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.WorkingSetScope;
import org.eclipse.team.ui.synchronize.WorkspaceScope;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Page that allows the user to select a set of resources that are managed by a subscriber 
 * participant. Callers can provide a scope hint to determine the initial selection for the
 * resource list. By default, the resources in the current selection are checked, otherwise
 * all resources are checked.
 * 
 * @see SubscriberRefreshWizard
 * @since 3.0
 */
public class GlobalRefreshResourceSelectionPage extends WizardPage {
	
	private boolean scopeCheckingElement = false;
	
	// Set of scope hint to determine the initial selection
	private Button participantScope;
	private Button selectedResourcesScope;
	private Button workingSetScope;
	
	// The checked tree viewer
	private ContainerCheckedTreeViewer fViewer;
	
	// Working set label and holder
	private Text workingSetLabel;
	private IWorkingSet[] workingSets;
	private List resources;
	private IDialogSettings settings;
	
	// dialog settings
	/** 
	 * Settings constant for section name (value <code>SynchronizeResourceSelectionDialog</code>).
	 */
	private static final String STORE_SECTION = "SynchronizeResourceSelectionDialog"; //$NON-NLS-1$
	/** 
	 * Settings constant for working sets (value <code>SynchronizeResourceSelectionDialog.STORE_WORKING_SET</code>).
	 */
	private static final String STORE_WORKING_SETS = "SynchronizeResourceSelectionDialog.STORE_WORKING_SETS"; //$NON-NLS-1$
	
	/**
	 * Content provider that accepts a <code>SubscriberParticipant</code> as input and
	 * returns the participants root resources.
	 */
	class MyContentProvider extends BaseWorkbenchContentProvider {
		public Object[] getChildren(Object element) {
			if(element instanceof List) {
				return (IResource[]) ((List)element).toArray(new IResource[((List)element).size()]);
			}
			return super.getChildren(element);
		}
	}
	
	/**
	 * Label decorator that will display the full path for participant roots that are folders. This
	 * is useful for participants that have non-project roots.
	 */
	class MyLabelProvider extends LabelProvider {
		private LabelProvider workbenchProvider = new WorkbenchLabelProvider();
		public String getText(Object element) {
			if(element instanceof IContainer) {
				IContainer c = (IContainer)element;
				if(c.getType() != IResource.PROJECT && resources.contains(c)) {
					return c.getFullPath().toString();
				}
			}
			return workbenchProvider.getText(element);
		}	
		public Image getImage(Object element) {
			return workbenchProvider.getImage(element);
		}
	}
		
	/**
	 * Create a new page for the given participant. The scope hint will determine the initial selection.
	 * 
	 * @param participant the participant to synchronize
	 */
	public GlobalRefreshResourceSelectionPage(IResource[] resources) {
		super(Policy.bind("GlobalRefreshResourceSelectionPage.1")); //$NON-NLS-1$
		// Caching the roots so that the decorator doesn't have to recompute all the time.
		this.resources = Arrays.asList(resources);
		setDescription(Policy.bind("GlobalRefreshResourceSelectionPage.2")); //$NON-NLS-1$
		setTitle(Policy.bind("GlobalRefreshResourceSelectionPage.3")); //$NON-NLS-1$
		IDialogSettings s = SVNUIPlugin.getPlugin().getDialogSettings();
		this.settings = s.getSection(STORE_SECTION);
		if(settings == null) {
			settings = s.addNewSection(STORE_SECTION);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent2) {
		Composite top = new Composite(parent2, SWT.NULL);
		top.setLayout(new GridLayout());
		initializeDialogUnits(top);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 50;
		top.setLayoutData(data);
		setControl(top);

		
		Label l = new Label(top, SWT.NULL);
		l.setText(Policy.bind("GlobalRefreshResourceSelectionPage.5")); //$NON-NLS-1$

		// The viewer
		fViewer = new ContainerCheckedTreeViewer(top, SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		//data.widthHint = 200;
		data.heightHint = 100;
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(new MyContentProvider());
		fViewer.setLabelProvider(new DecoratingLabelProvider(new MyLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				updateOKStatus();
			}
		});
		fViewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
		fViewer.setInput(resources);

		Composite selectGroup = new Composite(top, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		//layout.makeColumnsEqualWidth = false;
		selectGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		selectGroup.setLayoutData(data);

		Button selectAll = new Button(selectGroup, SWT.NULL);
		selectAll.setText(Policy.bind("GlobalRefreshResourceSelectionPage.12")); //$NON-NLS-1$
		selectAll.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				participantScope.setSelection(true);
				selectedResourcesScope.setSelection(false);
				workingSetScope.setSelection(false);
				updateParticipantScope();
				scopeCheckingElement = true;
				updateOKStatus();
				scopeCheckingElement = false;
			}
		});
		setButtonLayoutData(selectAll);

		Button deSelectAll = new Button(selectGroup, SWT.NULL);
		deSelectAll.setText(Policy.bind("GlobalRefreshResourceSelectionPage.13")); //$NON-NLS-1$
		deSelectAll.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				fViewer.setCheckedElements(new Object[0]);
				updateOKStatus();
			}
		});
		setButtonLayoutData(deSelectAll);

		// Scopes
		Group scopeGroup = new Group(top, SWT.NULL);
		scopeGroup.setText(Policy.bind("GlobalRefreshResourceSelectionPage.6")); //$NON-NLS-1$
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		scopeGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 50;
		scopeGroup.setLayoutData(data);

		participantScope = new Button(scopeGroup, SWT.RADIO);
		participantScope.setText(Policy.bind("GlobalRefreshResourceSelectionPage.7")); //$NON-NLS-1$
		participantScope.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				updateParticipantScope();
			}
		});

		selectedResourcesScope = new Button(scopeGroup, SWT.RADIO);
		selectedResourcesScope.setText(Policy.bind("GlobalRefreshResourceSelectionPage.8")); //$NON-NLS-1$
		selectedResourcesScope.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				updateSelectedResourcesScope();
			}
		});
		data = new GridData();
		data.horizontalSpan = 2;
		selectedResourcesScope.setLayoutData(data);

		workingSetScope = new Button(scopeGroup, SWT.RADIO);
		workingSetScope.setText(Policy.bind("GlobalRefreshResourceSelectionPage.10")); //$NON-NLS-1$
		workingSetScope.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if (workingSetScope.getSelection()) {
					updateWorkingSetScope();
				}
			}
		});

		workingSetLabel = new Text(scopeGroup, SWT.BORDER);
		workingSetLabel.setEditable(false);
		data = new GridData(GridData.FILL_HORIZONTAL);
		workingSetLabel.setLayoutData(data);

		Button selectWorkingSetButton = new Button(scopeGroup, SWT.NULL);
		selectWorkingSetButton.setText(Policy.bind("GlobalRefreshResourceSelectionPage.11")); //$NON-NLS-1$
		selectWorkingSetButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				selectWorkingSetAction();
			}
		});
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		selectWorkingSetButton.setLayoutData(data);
		Dialog.applyDialogFont(selectWorkingSetButton);

		initializeScopingHint();
		Dialog.applyDialogFont(top);
	}
	
	/**
	 * Allow the finish button to be pressed if there are checked resources.
	 *
	 */
	protected void updateOKStatus() {	
		if(fViewer != null) {
			if(! scopeCheckingElement) {
				if(! selectedResourcesScope.getSelection()) {
					selectedResourcesScope.setSelection(true);
					participantScope.setSelection(false);
					workingSetScope.setSelection(false);
					updateSelectedResourcesScope();
				}
			}
			setPageComplete(areAnyElementsChecked() != null);
		} else {
			setPageComplete(false);
		}
	}
	
	/**
	 * Returns <code>true</code> if any of the root resources are grayed.
	 */
	private IResource areAnyElementsChecked() {
		TreeItem[] item = fViewer.getTree().getItems();
		for (int i = 0; i < item.length; i++) {
			TreeItem child = item[i];
			if(child.getChecked() || child.getGrayed()) {
				return (IResource)child.getData();
			}
		}
		return null;
	}
	
	/**
	 * Return the list of top-most resources that have been checked.
	 * 
	 * @return  the list of top-most resources that have been checked or an
	 * empty list if nothing is selected.
	 */
	public IResource[] getRootResources() {
		TreeItem[] item = fViewer.getTree().getItems();
		List checked = new ArrayList();
		for (int i = 0; i < item.length; i++) {
			TreeItem child = item[i];
			collectCheckedItems(child, checked);
		}
		return (IResource[]) checked.toArray(new IResource[checked.size()]);
	}
	
	public ISynchronizeScope getSynchronizeScope() {
		if (workingSetScope.getSelection()) {
			return new WorkingSetScope(workingSets);
		}
		if (participantScope.getSelection()) {
			return new WorkspaceScope();
		}
		return new ResourceScope(getRootResources());
	}
	
	private void initializeScopingHint() {
		String working_sets = settings.get(STORE_WORKING_SETS);
		if (working_sets == null) {
			participantScope.setSelection(true);
			updateParticipantScope();
		} else {
			StringTokenizer st = new StringTokenizer(working_sets, " ,"); //$NON-NLS-1$
			ArrayList ws = new ArrayList();
			while (st.hasMoreTokens()) {
				String workingSetName = st.nextToken();
				if (workingSetName != null && workingSetName.equals("") == false) { //$NON-NLS-1$
					IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
					IWorkingSet workingSet = workingSetManager.getWorkingSet(workingSetName);
					if (workingSet != null) {
						ws.add(workingSet);
					}
				}
			}
			if(! ws.isEmpty()) {
				this.workingSets = (IWorkingSet[]) ws.toArray(new IWorkingSet[ws.size()]);
				updateWorkingSetScope();
				updateWorkingSetLabel();			
				participantScope.setSelection(false);
				selectedResourcesScope.setSelection(false);
				workingSetScope.setSelection(true);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		if(workingSets != null && workingSetScope.getSelection()) {
			String concatsWorkingSets = makeWorkingSetLabel();
			settings.put(STORE_WORKING_SETS, concatsWorkingSets);
		} else {
			settings.put(STORE_WORKING_SETS, (String)null);
		}
	}
	
	private void updateParticipantScope() {
		if(participantScope.getSelection()) {
			scopeCheckingElement = true;
			fViewer.setCheckedElements(resources.toArray());
			setPageComplete(getRootResources().length > 0);
			scopeCheckingElement = false;
		}
	}
	
	private void updateSelectedResourcesScope() {
		setPageComplete(getRootResources().length > 0);
	}
	
	private void selectWorkingSetAction() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(getShell(), true);
		dialog.open();
		IWorkingSet[] sets = dialog.getSelection();
		if(sets != null) {
			workingSets = sets;
		} else {
			// dialog cancelled
			return;
		}
		updateWorkingSetScope();
		updateWorkingSetLabel();
		
		participantScope.setSelection(false);
		selectedResourcesScope.setSelection(false);
		workingSetScope.setSelection(true);
	}
	
	private void updateWorkingSetScope() {
		if(workingSets != null) {
			List allWorkingSetResources = new ArrayList();
			for (int i = 0; i < workingSets.length; i++) {
				IWorkingSet set = workingSets[i];
				allWorkingSetResources.addAll(IDE.computeSelectedResources(new StructuredSelection(set.getElements())));
			}
			scopeCheckingElement = true;
			fViewer.setCheckedElements(allWorkingSetResources.toArray(new IResource[allWorkingSetResources.size()]));
			scopeCheckingElement = false;
			setPageComplete(true);
		} else {
			scopeCheckingElement = true;
			fViewer.setCheckedElements(new Object[0]);
			scopeCheckingElement = false;
			setPageComplete(false);
		}
	}
	
	private void collectCheckedItems(TreeItem item, List checked) {
		if(item.getChecked() && !item.getGrayed()) {
			checked.add(item.getData());
		} else if(item.getGrayed()) {
			TreeItem[] children = item.getItems();
			for (int i = 0; i < children.length; i++) {
				TreeItem child = children[i];
				collectCheckedItems(child, checked);
			}
		}
	}
	
	private void updateWorkingSetLabel() {
		if (workingSets == null) {
			workingSetLabel.setText(Policy.bind("StatisticsPanel.noWorkingSet")); //$NON-NLS-1$
		} else {
			workingSetLabel.setText(makeWorkingSetLabel());
		}
	}

	/**
	 * @return
	 */
	private String makeWorkingSetLabel() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < workingSets.length; i++) {
			IWorkingSet set = workingSets[i];
			if(i != 0) buffer.append(" ,"); //$NON-NLS-1$
			buffer.append(set.getName());
		}
		return buffer.toString();
	}
}
