/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.repository;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.WorkspacePathValidator;
import org.tigris.subversion.subclipse.ui.actions.OpenRemoteFileAction;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;
import org.tigris.subversion.subclipse.ui.repository.model.AllRootsElement;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;
import org.tigris.subversion.subclipse.ui.wizards.NewLocationWizard;

/**
 * RepositoriesView is a view on a set of known SVN repositories
 * which allows navigation of the structure of the repository and
 * the performing of SVN-specific operations on the repository contents.
 */
public class RepositoriesView extends ViewPart implements ISelectionListener {
	public static final String VIEW_ID = "org.tigris.subversion.subclipse.ui.repository.RepositoriesView"; //$NON-NLS-1$
	
	// The root
	private AllRootsElement root;
	
	// Actions
	private Action newAction;
	private RemoveRootAction removeRootAction;

    // The tree viewer
    protected TreeViewer treeViewer;

    // Drill down adapter
    private DrillDownAdapter drillPart; // Home, back, and "drill into"
    
    private Action refreshAction;
    private Action collapseAllAction;
    private OpenRemoteFileAction openAction;
    private Action propertiesAction;
    
    private RemoteContentProvider contentProvider;
    // this listener is used when a repository is added, removed or changed
	private IRepositoryListener repositoryListener = new IRepositoryListener() {
		public void repositoryAdded(final ISVNRepositoryLocation root) {
			getViewer().getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					refreshViewer(false);
					getViewer().setSelection(new StructuredSelection(root));
				}
			});
		}
		public void repositoryRemoved(ISVNRepositoryLocation root) {
			refresh(false);
		}
		public void repositoriesChanged(ISVNRepositoryLocation[] roots) {
			refresh(false);
		}
        public void remoteResourceDeleted(ISVNRemoteResource resource) {
            refresh(false);
        }
        public void remoteResourceCreated(ISVNRemoteFolder parent,String resourceName) {
            refresh(false);  
        }
        public void remoteResourceCopied(ISVNRemoteResource source,ISVNRemoteFolder destination) {
            refresh(false);  
        }
        public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder,String destinationResourceName) {
            refresh(false);
        }
		private void refresh(boolean refreshRepositoriesFolders) {
            final boolean finalRefreshReposFolders = refreshRepositoriesFolders;
			Display display = getViewer().getControl().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					RepositoriesView.this.refreshViewer(finalRefreshReposFolders);
				}
			});
		}
		public void repositoryModified(ISVNRepositoryLocation root) {
			refresh(false);
		}
	};

	/**
	 * Constructor for RepositoriesView.
	 * @param partName
	 */
	public RepositoriesView() {
	//	super(VIEW_ID);
	}

	/**
	 * Contribute actions to the view
	 */
	protected void contributeActions() {
		
		final Shell shell = getShell();
		
		// Create actions

		// New Repository (popup)
		newAction = new Action(Policy.bind("RepositoriesView.new"), //$NON-NLS-1$
            SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NEWLOCATION)) { //$NON-NLS-1$
			public void run() {
			    if (!WorkspacePathValidator.validateWorkspacePath()) return;
				NewLocationWizard wizard = new NewLocationWizard();
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};
		WorkbenchHelp.setHelp(newAction, IHelpContextIds.NEW_REPOSITORY_LOCATION_ACTION);
		
		// Properties
        propertiesAction = new PropertyDialogAction(shell, getViewer());
        getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);       
        IStructuredSelection selection = (IStructuredSelection)getViewer().getSelection();
        if (selection.size() == 1 && selection.getFirstElement() instanceof ISVNRepositoryLocation) {
            propertiesAction.setEnabled(true);
        } else {
            propertiesAction.setEnabled(false);
        }
        getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection ss = (IStructuredSelection)event.getSelection();
                boolean enabled = ss.size() == 1 && ss.getFirstElement() instanceof ISVNRepositoryLocation;
                propertiesAction.setEnabled(enabled);
            }
        });
        
        // Remove Root
		removeRootAction = new RemoveRootAction(treeViewer.getControl().getShell());
		removeRootAction.selectionChanged((IStructuredSelection)null);
		WorkbenchHelp.setHelp(removeRootAction, IHelpContextIds.REMOVE_REPOSITORY_LOCATION_ACTION);
		
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), removeRootAction);
		
        // Refresh action (toolbar)
        SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
        refreshAction = new Action(Policy.bind("RepositoriesView.refresh"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
            public void run() {
                refreshViewer(true);
            }
        };
        refreshAction.setToolTipText(Policy.bind("RepositoriesView.refreshTooltip")); //$NON-NLS-1$
        refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
        refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
        getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

        // Collapse action
        collapseAllAction = new Action(Policy.bind("RepositoriesView.collapseAll"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_COLLAPSE_ALL_ENABLED)) { //$NON-NLS-1$
            public void run() {
                collapseAll();
            }
        };
        collapseAllAction.setToolTipText(Policy.bind("RepositoriesView.collapseAllTooltip")); //$NON-NLS-1$
        collapseAllAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_COLLAPSE_ALL));
        

        // Create the popup menu
        MenuManager menuMgr = new MenuManager();
        Tree tree = treeViewer.getTree();
        Menu menu = menuMgr.createContextMenu(tree);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                addWorkbenchActions(manager);
            }

        });
        menuMgr.setRemoveAllWhenShown(true);
        tree.setMenu(menu);
        getSite().registerContextMenu(menuMgr, treeViewer);

        // Create the local tool bar
        IToolBarManager tbm = bars.getToolBarManager();
        drillPart.addNavigationActions(tbm);
        tbm.add(refreshAction);
        tbm.add(new Separator());
        tbm.add(collapseAllAction);
        tbm.update(false);


        // Create the open action for double clicks
        openAction = new OpenRemoteFileAction();
        bars.updateActionBars();
	} // contributeActions
	
	/**
	 * @see org.tigris.subversion.subclipse.ui.repo.RemoteViewPart#addWorkbenchActions(org.eclipse.jface.action.IMenuManager)
	 */
	protected void addWorkbenchActions(IMenuManager manager) {
		// New actions go next

		MenuManager sub = new MenuManager(Policy.bind("RepositoriesView.newSubmenu"), IWorkbenchActionConstants.GROUP_ADD); //$NON-NLS-1$
		sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(sub);
		
        // File actions go first (view file)
        manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
        // Misc additions
        manager.add(new Separator("checkoutGroup")); //$NON-NLS-1$
        manager.add(new Separator("miscGroup")); //$NON-NLS-1$
        
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(refreshAction);

	
		IStructuredSelection selection = (IStructuredSelection)getViewer().getSelection();
 		removeRootAction.selectionChanged(selection);
		if(removeRootAction.isEnabled()) {
			manager.add(removeRootAction);
		}		

		if (selection.size() == 1 && selection.getFirstElement() instanceof ISVNRepositoryLocation) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		}
		sub.add(newAction);
	}
	
	/*
	 * @see WorkbenchPart#createPartControl
	 */
	public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        contentProvider = new RemoteContentProvider();
        treeViewer.setContentProvider(contentProvider);
        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        getSite().setSelectionProvider(treeViewer);
        root = new AllRootsElement();
        treeViewer.setInput(root);
		treeViewer.setSorter(new RepositorySorter());
        drillPart = new DrillDownAdapter(treeViewer);
        
        contributeActions(); 

        // F1 Help
        String helpID = getHelpContextId();
        if (helpID != null)
            WorkbenchHelp.setHelp(treeViewer.getControl(), helpID);
        initializeListeners();
		SVNUIPlugin.getPlugin().getRepositoryManager().addRepositoryListener(repositoryListener);
	}
	
    /**
     * initialize the listeners
     */
	protected void initializeListeners() {
        getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
		treeViewer.addSelectionChangedListener(removeRootAction);
        
        // when F5 is pressed, refresh this view
        treeViewer.getControl().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.keyCode == SWT.F5) {
/*                    IStructuredSelection selection = (IStructuredSelection)getViewer().getSelection();
                     if (selection.size() == 1)
                     {
                         getViewer().refresh(selection.getFirstElement());     
                     }      
  */                   
                    refreshAction.run();
                }
            }
        });
        
        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                handleDoubleClick(e);
            }
        });        
	}
    
	/**
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String msg = getStatusLineMessage(selection);
		getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
	}

    /**
     * When selection is changed we update the status line 
     */
	private String getStatusLineMessage(ISelection selection) {
		if (selection==null || selection.isEmpty()) return ""; //$NON-NLS-1$
		if (!(selection instanceof IStructuredSelection)) return ""; //$NON-NLS-1$
		IStructuredSelection s = (IStructuredSelection)selection;
		
		if (s.size() > 1) return Policy.bind("RepositoriesView.NItemsSelected", String.valueOf(s.size())); //$NON-NLS-1$
		Object element = SVNAction.getAdapter(s.getFirstElement(), ISVNResource.class);
		if (element instanceof ISVNRemoteResource) {
			ISVNRemoteResource res = (ISVNRemoteResource)element;
			String name;
			if (res.isContainer()) {
				name = res.getRepositoryRelativePath();
			} else { 
					name = res.getRepositoryRelativePath() + " " + ((ISVNRemoteFile)res).getLastChangedRevision(); //$NON-NLS-1$
			}
	        return Policy.bind("RepositoriesView.ResourceInRepository", name, res.getRepository().getLocation()); //$NON-NLS-1$

		}
		return Policy.bind("RepositoriesView.OneItemSelected"); //$NON-NLS-1$
	}
	
	/**
	 * @see org.tigris.subversion.subclipse.ui.repo.RemoteViewPart#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IHelpContextIds.REPOSITORIES_VIEW;
	}

    /**
     * @see WorkbenchPart#setFocus
     */
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }
    
    /**
     * Method getShell.
     * @return Shell
     */
    protected Shell getShell() {
        return treeViewer.getTree().getShell();
    }

    /**
     * Returns the viewer.
     * @return TreeViewer
     */
    protected TreeViewer getViewer() {
        return treeViewer;
    }

    /**
     * this is called whenever a new repository location is added for example
     * or when user wants to refresh
     */
    protected void refreshViewer(boolean refreshRepositoriesFolders) {
        if (treeViewer == null) return;
        if (refreshRepositoriesFolders)
            SVNProviderPlugin.getPlugin().getRepositories().refreshRepositoriesFolders();
        treeViewer.refresh(); 
    }
    
    public void collapseAll() {
        if (treeViewer == null) return;
        treeViewer.getControl().setRedraw(false);       
        treeViewer.collapseToLevel(treeViewer.getInput(), TreeViewer.ALL_LEVELS);
        treeViewer.getControl().setRedraw(true);
    }
    
    /**
     * The mouse has been double-clicked in the tree, perform appropriate
     * behaviour.
     */
    private void handleDoubleClick(DoubleClickEvent e) {
        // Only act on single selection
        ISelection selection = e.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection)selection;
            if (structured.size() == 1) {
                Object first = structured.getFirstElement();
                if (first instanceof ISVNRemoteFile) {
                    // It's a file, open it.
                    openAction.selectionChanged(null, selection);
                    openAction.run(null);
                } else {
                    // Try to expand/contract
                    treeViewer.setExpandedState(first, !treeViewer.getExpandedState(first));
                }
            }
        } 
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
        SVNUIPlugin.getPlugin().getRepositoryManager().removeRepositoryListener(repositoryListener);
        super.dispose();
        treeViewer = null;
    }

}
