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
package org.tigris.subversion.subclipse.ui.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.PluginTransferData;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.part.WorkbenchPart;
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
import org.tigris.subversion.subclipse.ui.actions.RemoteResourceTransfer;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;
import org.tigris.subversion.subclipse.ui.repository.model.AllRootsElement;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;
import org.tigris.subversion.subclipse.ui.wizards.NewLocationWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardNewRepositoryPage;

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
    private Action refreshPopupAction;
    private Action collapseAllAction;
    private OpenRemoteFileAction openAction;
    private Action propertiesAction;
    
    private RemoteContentProvider contentProvider;
    // this listener is used when a repository is added, removed or changed
	private IRepositoryListener repositoryListener = new IRepositoryListener() {
		public void repositoryAdded(final ISVNRepositoryLocation root) {
			getViewer().getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					refreshViewer(null, false);
					getViewer().setSelection(new StructuredSelection(root));
				}
			});
		}
		public void repositoryRemoved(ISVNRepositoryLocation root) {
			refresh(null, false);
		}
		public void repositoriesChanged(ISVNRepositoryLocation[] roots) {
			refresh(null, false);
		}
        public void remoteResourceDeleted(ISVNRemoteResource resource) {
            refresh(resource.getParent(), false);
        }
        public void remoteResourceCreated(ISVNRemoteFolder parent,String resourceName) {
            refresh(parent, true);  
        }
        public void remoteResourceCopied(ISVNRemoteResource source,ISVNRemoteFolder destination) {
            refresh(destination, false);  
        }
        public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder,String destinationResourceName) {
            refresh(resource.getParent(), false);
            refresh(destinationFolder, false);
        }
		private void refresh(Object object, boolean refreshRepositoriesFolders) {
			final Object finalObject = object;
            final boolean finalRefreshReposFolders = refreshRepositoriesFolders;
			Display display = getViewer().getControl().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					RepositoriesView.this.refreshViewer(finalObject, finalRefreshReposFolders);
				}
			});
		}
		public void repositoryModified(ISVNRepositoryLocation root) {
			refresh(null, false);
		}
	};


    private static final class RepositoryDragSourceListener implements DragSourceListener {
      private IStructuredSelection selection;

      public void dragStart(DragSourceEvent event) {
          if(selection!=null) {
              final Object[] array = selection.toArray();
              // event.doit = Utils.getResources(array).length > 0;
              for (int i = 0; i < array.length; i++) {
                  if (array[i] instanceof ISVNRemoteResource) {
                      event.doit = true;
                      return;
                  }
              }
              event.doit = false;
          }
      }

      public void dragSetData(DragSourceEvent event) {
          if (selection!=null && RemoteResourceTransfer.getInstance().isSupportedType(event.dataType)) {
              final Object[] array = selection.toArray();
              for (int i = 0; i < array.length; i++) {
                  if (array[i] instanceof ISVNRemoteResource) {
                      event.data = array[i];
                      return;
                  }
              }
          } else if (PluginTransfer.getInstance().isSupportedType(event.dataType)) {
            final Object[] array = selection.toArray();
            for (int i = 0; i < array.length; i++) {
                if (array[i] instanceof ISVNRemoteResource) {
                    event.data = new PluginTransferData("org.tigris.subversion.subclipse.ui.svnRemoteDrop", RemoteResourceTransfer.getInstance().toByteArray((ISVNRemoteResource) array[i])); //$NON-NLS-1$
                    return;
                }
            }
           
        } 
      }

      public void dragFinished( DragSourceEvent event) {
      }

      public void updateSelection( IStructuredSelection selection) {
          this.selection = selection;
      }
    }


    RepositoryDragSourceListener repositoryDragSourceListener;


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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newAction, IHelpContextIds.NEW_REPOSITORY_LOCATION_ACTION);
		
		// Properties
        propertiesAction = new PropertyDialogAction(new SameShellProvider(shell), getViewer());
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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(removeRootAction, IHelpContextIds.REMOVE_REPOSITORY_LOCATION_ACTION);
		
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), removeRootAction);
		
        // Refresh action (toolbar)
        SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
        refreshAction = new Action(Policy.bind("RepositoriesView.refresh"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
            public void run() {
            	refreshViewer(null, true);
            }
        };
        refreshAction.setToolTipText(Policy.bind("RepositoriesView.refreshTooltip")); //$NON-NLS-1$
        refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
        refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
        getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

        refreshPopupAction = new Action(Policy.bind("RepositoriesView.refreshPopup"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH)) { //$NON-NLS-1$
            public void run() {
            	refreshViewerNode();
            }
        };
        
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
        
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager actionBarsMenu = actionBars.getMenuManager();
		Action newRepositoryAction = new Action(Policy.bind("RepositoriesView.newRepository")) { //$NON-NLS-1$
			public void run() {
				SvnWizardNewRepositoryPage newRepositoryPage = new SvnWizardNewRepositoryPage();
				SvnWizard wizard = new SvnWizard(newRepositoryPage);
				SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
				if (dialog.open() == SvnWizardDialog.OK) refreshViewer(null, false);
			}			
		};
		actionBarsMenu.add(newRepositoryAction);
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
        manager.add(new Separator("historyGroup")); //$NON-NLS-1$
        manager.add(new Separator("checkoutGroup")); //$NON-NLS-1$
        manager.add(new Separator("exportImportGroup")); //$NON-NLS-1$
        manager.add(new Separator("miscGroup")); //$NON-NLS-1$
        
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(refreshPopupAction);

	
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
        	PlatformUI.getWorkbench().getHelpSystem().setHelp(treeViewer.getControl(), helpID);
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

        repositoryDragSourceListener = new RepositoryDragSourceListener();
        treeViewer.addDragSupport( DND.DROP_LINK | DND.DROP_DEFAULT,
                new Transfer[] { RemoteResourceTransfer.getInstance(), PluginTransfer.getInstance()},
                repositoryDragSourceListener);
        
        treeViewer.addSelectionChangedListener( new ISelectionChangedListener() {
          public void selectionChanged( SelectionChangedEvent event) {
            repositoryDragSourceListener.updateSelection( (IStructuredSelection) event.getSelection());
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
    protected void refreshViewer(Object object, boolean refreshRepositoriesFolders) {
        if (treeViewer == null) return;
        if (refreshRepositoriesFolders) {
        	IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                	SVNProviderPlugin.getPlugin().getRepositories().refreshRepositoriesFolders(monitor);
				}
        	};
            try {
				new ProgressMonitorDialog(getShell()).run(true, false, runnable);
			} catch (Exception e) {
	            SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
			}
        }
        if (object == null) treeViewer.refresh();
        else treeViewer.refresh(object); 
    }
    
    protected void refreshViewerNode() {
    	IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
        	Object object = iter.next();
        	if (object instanceof ISVNRepositoryLocation) {
        		refreshAction.run();
        		break;
        	}
        	if (object instanceof ISVNRemoteFolder) ((ISVNRemoteFolder)object).refresh();
        	treeViewer.refresh(object); 
        }
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
