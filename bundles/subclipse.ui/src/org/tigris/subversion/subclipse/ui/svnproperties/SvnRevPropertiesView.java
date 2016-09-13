/*******************************************************************************
 * Copyright (c) 2004, 2010 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/

package org.tigris.subversion.subclipse.ui.svnproperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.PropertyConflict;
import org.tigris.subversion.subclipse.ui.settings.UnversionedCustomProperty;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;



public class SvnRevPropertiesView extends ViewPart {
  public static final String VIEW_ID = "org.tigris.subversion.subclipse.ui.svnproperties.SvnRevPropertiesView"; //$NON-NLS-1$
  
  private TableViewer tableViewer;
  private TextViewer textViewer;
  private ISVNLocalResource resource;
  private ISVNRemoteResource remoteResource;
  private Action refreshAction;
  private Label statusLabel;
//  private ISelectionListener pageSelectionListener;
//  private ISelectionChangedListener historyRevisionListener;
//  private IResourceStateChangeListener resourceStateChangeListener;
  
  private PropertyConflict[] conflicts;  


  class ResourceStateChangeListener implements IResourceStateChangeListener {
    /**
     * the svn status of some resources changed. Refresh if we are concerned
     */
    public void resourceSyncInfoChanged(IResource[] changedResources) {
        for (int i = 0; i < changedResources.length;i++) {
            if (resource != null && changedResources[i].equals(resource.getIResource())) {
                refresh();
            }
        }
    }

    public void resourceModified(IResource[] changedResources) {}

    public void projectConfigured(IProject project) {}

    public void projectDeconfigured(IProject project) {}
    
    public void initialize() {}
}  
  
  public SvnRevPropertiesView() {
  }
  
  /*
   *  (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPart#dispose()
   */
