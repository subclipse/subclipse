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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.repository.RepositoryFilters;

public class CheckoutWizardSelectionPage extends WizardPage {
    private static final int LIST_HEIGHT_HINT = 250;
    private static final int LIST_WIDTH_HINT = 450;

    private ISVNRepositoryLocation repositoryLocation;

    private TreeViewer treeViewer;

	public CheckoutWizardSelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		treeViewer = new TreeViewer(outerContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		RepositoryContentProvider contentProvider = new RepositoryContentProvider();
        treeViewer.setContentProvider(contentProvider);
        treeViewer.addFilter(RepositoryFilters.FOLDERS_ONLY);
        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        treeViewer.setInput(repositoryLocation);

		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.widthHint = LIST_WIDTH_HINT;
		treeViewer.getControl().setLayoutData(data);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				CheckoutWizard wizard = (CheckoutWizard)getWizard();
				ArrayList folderArray = new ArrayList();
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				Iterator iter = selection.iterator();
				while (iter.hasNext()) {
					Object object = iter.next();
					if (object instanceof ISVNRemoteFolder || object instanceof ISVNRepositoryLocation) {
						if (object instanceof ISVNRepositoryLocation) folderArray.add(((ISVNRepositoryLocation)object).getRootFolder());
						else folderArray.add(object);
					}
				}
				ISVNRemoteFolder[] remoteFolders = new ISVNRemoteFolder[folderArray.size()];
				folderArray.toArray(remoteFolders);
				wizard.setRemoteFolders(remoteFolders);
				setPageComplete(!treeViewer.getSelection().isEmpty());
			}
		});
		
        final Action refreshAction = new Action(Policy.bind("RepositoriesView.refreshPopup"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH)) { //$NON-NLS-1$
            public void run() {
            	refreshViewerNode();
            }
        };
        MenuManager menuMgr = new MenuManager();
        Tree tree = treeViewer.getTree();
        Menu menu = menuMgr.createContextMenu(tree);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(refreshAction);
            }

        });
        menuMgr.setRemoveAllWhenShown(true);
        tree.setMenu(menu);

		setMessage(Policy.bind("CheckoutWizardSelectionPage.text")); //$NON-NLS-1$

		setControl(outerContainer);
	}

	public boolean canFlipToNextPage() {
		CheckoutWizard wizard = (CheckoutWizard)getWizard();
		if (wizard != null) {
			return isPageComplete() && wizard.getNextPage(this, false) != null;
		}
		return super.canFlipToNextPage();
	}

	public void setLocation(ISVNRepositoryLocation repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
		if (treeViewer != null) {
			treeViewer.setInput(repositoryLocation.getLocation());
			treeViewer.refresh();
			treeViewer.expandToLevel(2);
		}
	}
	
    protected void refreshViewerNode() {
    	IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
        	Object object = iter.next();
        	if (object instanceof ISVNRepositoryLocation) ((ISVNRepositoryLocation)object).refreshRootFolder();
        	if (object instanceof ISVNRemoteFolder) ((ISVNRemoteFolder)object).refresh();
        	treeViewer.refresh(object); 
        }
    }
	
	class RepositoryContentProvider extends WorkbenchContentProvider {
		private DeferredTreeContentManager manager;
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (viewer instanceof AbstractTreeViewer) {
				manager = new DeferredTreeContentManager(this, (AbstractTreeViewer) viewer);
			}
			super.inputChanged(viewer, oldInput, newInput);
		}
		
		public boolean hasChildren(Object element) {
			if (element == null) return false;
			else return true;
		}
		
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {
				Object[] root = { repositoryLocation };
				return root;
			}
			if (manager != null) {
				Object[] children = manager.getChildren(parentElement);
				return children;
			}
			return super.getChildren(parentElement);
		}
	}

}
