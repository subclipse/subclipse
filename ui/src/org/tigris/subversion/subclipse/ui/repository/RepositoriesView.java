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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.OpenRemoteFileAction;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;
import org.tigris.subversion.subclipse.ui.repository.model.AllRootsElement;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;
import org.tigris.subversion.subclipse.ui.wizards.NewLocationWizard;
import org.tigris.subversion.subclipse.ui.wizards.NewRemoteFolderWizard;

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
    private Action newRemoteFolderAction;
	private PropertyDialogAction propertiesAction;
	private RemoveRootAction removeRootAction;

    // The tree viewer
    protected TreeViewer viewer;

    // Drill down adapter
    private DrillDownAdapter drillPart; // Home, back, and "drill into"
    
    private Action refreshAction;
    private Action collapseAllAction;
    private OpenRemoteFileAction openAction;
    
    private RemoteContentProvider contentProvider;
    private IDialogSettings settings;


	// this listener is used when a repository is added, removed or changed
	IRepositoryListener listener = new IRepositoryListener() {
		public void repositoryAdded(final ISVNRepositoryLocation root) {
			getViewer().getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					refreshViewer();
					getViewer().setSelection(new StructuredSelection(root));
				}
			});
		}
		public void repositoryRemoved(ISVNRepositoryLocation root) {
			refresh();
		}
		public void repositoriesChanged(ISVNRepositoryLocation[] roots) {
			refresh();
		}
		private void refresh() {
			Display display = getViewer().getControl().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					RepositoriesView.this.refreshViewer();
				}
			});
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
		newAction = new Action(Policy.bind("RepositoriesView.new"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NEWLOCATION)) { //$NON-NLS-1$
			public void run() {
				NewLocationWizard wizard = new NewLocationWizard();
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};
		WorkbenchHelp.setHelp(newAction, IHelpContextIds.NEW_REPOSITORY_LOCATION_ACTION);
		
        newRemoteFolderAction = new Action("New folder", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NEWLOCATION)) { //$NON-NLS-1$
            public void run() {
                NewRemoteFolderWizard wizard = new NewRemoteFolderWizard();
                WizardDialog dialog = new WizardDialog(shell, wizard);
                dialog.open();
            }
        };
        //WorkbenchHelp.setHelp(newAction, IHelpContextIds.NEW_REPOSITORY_LOCATION_ACTION);
        
        
        
/*		// Properties
		propertiesAction = new PropertyDialogAction(shell, getViewer());
		getViewSite().getActionBars().setGlobalActionHandler(IWorkbenchActionConstants.PROPERTIES, propertiesAction);		
		IStructuredSelection selection = (IStructuredSelection)getViewer().getSelection();
		if (selection.size() == 1 && selection.getFirstElement() instanceof RepositoryRoot) {
			propertiesAction.setEnabled(true);
		} else {
			propertiesAction.setEnabled(false);
		}
		getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection)event.getSelection();
				boolean enabled = ss.size() == 1 && ss.getFirstElement() instanceof RepositoryRoot;
				propertiesAction.setEnabled(enabled);
			}
		}); */
		removeRootAction = new RemoveRootAction(viewer.getControl().getShell());
		removeRootAction.selectionChanged((IStructuredSelection)null);
		WorkbenchHelp.setHelp(removeRootAction, IHelpContextIds.REMOVE_REPOSITORY_LOCATION_ACTION);
		
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(IWorkbenchActionConstants.DELETE, removeRootAction);
		
//      final Shell shell = getShell();
        
        // Refresh action (toolbar)
        SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
        refreshAction = new Action(Policy.bind("RepositoriesView.refresh"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
            public void run() {
                refreshViewer();
            }
        };
        refreshAction.setToolTipText(Policy.bind("RepositoriesView.refreshTooltip")); //$NON-NLS-1$
        refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
        refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
        getViewSite().getActionBars().setGlobalActionHandler(IWorkbenchActionConstants.REFRESH, refreshAction);

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
        Tree tree = viewer.getTree();
        Menu menu = menuMgr.createContextMenu(tree);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                addWorkbenchActions(manager);
            }

        });
        menuMgr.setRemoveAllWhenShown(true);
        tree.setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);

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

/*
		if (selection.size() == 1 && selection.getFirstElement() instanceof RepositoryRoot) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		} */
		sub.add(newAction);
//        sub.add(newRemoteFolderAction);
	}
	
	/*
	 * @see WorkbenchPart#createPartControl
	 */
	public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        contentProvider = new RemoteContentProvider();
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new WorkbenchLabelProvider());
        getSite().setSelectionProvider(viewer);
        root = new AllRootsElement();
        viewer.setInput(root);
//      viewer.setSorter(new RepositorySorter());
        drillPart = new DrillDownAdapter(viewer);
        
        contributeActions(); 

        // F1 Help
        String helpID = getHelpContextId();
        if (helpID != null)
            WorkbenchHelp.setHelp(viewer.getControl(), helpID);
        initializeListeners();
		SVNUIPlugin.getPlugin().getRepositoryManager().addRepositoryListener(listener);
	}
	
    /**
     * initialize the listeners
     */
	protected void initializeListeners() {
        getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
		viewer.addSelectionChangedListener(removeRootAction);
        
        // when F5 is pressed, refresh this view
        viewer.getControl().addKeyListener(new KeyAdapter() {
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
        
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                handleDoubleClick(e);
            }
        });        
       
        // when DEL is pressed, we remove the selected repository
		viewer.getControl().addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent event) {
                if (event.character == SWT.DEL && event.stateMask == 0) {
                    removeRootAction.run();
                }
			}
			public void keyReleased(KeyEvent event) {
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
					name = res.getRepositoryRelativePath() + " " + ((ISVNRemoteFile)res).getRevision(); //$NON-NLS-1$
			}
            try {
			     return Policy.bind("RepositoriesView.ResourceInRepository", name, res.getRepository().getLocation()); //$NON-NLS-1$
            } catch (SVNException e) {
            }
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
        viewer.getControl().setFocus();
    }
    
    /**
     * Method getShell.
     * @return Shell
     */
    protected Shell getShell() {
        return viewer.getTree().getShell();
    }

    /**
     * Returns the viewer.
     * @return TreeViewer
     */
    protected TreeViewer getViewer() {
        return viewer;
    }

    /**
     * this is called whenever a new repository location is added for example
     * or when user wants to refresh
     */
    protected void refreshViewer() {
        if (viewer == null) return;
        viewer.refresh(); 
    }
    
    public void collapseAll() {
        if (viewer == null) return;
        viewer.getControl().setRedraw(false);       
        viewer.collapseToLevel(viewer.getInput(), TreeViewer.ALL_LEVELS);
        viewer.getControl().setRedraw(true);
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
                    viewer.setExpandedState(first, !viewer.getExpandedState(first));
                }
            }
        } 
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
//      SVNUIPlugin.getPlugin().getRepositoryManager().removeRepositoryListener(listener);
        super.dispose();
        viewer = null;
    }

}
