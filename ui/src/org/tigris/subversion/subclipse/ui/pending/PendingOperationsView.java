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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.javahl.SVNClientAdapter;

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

    public PendingOperationsView() {
        SVNProviderPlugin.addResourceStateChangeListener(this);
    }
    
    /*
     *  (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
        super.dispose();
        SVNProviderPlugin.removeResourceStateChangeListener(this);
    }

	class EditorsContentProvider implements IStructuredContentProvider {

		/**
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
            Status[] status = (Status[]) inputElement;
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
                    
            List resourceList = new ArrayList();
            for (int i = 0; i < status.length;i++) {
                if ( ((status[i].isAdded()) && (toggleAddedAction.isChecked())) ||
                     ((status[i].isDeleted()) && (toggleDeletedAction.isChecked())) ||
                     ((status[i].isModified()) && (toggleModifiedAction.isChecked())) ) {

                    // path is sometimes absolute, sometimes relative.
                    // here we make sure it is absolute
                    IPath pathEclipse = null;
                    try {
                        pathEclipse = new Path( (new Path(status[i].getPath())).toFile().getCanonicalPath());
                    } catch (IOException e)
                    {
                        break; // should never occur ...
                    }
        
                    IResource resource = null;
                    if (status[i].getNodeKind()  == NodeKind.dir)       
                        resource = workspaceRoot.getContainerForLocation(pathEclipse);
                    else
                    if (status[i].getNodeKind() == NodeKind.file)
                        resource =  workspaceRoot.getFileForLocation(pathEclipse);
                
                    resourceList.add(resource);
                }
            }
            IResource[] resourceArray = new IResource[resourceList.size()];
            return (IResource[]) resourceList.toArray(resourceArray);
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
		}

	}

	class EditorsLabelProvider implements ITableLabelProvider {
		private Image imgAddFile;
        private Image imgDeleteFile;
        private Image imgAddFolder;
        private Image imgDeleteFolder;
        private Image imgModifiedFile;
        private Image imgModifiedFolder;
        
        public EditorsLabelProvider() {
            imgAddFile = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEADD_PENDING).createImage(false);
            imgDeleteFile = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEDELETE_PENDING).createImage(false);
            imgAddFolder = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FOLDERADD_PENDING).createImage(false);
            imgDeleteFolder = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FOLDERDELETE_PENDING).createImage(false);
            imgModifiedFile = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILEMODIFIED_PENDING).createImage(false);
            imgModifiedFolder = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FOLDERMODIFIED_PENDING).createImage(false);
        }
        
        /**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
            if ((element == null) || (columnIndex != 0))
                return null;
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor((IResource)element); 
            
            ISVNStatus status = null; 
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
            if ((!svnResource.isFolder()) && (status.isModified()))
                return imgModifiedFile;
            else
            if ((svnResource.isFolder()) && (status.isModified()))
                return imgModifiedFolder;
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
            ISVNStatus status = null; 
            try {
                status = svnResource.getStatus();
            } catch (SVNException e) {
                return null;
            }

			String result = null;
			switch (columnIndex) {
				case 0 :
    				result = ""; //status.getPath();
					break;
                case 1 : // description
                    if (status.isCopied()) {
                        result = Policy.bind("PendingOperationsView.hasBeenCopied",svnResource.getName(),
                            status.getUrlCopiedFrom());
                    }    
                    else
                    if (status.isAdded())
                        result = Policy.bind("PendingOperationsView.hasBeenAdded",svnResource.getName());
                    else
                    if (status.isDeleted())
                        result = Policy.bind("PendingOperationsView.hasBeenDeleted",svnResource.getName());
                    else
                    if (status.isModified())
                        result = Policy.bind("PendingOperationsView.hasBeenModified",svnResource.getName());
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
        toggleAddedAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_SHOW_ADDED_RESOURCES));
//        WorkbenchHelp.setHelp(toggleTextAction, IHelpContextIds.SHOW_COMMENT_IN_HISTORY_ACTION);    
    
        // Toggle show deleted resources action
        toggleDeletedAction = new Action(Policy.bind("PendingOperationsView.showDeletedResources")) { //$NON-NLS-1$
            public void run() {
                store.setValue(ISVNUIConstants.PREF_SHOW_DELETED_RESOURCES, toggleAddedAction.isChecked());
                refresh();                
            }
        };
        toggleDeletedAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_SHOW_DELETED_RESOURCES));
//        WorkbenchHelp.setHelp(toggleTextAction, IHelpContextIds.SHOW_COMMENT_IN_HISTORY_ACTION);    

        // Toggle show modified resources action
        toggleModifiedAction = new Action(Policy.bind("PendingOperationsView.showModifiedResources")) { //$NON-NLS-1$
            public void run() {
                store.setValue(ISVNUIConstants.PREF_SHOW_MODIFIED_RESOURCES, toggleModifiedAction.isChecked());
                refresh();                
            }
        };
        toggleModifiedAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_SHOW_MODIFIED_RESOURCES));
//        WorkbenchHelp.setHelp(toggleTextAction, IHelpContextIds.SHOW_COMMENT_IN_HISTORY_ACTION);
        
        // Contribute toggle text visible to the toolbar drop-down
        IActionBars actionBars = getViewSite().getActionBars();
        IMenuManager actionBarsMenu = actionBars.getMenuManager();
        actionBarsMenu.add(toggleAddedAction);
        actionBarsMenu.add(toggleDeletedAction);
        actionBarsMenu.add(toggleModifiedAction);                
        
		// set F1 help
//		WorkbenchHelp.setHelp(tableViewer.getControl(), IHelpContextIds.CVS_EDITORS_VIEW);

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                handleDoubleClick(e);
            }
        }); 

	}

    /**
     * Adds drag and drop support to the pending operations view.
     */
    void initDragAndDrop() {
        int ops = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
        Transfer[] transfers = new Transfer[] {ResourceTransfer.getInstance()};
        tableViewer.addDropSupport(ops, transfers, new PendingDropAdapter(tableViewer, this));
    }
    
	public void setInput(Status[] status) {
		tableViewer.setInput(status);
	}

    /**
     * fill the popup menu for the table
     */
    private void fillTableMenu(IMenuManager manager) {
        // file actions go first (view file)
        manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
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

        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                try {
                    tableViewer.setInput(getStatus());
                    tableViewer.refresh();
                } catch (SVNException e) {
                    // silently ignore exception
                }
            }
        });
    }

    public void resourceSyncInfoChanged(IResource[] changedResources) {
        refresh();
    }

    public void resourceModified(IResource[] changedResources) {}
    
    public void projectConfigured(IProject project) {}
    
    public void projectDeconfigured(IProject project) {}
    
    
    /**
     * Shows the pending operations for the given IContainer and its children in the view.
     */
    public void showPending(IContainer container) throws SVNException {
        parent = container;
        setTitle(Policy.bind("PendingOperationsView.titleWithArgument", container.getName())); //$NON-NLS-1$
        tableViewer.setInput(getStatus());
    }

    private ISVNStatus[] getStatus() throws SVNException {
        ISVNStatus[] status = null;
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
        SVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
        try { 
            status = svnClient.getStatusRecursively(parent.getLocation().toFile(),false);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        }
        return status;
    }

    /**
     * The mouse has been double-clicked in the table, open the file
     */
    private void handleDoubleClick(DoubleClickEvent e) {
        // Only act on single selection
        ISelection selection = e.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection)selection;
            if (structured.size() == 1) {
                Object first = structured.getFirstElement();
                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor((IResource)first);
                try {
                    // don't open file for directory or deleted files                 
                    if ((!svnResource.getStatus().isDeleted()) && (!svnResource.isFolder())) {                
                        IWorkbench workbench = SVNUIPlugin.getPlugin().getWorkbench();
                        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
                        page.openEditor((IFile)svnResource.getIResource());
                    }
                } catch (SVNException ex) {
                    SVNUIPlugin.log(ex);
                } catch (PartInitException ex) {
                    SVNUIPlugin.log(SVNException.wrapException(ex));
                }
            }
        } 
    }

}
