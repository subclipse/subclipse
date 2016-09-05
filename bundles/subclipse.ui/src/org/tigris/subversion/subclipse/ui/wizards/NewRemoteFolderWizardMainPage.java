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
package org.tigris.subversion.subclipse.ui.wizards;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownComposite;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.repository.RepositoryFilters;
import org.tigris.subversion.subclipse.ui.repository.model.AllRootsElement;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * the main wizard page for creating a new remote folder on repository
 */
public class NewRemoteFolderWizardMainPage extends SVNWizardPage {

    private static final int LIST_WIDTH = 250;
    private static final int LIST_HEIGHT = 300;

    private Text urlParentText;
    private Text folderNameText;

    private TreeViewer viewer;

    private ISVNRemoteFolder parentFolder;

    private ISelectionChangedListener treeSelectionChangedListener = new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
            Object selection = ((IStructuredSelection)event.getSelection()).getFirstElement();

            if (selection instanceof ISVNRemoteFolder) {
                parentFolder = (ISVNRemoteFolder)selection;

            }
            else
            if (selection instanceof IAdaptable) {
                // ISVNRepositoryLocation is adaptable to ISVNRemoteFolder
                IAdaptable a = (IAdaptable) selection;
                Object adapter = a.getAdapter(ISVNRemoteFolder.class);
                parentFolder = (ISVNRemoteFolder)adapter;
            }

            if (parentFolder != null)
                urlParentText.setText(parentFolder.getUrl().toString());
        }

    };


	/**
	 * NewRemoteFolderWizardMainPage constructor.
	 *
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public NewRemoteFolderWizardMainPage(
        String pageName,
        String title,
        ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * Creates the UI part of the page.
	 *
	 * @param parent  the parent of the created widgets
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CREATE_REMOTE_FOLDER_PAGE);

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				validateFields();
			}
		};

        // the text field for the parent folder
		createLabel(composite, Policy.bind("NewRemoteFolderWizardMainPage.selectParentUrl")); //$NON-NLS-1$

        urlParentText = createTextField(composite);
        urlParentText.addListener(SWT.Selection, listener);
        urlParentText.addListener(SWT.Modify, listener);
        urlParentText.setEditable(false);


        // Create drill down.
        DrillDownComposite drillDown = new DrillDownComposite(composite, SWT.BORDER);
        GridData spec = new GridData(GridData.FILL_BOTH);
        spec.widthHint = LIST_WIDTH;
        spec.heightHint = LIST_HEIGHT;
        drillDown.setLayoutData(spec);

        // Create tree viewer inside drill down.
        viewer = new TreeViewer(drillDown, SWT.H_SCROLL | SWT.V_SCROLL);
        drillDown.setChildTree(viewer);
        viewer.setLabelProvider(new WorkbenchLabelProvider());
        RemoteContentProvider remoteContentProvider = new RemoteContentProvider();
        remoteContentProvider.setUseDeferredContentManager(false);
        viewer.setContentProvider(remoteContentProvider);
        viewer.setInput(new AllRootsElement());
        viewer.addFilter(RepositoryFilters.FOLDERS_ONLY);
        viewer.addSelectionChangedListener(treeSelectionChangedListener);

        // the text field for the folder name
        createLabel(composite, Policy.bind("NewRemoteFolderWizardMainPage.folderName")); //$NON-NLS-1$

        folderNameText = createTextField(composite);
        folderNameText.addListener(SWT.Selection, listener);
        folderNameText.addListener(SWT.Modify, listener);

		validateFields();
        folderNameText.setFocus();

		setControl(composite);

        // set the initial selection in the tree
        if (parentFolder != null) {      	
        	List itemsToExpand = new ArrayList();
        	ISVNRemoteFolder remoteParent = parentFolder.getParent();
        	while (remoteParent != null) {
    			if (remoteParent.getParent() == null) itemsToExpand.add(0, remoteParent.getRepository());
    			else itemsToExpand.add(0, remoteParent);
    			remoteParent = remoteParent.getParent();        		
        	}

        	viewer.setExpandedElements(itemsToExpand.toArray());
        	if (parentFolder.getParent() == null) viewer.setSelection(new StructuredSelection(parentFolder.getRepository()), true);
        	else viewer.setSelection(new StructuredSelection(parentFolder), true);
        }

	}

	/**
	 * Validates the contents of the editable fields and set page completion
	 * and error messages appropriately. Call each time folder name or parent url
     * is modified
	 */
	private void validateFields() {
		if (folderNameText.getText().length() == 0) {
			setErrorMessage(null);
			setPageComplete(false);
			return;
		}
		try {
			new SVNUrl(Util.appendPath(urlParentText.getText(), folderNameText.getText()));
		} catch (MalformedURLException e) {
			setErrorMessage(Policy.bind("NewRemoteFolderWizardMainPage.invalidUrl")); //$NON-NLS-1$);
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}


	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
            folderNameText.setFocus();
		}
	}

    public String getFolderName() {
        return folderNameText.getText();
    }

    public ISVNRemoteFolder getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(ISVNRemoteFolder parentFolder) {
        this.parentFolder = parentFolder;
    }

}