//  public void dispose() {
//      SVNProviderPlugin.removeResourceStateChangeListener(resourceStateChangeListener);
//      resourceStateChangeListener = null;
//      getSite().getPage().removePostSelectionListener(pageSelectionListener);
//      if (historyRevisionListener != null) {
//    
//      }
//      super.dispose();
//  }

  class PropertiesLabelProvider implements ITableLabelProvider {
    
    public PropertiesLabelProvider() {
    }
    
    /**
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
        if (columnIndex == 0 && element != null && conflicts != null) {
            ISVNProperty svnProperty = (ISVNProperty)element; 
            for (int i = 0; i < conflicts.length; i++) {
                if (conflicts[i].getPropertyName().equals(svnProperty.getName())) {
                    return SVNUIPlugin.getImage(ISVNUIConstants.IMG_PROPERTY_CONFLICTED);                       
                }
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    public String getColumnText(Object element, int columnIndex) {
        if (element == null)
            return ""; //$NON-NLS-1$
        ISVNProperty svnProperty = (ISVNProperty)element; 
        
        String result = null;
        switch (columnIndex) {
            case 1 :
                result = svnProperty.getName();
                break;
            case 2 : 
                result = svnProperty.getValue();
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
  

  private TableViewer createTable(Composite parent) {
      Table table =   new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
      table.setHeaderVisible(true);
      table.setLinesVisible(true);

      GridData gridData = new GridData(GridData.FILL_BOTH);
      table.setLayoutData(gridData);
      TableLayout layout = new TableLayout();
      table.setLayout(layout);
      
      tableViewer = new TableViewer(table);
      createColumns(table, layout);

      tableViewer.setContentProvider(new ArrayContentProvider());
      tableViewer.setLabelProvider(new PropertiesLabelProvider());
      return tableViewer;
  }

  /**
   * Create the TextViewer 
   */
  protected TextViewer createText(Composite parent) {
      return new TextViewer(parent, SWT.V_SCROLL | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
  }
  
  /**
   * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  public void createPartControl(Composite parent) {
      GridLayout layout = new GridLayout();
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.numColumns = 1;
      parent.setLayout(layout);       
      statusLabel = new Label(parent,SWT.LEFT);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.grabExcessHorizontalSpace = true;
      statusLabel.setLayoutData(gridData);

      SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
      sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
      tableViewer = createTable(sashForm);
      textViewer = createText(sashForm);
      sashForm.setWeights(new int[] { 70, 30 });

      contributeActions();        
      
//      pageSelectionListener = new ISelectionListener() {
//          public void selectionChanged(IWorkbenchPart part, ISelection selection) {
//              handlePartSelectionChanged(part,selection);
//          }
//      };
//      
//      getSite().getPage().addPostSelectionListener(pageSelectionListener);
//      resourceStateChangeListener = new ResourceStateChangeListener();
//      SVNProviderPlugin.addResourceStateChangeListener(resourceStateChangeListener);
      
  }
  
  /**
   * called when the selection changed on another part  
   */
//  private void handlePartSelectionChanged(IWorkbenchPart part, ISelection selection) {
//      if (!(selection instanceof IStructuredSelection))
//          return;
//      
//      try {
//          Object first = ((IStructuredSelection)selection).getFirstElement(); 
//
//          if (first instanceof IAdaptable) {
//              IAdaptable a = (IAdaptable) first;
//              Object adapter = a.getAdapter(IResource.class);
//              if (adapter instanceof IResource) {
//                  IResource resource = (IResource)adapter;
//                  
//                  // If the resource isn't open or doesn't exist it won't have properties
//                  if (!resource.isAccessible()) {
//                      clearSvnProperties();
//                      refresh();
//                  } else {
//                      ISVNLocalResource svnResource = (ISVNLocalResource)resource.getAdapter(ISVNLocalResource.class);
//                      showSvnProperties(svnResource);
//                      refresh();
//                  }
//              }
//          }
//      } catch (SVNException e) {
//      }   
//  }  
  
  private Action getRefreshAction() {
    if (refreshAction == null) {
        SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
        refreshAction = new Action(Policy.bind("SvnPropertiesView.refreshLabel"), plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
            public void run() {
                try {
                  if (resource != null)
                    showSvnProperties(resource);
                  else if (remoteResource != null)
                    showSvnProperties(remoteResource);
                } catch (SVNException e) {; } // eat it
                refresh();
            }
        };
        refreshAction.setToolTipText(Policy.bind("SvnPropertiesView.refresh")); //$NON-NLS-1$
        refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
        refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
    }
    return refreshAction;
}

// TODO: Add, modify and Delete actions, this rev is just view only for now  
  
/**
 * Adds the action contributions for this view.
 */    
public void contributeActions() {
    // Contribute actions to popup menu for the table
//    MenuManager menuMgr = new MenuManager();
//    Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
//    menuMgr.addMenuListener(new IMenuListener() {
//        public void menuAboutToShow(IMenuManager menuMgr) {
//            fillTableMenu(menuMgr);
//        }
//    });
//    menuMgr.setRemoveAllWhenShown(true);
//    tableViewer.getTable().setMenu(menu);
//    getSite().registerContextMenu(menuMgr, tableViewer); 

    // Create the local tool bar
    IActionBars actionBars = getViewSite().getActionBars();
    IToolBarManager tbm = actionBars.getToolBarManager();
    tbm.add(getRefreshAction());
    tbm.update(false);
    
    // set F1 help
    PlatformUI.getWorkbench().getHelpSystem().setHelp(tableViewer.getControl(), IHelpContextIds.REV_PROPERTIES_VIEW);

    // set the selectionchanged listener for the table
    // updates property value when selection changes
    tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
            ISelection selection = event.getSelection();
            if (selection == null || !(selection instanceof IStructuredSelection)) {
                textViewer.setDocument(new Document("")); //$NON-NLS-1$
                return;
            }
            IStructuredSelection ss = (IStructuredSelection)selection;
            if (ss.size() != 1) {
                textViewer.setDocument(new Document("")); //$NON-NLS-1$
                return;
            }
            ISVNProperty property = (ISVNProperty)ss.getFirstElement();
            textViewer.setDocument(new Document(property.getValue()));
        }
    });
}

/**
 * fill the popup menu for the table
 */
//private void fillTableMenu(IMenuManager manager) {
//    manager.add(getRefreshAction());
// }

/**
 * Method createColumns.
 * @param table
 * @param layout
 * @param viewer
 */
