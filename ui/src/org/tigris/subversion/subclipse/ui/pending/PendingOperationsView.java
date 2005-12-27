/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.pending;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.CompareWithBaseRevisionAction;
import org.tigris.subversion.subclipse.ui.internal.Utils;

/**
 * 
 * The <code>PendingOperationsView</code> shows the pending operations
 * (added resources, deleted resources, modified resources ie all resources that need
 * commit)
 * 
 */
public class PendingOperationsView extends ViewPart implements IResourceStateChangeListener {
	public static final String VIEW_ID = "org.tigris.subversion.subclipse.ui.pending.PendingOperationsView"; //$NON-NLS-1$

	private Table table;
	private TableViewer tableViewer;
    private IContainer parent;
    private Action refreshAction;
    private Action toggleAddedAction;
    private Action toggleDeletedAction;
    private Action toggleModifiedAction;

    private IResource[] changedResources = null;
    private boolean shutdown = false;
    private ComputeStatusJob computeStatusJob = null;
    
    private Action compareWithBaseAction;
    
    private OpenActionGroup fOpenEditorGroup; 
    
    public PendingOperationsView() {
        SVNProviderPlugin.addResourceStateChangeListener(this);
    }
    
    /*
     *  (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
        if (fOpenEditorGroup != null)
            fOpenEditorGroup.dispose();
        shutdown = true;
        super.dispose();
        SVNProviderPlugin.removeResourceStateChangeListener(this);
    }

	class EditorsContentProvider implements IStructuredContentProvider {
		/**
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
				// Short-circuit to optimize
				if (changedResources != null) return changedResources;
				
				if(computeStatusJob == null) {
				    computeStatusJob = new ComputeStatusJob();
				}
				if(computeStatusJob.getState() != Job.NONE) {
				    computeStatusJob.cancel();
					try {
					    computeStatusJob.join();
					} catch (InterruptedException e) {
						SVNUIPlugin.log(new SVNException(Policy.bind("PendingOperationsView.errorComputingStatus", parent.getName()), e)); //$NON-NLS-1$
					}
				}
				computeStatusJob.setContainer( parent );
				Utils.schedule(computeStatusJob, getViewSite());

				return new IResource[0];
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(
			Viewer viewer,
			Object oldInput,
			Object newInput) {
		    changedResources = null;
		}

	}

	class EditorsLabelProvider implements ITableLabelProvider {
		private Image imgAddFile;
        private Image imgDeleteFile;
        private Image imgAddFolder;
        private Image imgDeleteFolder;
        private Image imgModifiedFile;
        private Image imgModifiedFolder;
        private Image imgMissing;
        
        public EditorsLabelProvider() {
            imgAddFile = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEADD_PENDING).createImage(false);
            imgDeleteFile = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEDELETE_PENDING).createImage(false);
            imgAddFolder = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FOLDERADD_PENDING).createImage(false);
            imgDeleteFolder = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FOLDERDELETE_PENDING).createImage(false);
            imgModifiedFile = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEMODIFIED_PENDING).createImage(false);
            imgModifiedFolder = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FOLDERMODIFIED_PENDING).createImage(false);
            imgMissing = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEMISSING_PENDING).createImage(false);
        }
        
        /**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
            if ((element == null) || (columnIndex != 0))
                return null;
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor((IResource)element); 
            
            LocalResourceStatus status = null; 
            try {
                status = svnResource.getStatus();
            } catch (SVNException e) {
                return null;
            }
            
            if ((!svnResource.isFolder()) && (status.isAdded()))
                return imgAddFile;
            else
            if ((svnResource.isFolder()) && (status.isAdded()))
                return imgAddFolder;
            else
            if ((!svnResource.isFolder()) && (status.isDeleted()))
                return imgDeleteFile;
            else
            if ((svnResource.isFolder()) && (status.isDeleted()))
                return imgDeleteFolder;
            else   
            if ((!svnResource.isFolder()) && (status.isTextModified()))
                return imgModifiedFile;
            else
            if ((svnResource.isFolder()) && (status.isTextModified()))
                return imgModifiedFolder;
            else
            if (status.isMissing()) 
            	return imgMissing;
            else
                return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			if (element == null)
				return ""; //$NON-NLS-1$
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor((IResource)element); 
            LocalResourceStatus status = null; 
            try {
                status = svnResource.getStatus();
            } catch (SVNException e) {
                return null;
            }

			String result = null;
			switch (columnIndex) {
				case 0 :
    				result = ""; //status.getPath(); //$NON-NLS-1$
					break;
                case 1 : // description
                    if (status.isCopied()) {
                        result = Policy.bind("PendingOperationsView.hasBeenCopied",svnResource.getName(), //$NON-NLS-1$
                            status.getUrlCopiedFrom().toString());
                    }    
                    else
                    if (status.isAdded())
                        result = Policy.bind("PendingOperationsView.hasBeenAdded",svnResource.getName()); //$NON-NLS-1$
                    else
                    if (status.isDeleted())
                        result = Policy.bind("PendingOperationsView.hasBeenDeleted",svnResource.getName()); //$NON-NLS-1$
                    else
                    if (status.isTextModified())
                        result = Policy.bind("PendingOperationsView.hasBeenModified",svnResource.getName()); //$NON-NLS-1$
                    else
                    if (status.isMissing())
                    	result = Policy.bind("PendingOperationsView.missing",svnResource.getName()); //$NON-NLS-1$
                    break;
				case 2 : // resource
					result = svnResource.getName();
					break;
				case 3 : // In Folder
                    IPath path = svnResource.getIResource().getFullPath();
					result = path.uptoSegment(path.segmentCount()-1).toString();
					break;
			}
			// This method must not return null
			if (result == null) result = ""; //$NON-NLS-1$
			return result;

		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
            imgAddFile.dispose();
            imgDeleteFile.dispose();
            imgAddFolder.dispose();
            imgDeleteFolder.dispose();
            imgModifiedFile.dispose();
            imgModifiedFolder.dispose();
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
		}

	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		table =	new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData);
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
        
 		tableViewer = new TableViewer(table);
		createColumns(table, layout);

		tableViewer.setContentProvider(new EditorsContentProvider());
		tableViewer.setLabelProvider(new EditorsLabelProvider());
        
        getViewSite().setSelectionProvider(tableViewer);
        
        contributeActions();
        
        initDragAndDrop();        
    }

    /**
     * Adds the action contributions for this view.
     */    
    public void contributeActions() {
        // Contribute actions to popup menu for the table
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager menuMgr) {
                fillTableMenu(menuMgr);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        tableViewer.getTable().setMenu(menu);
        getSite().registerContextMenu(menuMgr, tableViewer); 

        SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
        refreshAction = new Action(Policy.bind("PendingOperationsView.refreshLabel"), plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
            public void run() {
                refresh();
            }
        };
        refreshAction.setToolTipText(Policy.bind("PendingOperationsView.refresh")); //$NON-NLS-1$
        refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
        refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));

        // Create the local tool bar
        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
        tbm.add(refreshAction);
        tbm.update(false);

        // Toggle show added resources action
        final IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
        toggleAddedAction = new Action(Policy.bind("PendingOperationsView.showAddedResources")) { //$NON-NLS-1$
            public void run() {
                store.setValue(ISVNUIConstants.PREF_SHOW_ADDED_RESOURCES, toggleAddedAction.isChecked());
                refresh();                
            }
        };
        
        // we want the default to be true if it doesn't exist in store. getBoolean will return false as a default.
        if(store.contains(ISVNUIConstants.PREF_SHOW_ADDED_RESOURCES))
        	toggleAddedAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_SHOW_ADDED_RESOURCES));
        else
			toggleAddedAction.setChecked(true);
			
   
        // Toggle show deleted resources action
        toggleDeletedAction = new Action(Policy.bind("PendingOperationsView.showDeletedResources")) { //$NON-NLS-1$
            public void run() {
                store.setValue(ISVNUIConstants.PREF_SHOW_DELETED_RESOURCES, toggleAddedAction.isChecked());
                refresh();                
            }
        };

		if(store.contains(ISVNUIConstants.PREF_SHOW_DELETED_RESOURCES))
			toggleDeletedAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_SHOW_DELETED_RESOURCES));
		else
			toggleDeletedAction.setChecked(true);
        

        // Toggle show modified resources action
        toggleModifiedAction = new Action(Policy.bind("PendingOperationsView.showModifiedResources")) { //$NON-NLS-1$
            public void run() {
                store.setValue(ISVNUIConstants.PREF_SHOW_MODIFIED_RESOURCES, toggleModifiedAction.isChecked());
                refresh();                
            }
        };

		if(store.contains(ISVNUIConstants.PREF_SHOW_MODIFIED_RESOURCES))
			toggleModifiedAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_SHOW_MODIFIED_RESOURCES));
		else
			toggleModifiedAction.setChecked(true);

        
        
        // Contribute toggle text visible to the toolbar drop-down
        IActionBars actionBars = getViewSite().getActionBars();
        IMenuManager actionBarsMenu = actionBars.getMenuManager();
        actionBarsMenu.add(toggleAddedAction);
        actionBarsMenu.add(toggleDeletedAction);
        actionBarsMenu.add(toggleModifiedAction);                
        
		// set F1 help
        WorkbenchHelp.setHelp(tableViewer.getControl(), IHelpContextIds.PENDING_OPERATIONS_VIEW);

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                handleDoubleClick(e);
            }
        });
                

        fOpenEditorGroup= new OpenActionGroup(this);
	}

    /**
     * Adds drag and drop support to the pending operations view.
     */
    void initDragAndDrop() {
        int ops = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
        Transfer[] transfers = new Transfer[] {ResourceTransfer.getInstance()};
        tableViewer.addDropSupport(ops, transfers, new PendingDropAdapter(tableViewer, this));
    }

    /**
     * fill the popup menu for the table
     */
    private void fillTableMenu(IMenuManager manager) {
        manager.add(new GroupMarker("group.open")); //$NON-NLS-1$
        fOpenEditorGroup.setContext(new ActionContext(tableViewer.getSelection()));
        fOpenEditorGroup.fillContextMenu(manager);
        fOpenEditorGroup.setContext(null);
        manager.add(new Separator("additions")); //$NON-NLS-1$
        manager.add(refreshAction);
        manager.add(new Separator("additions-end")); //$NON-NLS-1$
    }
    
	/**
	 * Method createColumns.
	 * @param table
	 * @param layout
	 * @param viewer
	 */
	private void createColumns(Table table, TableLayout layout) {

		TableColumn col;
		// icon
		col = new TableColumn(table, SWT.NONE);
    	col.setResizable(false);
		layout.addColumnData(new ColumnPixelData(20, false));

		// description
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("PendingOperationsView.description")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(60, true));

		// resource
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("PendingOperationsView.resource")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(20, true));

		// in folder
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("PendingOperationsView.infolder")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(60, true));

	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
	}
	/**
	 * Method getTable.
	 */
	public Table getTable() {
		return table;
	}

    /**
     * refresh the view
     */
    public void refresh()  {
        changedResources = null;
        // Force a refresh of the parent to pick up any changes outside of
        // Eclipse and update their SVN status.
        try {
			parent.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
		}
        updateTable();
    }

    private void updateTable() {
        // updateTable is not always called from UI thread 
        getSite().getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                tableViewer.refresh();
            }
        });
    }
    
    /**
     * updates the changes resources
     * 
     * @param resources resources for which status changed
     * @throws SVNException
     */
    private void updateChangedResourcesWith(IResource[] resources) throws SVNException {
        Set set = new HashSet();
        if (this.changedResources != null) {
	        for (int i = 0; i < this.changedResources.length;i++) {
	            set.add(this.changedResources[i]);
	        }
        }
        for (int i = 0; i < resources.length;i++) {
            set.add(resources[i]);
        }
        this.changedResources = getChangedResources((IResource[])set.toArray(new IResource[0]));
    }
    
    public void resourceSyncInfoChanged(IResource[] changedResources) {
        // we don't refresh because it takes a long time
        try {
            updateChangedResourcesWith(changedResources);
        } catch (SVNException e) {
        }
        updateTable();
    }

    public void resourceModified(IResource[] changedResources) {
        // we don't refresh because it takes a long time
        try {
            updateChangedResourcesWith(changedResources);
        } catch (SVNException e) {
        }
        updateTable();
    }
    
    public void projectConfigured(IProject project) {}
    
    public void projectDeconfigured(IProject project) {}
    
    
    /**
     * Shows the pending operations for the given IContainer and its children in the view.
     */
    public void showPending(IContainer container) throws SVNException {
        parent = container;
        // Force a refresh of the parent to pick up any changes outside of
        // Eclipse and update their SVN status.
        try {
			parent.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
		}
        setPartName(Policy.bind("PendingOperationsView.titleWithArgument", container.getName())); //$NON-NLS-1$
        setContentDescription(Policy.bind("PendingOperationsView.titleWithArgument", container.getName())); //$NON-NLS-1$
        tableViewer.setInput(container);
    }

    /**
     * get the action for comparing with base revision
     * @return
     */
    private Action getCompareWithBaseAction() {
        if (compareWithBaseAction == null) {
            compareWithBaseAction = new Action("") { //$NON-NLS-1$
                public void run() {
                    CompareWithBaseRevisionAction delegate = new CompareWithBaseRevisionAction();
                    delegate.init(this);
                    delegate.selectionChanged(this,tableViewer.getSelection());
                    if (this.isEnabled()) {
                    	delegate.run(this);
                    }
                }
            };          
        }
        return compareWithBaseAction;
    }    
    
    /**
     * The mouse has been double-clicked in the table, open the file
     */
    private void handleDoubleClick(DoubleClickEvent e) {
        getCompareWithBaseAction().run();
    }

	private class ComputeStatusJob extends Job {
		public IContainer container;
		public ComputeStatusJob() {
			super(Policy.bind("PendingOperationsView.computeStatusJob"));  //$NON-NLS-1$;
		}
		public void setContainer(IContainer container) {
			this.container = container;
		}
		public IStatus run(IProgressMonitor monitor) {
			try {
				if(container != null && !shutdown) {
                    LocalResourceStatus[] status = getStatus( container );
					changedResources = getChangedResources( status ); 
					
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							if(changedResources != null && tableViewer != null && ! tableViewer.getTable().isDisposed()) {
							    // once we got the entries, we refresh the table 
                                tableViewer.refresh();
							}
						}
					});
				}
				return Status.OK_STATUS;
			} catch (TeamException e) {
				return e.getStatus();
			}
		}
	}

    /**
     * get the statuses (recursively) of resources in given container 
     * @param container
     * @return
     * @throws SVNException
     */
    private LocalResourceStatus[] getStatus(IContainer container) throws SVNException {
    	// can be a null parent if we have the view open before we select anything
    	if(container == null)
    		return null;
        GetStatusCommand command = new GetStatusCommand(SVNWorkspaceRoot.getSVNResourceFor(container), true, true);
        command.run(null);
        return command.getLocalResourceStatuses();
    }

    
    /**
     * filters the given resources depending on the settings of the view
     * @param resources
     * @return
     * @throws SVNException
     */
    private IResource[] getChangedResources(IResource[] resources) throws SVNException {
        LocalResourceStatus[] statuses = new LocalResourceStatus[resources.length];
        for (int i = 0; i < resources.length;i++) {
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
            statuses[i] = svnResource.getStatus();
        }
        return getChangedResources(statuses);
    }
    
    /**
     * filters the given statuses depending on the settings of the view and returns 
     * the corresponding resources
     * 
     * @param status
     * @return
     */
    private IResource[] getChangedResources(LocalResourceStatus[] status) {
        if( status == null ) return null;
        Set resourceSet = new HashSet();
        for (int i = 0; i < status.length;i++) {
            if ( ((status[i].isAdded()) && (toggleAddedAction.isChecked())) ||
            		status[i].isMissing() ||
                    ((status[i].isDeleted()) && (toggleDeletedAction.isChecked())) ||
                    ((status[i].isTextModified()) && (toggleModifiedAction.isChecked())) ) {
                
            	try {
            		resourceSet.add(status[i].getResource());
        		} catch (SVNException e) {
        			SVNProviderPlugin.log(e);
        			return null;
        		}
            }
        }
        return (IResource[]) resourceSet.toArray(new IResource[0]);
    }
}