private void createColumns(Table table, TableLayout layout) {

    TableColumn col;
    
    // name
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(false);
    layout.addColumnData(new ColumnWeightData(1, true));

    // name
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(true);
    col.setText(Policy.bind("SvnPropertiesView.propertyName")); //$NON-NLS-1$
    layout.addColumnData(new ColumnWeightData(60, true));

    // value
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(true);
    col.setText(Policy.bind("SvnPropertiesView.propertyValue")); //$NON-NLS-1$
    layout.addColumnData(new ColumnWeightData(120, true));
}

/**
 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
 */
public void setFocus() {
    tableViewer.getControl().setFocus();
}

/**
 * refresh the view
 */
public void refresh()  {

    Display.getDefault().syncExec(new Runnable() {
        public void run() {
            try {
                ISVNProperty[] props = getSvnRevProperties();
                tableViewer.setInput(props);
                tableViewer.refresh();
            } catch (SVNException e) {
                // silently ignore exception
            }
        }
    });
}

/**
 * update the status text
 *
 */
private void updateStatus() {
    conflicts = null;
    if (resource == null && remoteResource == null) {
        statusLabel.setText(""); //$NON-NLS-1$
        return;
    }
    if (resource != null) {
    try {
        LocalResourceStatus status = resource.getStatus();
        if (!resource.isManaged()) {
            statusLabel.setText(Policy.bind("SvnPropertiesView.resourceNotManaged")); //$NON-NLS-1$
        } else 
        if (status.getPropStatus().equals(SVNStatusKind.MODIFIED))
        {
            statusLabel.setText(Policy.bind("SvnPropertiesView.somePropertiesModified")); //$NON-NLS-1$
        } else
        if (status.getPropStatus().equals(SVNStatusKind.NORMAL)) {
            statusLabel.setText(Policy.bind("SvnPropertiesView.noPropertiesModified")); //$NON-NLS-1$
        } else
        if (status.getPropStatus().equals(SVNStatusKind.CONFLICTED))
        {
            statusLabel.setText(Policy.bind("SvnPropertiesView.conflictOnProperties")); //$NON-NLS-1$
            try {
                conflicts = PropertyConflict.getPropertyConflicts(resource);
            } catch (Exception e) {}
        } else {
            statusLabel.setText(""); //$NON-NLS-1$
        }
    } catch (SVNException e) {
        statusLabel.setText(Policy.bind("SvnPropertiesView.errorGettingStatus")); //$NON-NLS-1$
    }
    }
}

public void clearSvnProperties() {
  setContentDescription("");
  updateStatus();
}
    
  /**
   * Shows the properties for the given resource 
   */
  public void showSvnProperties(ISVNLocalResource resource) throws SVNException {
      this.resource = resource;
      this.remoteResource = null;
      
      if (resource != null) {
          if (resource.isManaged()) {
              if (resource.getRevision() != null) {
                setContentDescription(Policy.bind("SvnRevPropertiesView.titleWithTwoArguments", resource.getRevision().toString(), resource.getName())); //$NON-NLS-1$
              }
          } else {
              setContentDescription(Policy.bind("SvnRevPropertiesView.titleWithOneArgument", resource.getName())); //$NON-NLS-1$
          }
      } else {
          setContentDescription(""); //$NON-NLS-1$
      }
      updateStatus();
  }  
  
  /**
   * Shows the properties for the given resource 
   */
  public void showSvnProperties(ISVNRemoteResource resource) throws SVNException {
      this.remoteResource = resource;
      this.resource = null;
      if (remoteResource != null) {
        if (remoteResource.getRevision() != null) {
          setContentDescription(Policy.bind("SvnRevPropertiesView.titleWithTwoArguments", remoteResource.getRevision().toString(), remoteResource.getName())); //$NON-NLS-1$
        }
      } else {
          setContentDescription(""); //$NON-NLS-1$
      }
      updateStatus();
  }  
  
  private ISVNProperty[] getSvnRevProperties() throws SVNException {
    
    if(resource != null && resource.isManaged()) {
      SVNRevision rev = resource.getRevision();
      ISVNRemoteResource remoteResource = resource.getRemoteResource(rev);
      return UnversionedCustomProperty.getSvnRevisionProperties(remoteResource.getUrl(), rev, null);
    }
    
    if (remoteResource != null) {
      SVNRevision rev = remoteResource.getRevision();
      return UnversionedCustomProperty.getSvnRevisionProperties(remoteResource.getUrl(), rev, null);
    }
    
    return null;
  }
  
}
