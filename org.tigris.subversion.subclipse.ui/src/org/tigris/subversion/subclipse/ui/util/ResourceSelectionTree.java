package org.tigris.subversion.subclipse.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusUtil;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class ResourceSelectionTree extends Composite {
	private Tree tree;
	private int mode;
	private IResource[] resources;
	private ArrayList resourceList;
	private Set unversionedResourceList;
	private IContainer[] compressedFolders;
	private IContainer[] folders;
	private ArrayList folderList;
	private IContainer[] rootFolders;
	private ArrayList compressedFolderList;
	private TreeViewer treeViewer;
	private LabelProvider labelProvider;
	private String label;
	private Action treeAction;
	private Action flatAction;
	private Action compressedAction;
	private IDialogSettings settings;
	private HashMap statusMap;
	private ResourceComparator comparator = new ResourceComparator();
	private boolean checkbox;
	private IToolbarControlCreator toolbarControlCreator;
	private IRemoveFromViewValidator removeFromViewValidator;
	private SyncInfoSet syncInfoSet;
	private boolean showRemoveFromViewAction = true;
	private ResourceSelectionTreeDecorator resourceSelectionTreeDecorator = new ResourceSelectionTreeDecorator();
	private boolean resourceRemoved = false;
	private boolean includeUnversioned = true;
	private ResourceSelectionContentProvider resourceSelectionContentProvider = new ResourceSelectionContentProvider();
	private Action[] customOptions;
	
	public final static String MODE_SETTING = "ResourceSelectionTree.mode"; //$NON-NLS-1$
	public final static int MODE_COMPRESSED_FOLDERS = 0;
	public final static int MODE_FLAT = 1;
	public final static int MODE_TREE = 2;
	
	private final static int SPACEBAR = 32;

	public ResourceSelectionTree(Composite parent, int style, String label, IResource[] resources, HashMap statusMap, LabelProvider labelProvider, boolean checkbox, IToolbarControlCreator toolbarControlCreator, SyncInfoSet syncInfoSet) {
		super(parent, style);
		this.label = label;
		this.resources = resources;
		this.statusMap = statusMap;
		this.labelProvider = labelProvider;
		this.checkbox = checkbox;
		this.toolbarControlCreator = toolbarControlCreator;
		this.syncInfoSet = syncInfoSet;
		this.settings = SVNUIPlugin.getPlugin().getDialogSettings();
		if(resources!=null) 
		{
	  		Arrays.sort(resources, comparator);
	  		resourceList = new ArrayList();
	  		for (int i = 0; i < resources.length; i++) 
	  		{
		        IResource resource = resources[i];
				resourceList.add(resource);
	  		}
	  		unversionedResourceList = new HashSet();
	  		try
	  		{
		  		for (int i = 0; i < resources.length; i++) 
		  		{
		  	        IResource resource = resources[i];
			        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			        if(resource.exists() && !svnResource.getStatus().isManaged() )
			        {
			        	unversionedResourceList.add(resource);
			        }
				}
	  		}
	  		catch(Exception e)
	  		{
	  			SVNUIPlugin.openError(getShell(), null, null, e);
	  		}
  		}
		createControls();
	}
	
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	public IResource[] getSelectedResources() {
		if (!checkbox) return resources;
		ArrayList selected = new ArrayList();	
		Object[] checkedResources = ((CheckboxTreeViewer)treeViewer).getCheckedElements();
		for (int i = 0; i < checkedResources.length; i++) {
			if (resourceList.contains(checkedResources[i]))
				selected.add(checkedResources[i]);
		}
		IResource[] selectedResources = new IResource[selected.size()];
		selected.toArray(selectedResources);
		return selectedResources;
	}

	public void setCustomOptions(Action[] customOptions) {
		this.customOptions = customOptions;
	}

	private void createControls() {
		setLayout(new GridLayout(2, false));
		setLayoutData(new GridData(GridData.FILL_BOTH));

		ViewForm viewerPane = new ViewForm(this, SWT.BORDER | SWT.FLAT);
		viewerPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
//		Composite treeGroup = new Composite(viewerPane, SWT.NONE);
//		
//		GridLayout treeLayout = new GridLayout();
//		treeLayout.marginWidth = 0;
//		treeLayout.verticalSpacing = 1;
//		treeLayout.horizontalSpacing = 0;
//		treeLayout.numColumns = 1;
//		treeLayout.marginHeight = 0;
//		treeGroup.setLayout(treeLayout);
//		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
//		treeGroup.setLayoutData(gridData);	
		
//		Composite toolbarGroup = new Composite(treeGroup, SWT.NONE);
//		GridLayout toolbarGroupLayout = new GridLayout();
//		toolbarGroupLayout.numColumns = 2;
//		toolbarGroupLayout.marginWidth = 0;
//		toolbarGroupLayout.marginHeight = 0;
//		toolbarGroup.setLayout(toolbarGroupLayout);
//		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
//		toolbarGroup.setLayoutData(gridData);	

    CLabel toolbarLabel = new CLabel(viewerPane, SWT.NONE) {
      public Point computeSize(int wHint, int hHint, boolean changed) {
        return super.computeSize(wHint, Math.max(24, hHint), changed);
      }
    };
		
//		Label toolbarLabel = new Label(viewerPane, SWT.NONE);
//		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
//		gridData.horizontalIndent = 3;
//		gridData.horizontalAlignment = SWT.BEGINNING;
//		gridData.verticalAlignment = SWT.CENTER;
//		toolbarLabel.setLayoutData(gridData);
		if (label != null) {
		  toolbarLabel.setText(label);
		}
    viewerPane.setTopLeft(toolbarLabel);
		
		int buttonGroupColumns = 1;
		if (toolbarControlCreator != null) {
		  buttonGroupColumns = buttonGroupColumns + toolbarControlCreator.getControlCount();
		}

//		Composite buttonGroup = new Composite(toolbarGroup, SWT.NONE);
//		GridLayout buttonLayout = new GridLayout();
//		buttonLayout.numColumns = buttonGroupColumns;
//		buttonLayout.marginHeight = 0;
//		buttonLayout.marginWidth = 0;
//		buttonGroup.setLayout(buttonLayout);
//		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
//		buttonGroup.setLayoutData(gridData);
		
		ToolBar toolbar = new ToolBar(viewerPane, SWT.FLAT);
//		GridLayout toolbarLayout = new GridLayout();
//		toolbarLayout.numColumns = 3;
//		toolbar.setLayout(toolbarLayout);
//		toolbar.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		viewerPane.setTopCenter(toolbar);

		ToolBarManager toolbarManager = new ToolBarManager(toolbar);
		
		if (toolbarControlCreator != null) {
		  toolbarControlCreator.createToolbarControls(toolbarManager);
		  toolbarManager.add(new Separator());
		}

		flatAction = new Action(Policy.bind("ResourceSelectionTree.flat"), Action.AS_RADIO_BUTTON) {  //$NON-NLS-1$
			public void run() {
				mode = MODE_FLAT;
				settings.put(MODE_SETTING, MODE_FLAT);
				treeAction.setChecked(false);
				compressedAction.setChecked(false);
				refresh();
			}			
		};
		flatAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_MODE));
		toolbarManager.add(flatAction);
		treeAction = new Action(Policy.bind("ResourceSelectionTree.tree"), Action.AS_RADIO_BUTTON) {  //$NON-NLS-1$
			public void run() {
				mode = MODE_TREE;
				settings.put(MODE_SETTING, MODE_TREE);
				flatAction.setChecked(false);
				compressedAction.setChecked(false);
				refresh();
			}					
		};
		treeAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_TREE_MODE));
		toolbarManager.add(treeAction);
		
		compressedAction = new Action(Policy.bind("ResourceSelectionTree.compressedFolders"), Action.AS_RADIO_BUTTON) {  //$NON-NLS-1$
			public void run() {
				mode = MODE_COMPRESSED_FOLDERS;
				settings.put(MODE_SETTING, MODE_COMPRESSED_FOLDERS);
				treeAction.setChecked(false);
				flatAction.setChecked(false);
				refresh();
			}					
		};
		compressedAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_COMPRESSED_MODE));
		toolbarManager.add(compressedAction);
		
		toolbarManager.update(true);
		
		mode = MODE_COMPRESSED_FOLDERS;
		try {
			mode = settings.getInt(MODE_SETTING);
		} catch (Exception e) {}
		switch (mode) {
		case MODE_COMPRESSED_FOLDERS:
			compressedAction.setChecked(true);
			break;
		case MODE_FLAT:
			flatAction.setChecked(true);
			break;
		case MODE_TREE:
			treeAction.setChecked(true);
			break;			
		default:
			break;
		}

		if (checkbox) {
		  treeViewer = new CheckboxTreeViewer(viewerPane, SWT.MULTI); 
		  
		  // Override the spacebar behavior to toggle checked state for all selected items.
	      treeViewer.getControl().addKeyListener(new KeyAdapter() {
	          public void keyPressed(KeyEvent event) {
	              if (event.keyCode == SPACEBAR) {
	            	  Tree tree = (Tree)treeViewer.getControl();
	            	  TreeItem[] items = tree.getSelection();
	            	  for (int i = 0; i < items.length; i++) {
	            		  if (i > 0) items[i].setChecked(!items[i].getChecked());
	            	  }
	              }
	          }
	      });	    
	    } else {
	      treeViewer = new TreeViewer(viewerPane, SWT.MULTI);
	    }
		tree = treeViewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    viewerPane.setContent(tree);
		
		if (labelProvider == null) {
		  labelProvider = new ResourceSelectionLabelProvider();
		}
		
		treeViewer.setLabelProvider(labelProvider);
		treeViewer.setContentProvider(resourceSelectionContentProvider);
		treeViewer.setUseHashlookup(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 125;
		treeViewer.getControl().setLayoutData(gd);
		treeViewer.setInput(this);

//		if (checkbox) {
//		  SelectionListener selectionListener = new SelectionAdapter() {
//		    public void widgetSelected(SelectionEvent e) {
//		      setAllChecked(e.getSource() == selectAllButton);
//		    }			
//		  };
//
//		  deselectAllButton = new Button(this, SWT.PUSH);
//  	  deselectAllButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
//  	  deselectAllButton.setText(Policy.bind("ResourceSelectionTree.DeselectAll")); //$NON-NLS-1$
//  	  deselectAllButton.addSelectionListener(selectionListener);
//  	
//  	  selectAllButton = new Button(this, SWT.PUSH);
//  	  selectAllButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
//  	  selectAllButton.setText(Policy.bind("ResourceSelectionTree.SelectAll")); //$NON-NLS-1$
//  	  selectAllButton.addSelectionListener(selectionListener);
//		}
		
		treeViewer.expandAll();
		
		if (checkbox) {
      setAllChecked(true);
      ((CheckboxTreeViewer) treeViewer)
          .addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
              handleCheckStateChange(event);
            }
          });
    }

		MenuManager menuMgr = new MenuManager();
    Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager menuMgr) {
        fillTreeMenu(menuMgr);
      }
    });
    menuMgr.setRemoveAllWhenShown(true);
    treeViewer.getTree().setMenu(menu);			
	}
	
    void setAllChecked(boolean state) {
		((CheckboxTreeViewer)treeViewer).setAllChecked(state);  
	}
	
	protected void fillTreeMenu(IMenuManager menuMgr) {
		if (checkbox) {
			Action selectAllAction = new Action(Policy.bind("ResourceSelectionTree.SelectAll")) { //$NON-NLS-1$
				public void run() {
					setAllChecked(true);
				}
			};
			menuMgr.add(selectAllAction);
			Action deselectAllAction = new Action(Policy.bind("ResourceSelectionTree.DeselectAll")) { //$NON-NLS-1$
				public void run() {
					setAllChecked(false);
				}
			};
			menuMgr.add(deselectAllAction);	
			if (showIncludeUnversionedButton() && includeUnversioned) {
				menuMgr.add(new Separator());
				Action selectUnversionedAction = new Action(Policy.bind("ResourceSelectionTree.SelectUnversioned")) { //$NON-NLS-1$
					public void run() {
						checkUnversioned(tree.getItems(), true);
					}
				};
				menuMgr.add(selectUnversionedAction);
				Action deselectUnversionedAction = new Action(Policy.bind("ResourceSelectionTree.DeselectUnversioned")) { //$NON-NLS-1$
					public void run() {
						checkUnversioned(tree.getItems(), false);
					}
				};
				menuMgr.add(deselectUnversionedAction);				
			}
		}
		menuMgr.add(new Separator());
		if (mode != MODE_FLAT) {
			Action expandAllAction = new Action(Policy.bind("SyncAction.expandAll")) { //$NON-NLS-1$
				public void run() {
					treeViewer.expandAll();
				}
			};
			menuMgr.add(expandAllAction);
		}
		if (showRemoveFromViewAction && !checkbox && !treeViewer.getSelection().isEmpty()) {
			Action removeAction = new Action(Policy.bind("ResourceSelectionTree.remove")) { //$NON-NLS-1$
				public void run() {
					removeFromView();
				}
			};
			menuMgr.add(removeAction);			
		}
		if (customOptions != null) {
			menuMgr.add(new Separator());
			for (int i = 0; i < customOptions.length; i++) {
				menuMgr.add(customOptions[i]);
			}
		}
	}

	private void removeFromView() {
		IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
		if (removeFromViewValidator != null) {
			if (!removeFromViewValidator.canRemove(resourceList, selection)) {
				if (removeFromViewValidator.getErrorMessage() != null) {
					MessageDialog.openError(getShell(), Policy.bind("ResourceSelectionTree.remove"), removeFromViewValidator.getErrorMessage()); //$NON-NLS-1$
				}
				return;
			}
		}
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			IResource resource = (IResource)iter.next();
			remove(resource);
			resourceRemoved = true;
		}
		resources = new IResource[resourceList.size()];
		resourceList.toArray(resources);
		compressedFolders = null;
		rootFolders = null;
		folders = null;
		refresh();
	}
	
	private void remove(IResource resource) {
		ArrayList removedResources = new ArrayList();
		Iterator iter = resourceList.iterator();
		while (iter.hasNext()) {
			IResource checkResource = (IResource)iter.next();
			if (checkResource.getFullPath().toString().equals(resource.getFullPath().toString()) || (mode != MODE_FLAT && isChild(checkResource, resource)))
				removedResources.add(checkResource);
		}
		iter = removedResources.iterator();
		while(iter.hasNext()) resourceList.remove(iter.next());
	}
	
	public boolean showIncludeUnversionedButton() {
		return unversionedResourceList != null && unversionedResourceList.size() > 0;
	}

	public void removeUnversioned() {
		try 
		{
			Iterator iter = unversionedResourceList.iterator();
			while(iter.hasNext()) resourceList.remove(iter.next());
			
			resources = new IResource[resourceList.size()];
			resourceList.toArray(resources);
			compressedFolders = null;
			rootFolders = null;
			folders = null;
			refresh();
			includeUnversioned = false;
		}
		catch (Exception e) {
			SVNUIPlugin.openError(getShell(), null, null, e);
		}
	}
	
	public void addUnversioned() {
		try 
		{
			Iterator iter = unversionedResourceList.iterator();
			while(iter.hasNext()) resourceList.add(iter.next());
			
			resources = new IResource[resourceList.size()];
			resourceList.toArray(resources);
			Arrays.sort(resources, comparator);
			compressedFolders = null;
			rootFolders = null;
			folders = null;
			refresh();
			checkUnversioned(tree.getItems(), true);
			includeUnversioned = true;
		}
		catch (Exception e) {
			SVNUIPlugin.openError(getShell(), null, null, e);
		}
	}
	
	private void checkUnversioned(TreeItem[] items, boolean state) {
		for (int i = 0; i < items.length; i++) {
			if (unversionedResourceList.contains(items[i].getData())) {
				items[i].setChecked(state);
			}
			checkUnversioned(items[i].getItems(), state);
		}
	}
	
	private boolean isChild(IResource resource, IResource parent) {
    	IContainer container = resource.getParent();
    	while (container != null) {
    		if (container.getFullPath().toString().equals(parent.getFullPath().toString()))
    			return true;
    		container = container.getParent();
    	}		
		return false;
	}
	
	private void handleCheckStateChange(CheckStateChangedEvent event) {
		((CheckboxTreeViewer)treeViewer).setGrayed(event.getElement(), false);
		((CheckboxTreeViewer)treeViewer).setSubtreeChecked(event.getElement(), event.getChecked());	
		IResource resource = (IResource) event.getElement();
		updateParentState(resource, event.getChecked());
	}

	private void updateParentState(IResource child, boolean baseChildState) {
		if (mode == MODE_FLAT || child == null || child.getParent() == null || resourceList.contains(child.getParent())) {
			return;
		}
		CheckboxTreeViewer checkboxTreeViewer = (CheckboxTreeViewer)treeViewer;
		if (child == null) return;
		Object parent = resourceSelectionContentProvider.getParent(child);
		if (parent == null) return;
		boolean allSameState = true;
		Object[] children = null;
		children = resourceSelectionContentProvider.getChildren(parent);
		for (int i = children.length - 1; i >= 0; i--) {
			if (checkboxTreeViewer.getChecked(children[i]) != baseChildState || checkboxTreeViewer.getGrayed(children[i])) {
			   allSameState = false;
		       break;
			}
		}
		checkboxTreeViewer.setGrayed(parent, !allSameState);
		checkboxTreeViewer.setChecked(parent, !allSameState || baseChildState);
		updateParentState((IResource)parent, baseChildState);
	}


	private void refresh() {
		Object[] checkedElements = null;
		if (checkbox) checkedElements = ((CheckboxTreeViewer)treeViewer).getCheckedElements();
		treeViewer.refresh();
		treeViewer.expandAll();
		if (checkbox) ((CheckboxTreeViewer)treeViewer).setCheckedElements(checkedElements);
	}
	
	private IContainer[] getRootFolders() {
		if (rootFolders == null) getFolders();
		return rootFolders;
	}
	
	private IContainer[] getCompressedFolders() {
		if (compressedFolders == null) {
			compressedFolderList = new ArrayList();
			for (int i = 0; i < resources.length; i++) {
				if (resources[i] instanceof IContainer && !compressedFolderList.contains(resources[i]))
					compressedFolderList.add(resources[i]);
				if (!(resources[i] instanceof IContainer)) {
					IContainer parent = resources[i].getParent();
					if (parent != null && !(parent instanceof IWorkspaceRoot) && !compressedFolderList.contains(parent)) {
						compressedFolderList.add(parent);
					}
				}
			}
			compressedFolders = new IContainer[compressedFolderList.size()];
			compressedFolderList.toArray(compressedFolders);
			Arrays.sort(compressedFolders, comparator);
		}
		return compressedFolders;
	}
	
	private IResource[] getChildResources(IContainer parent) {
		ArrayList children = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			if (!(resources[i] instanceof IContainer)) {
				IContainer parentFolder = resources[i].getParent();
				if (parentFolder != null && parentFolder.equals(parent) && !children.contains(parentFolder))
					children.add(resources[i]);
			}
		}
		IResource[] childArray = new IResource[children.size()];
		children.toArray(childArray);
		return childArray;
	}
	
	private IResource[] getFolderChildren(IContainer parent) {
		ArrayList children = new ArrayList();
		folders = getFolders();
		for (int i =0; i < folders.length; i++) {
			if (folders[i].getParent() != null && folders[i].getParent().equals(parent)) children.add(folders[i]);
		}
		for (int i = 0; i < resources.length; i++) {
			if (!(resources[i] instanceof IContainer) && resources[i].getParent() != null && resources[i].getParent().equals(parent))
				children.add(resources[i]);
		}
		IResource[] childArray = new IResource[children.size()];
		children.toArray(childArray);
		return childArray;
	}
	
	private IContainer[] getFolders() {
		List rootList = new ArrayList();
		if (folders == null) {
			folderList = new ArrayList();
			for (int i = 0; i < resources.length; i++) {
				if (resources[i] instanceof IContainer) folderList.add(resources[i]);
				IResource parent = resources[i];
				while (parent != null && !(parent instanceof IWorkspaceRoot)) {
					if (!(parent.getParent() instanceof IWorkspaceRoot) && folderList.contains(parent.getParent())) break;
					if (parent.getParent() == null || parent.getParent() instanceof IWorkspaceRoot) {
						rootList.add(parent);
					}
					parent = parent.getParent();
					folderList.add(parent);
				}
			}
			folders = new IContainer[folderList.size()];
			folderList.toArray(folders);
			Arrays.sort(folders, comparator);
			rootFolders = new IContainer[rootList.size()];
			rootList.toArray(rootFolders);
			Arrays.sort(rootFolders, comparator);
		}
		return folders;
	}
	
	private class ResourceSelectionContentProvider extends WorkbenchContentProvider {
		public Object getParent(Object element) {
			return ((IResource)element).getParent();
		}
		public boolean hasChildren(Object element) {
			if (mode != MODE_FLAT && element instanceof IContainer) return true;
			else return false;
		}
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ResourceSelectionTree) {
				if (mode == MODE_FLAT) return resources;
				else if (mode == MODE_COMPRESSED_FOLDERS) return getCompressedFolders();
				else return getRootFolders();
			}
			if (parentElement instanceof IContainer) {
				if (mode == MODE_COMPRESSED_FOLDERS) {
					return getChildResources((IContainer)parentElement);
				}
				if (mode == MODE_TREE) {
					return getFolderChildren((IContainer)parentElement);
				}
			}
			return new Object[0];
		}
	}
	
	private class ResourceSelectionLabelProvider extends LabelProvider {
		private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		private CompareConfiguration compareConfiguration = new CompareConfiguration();		
		private AbstractSynchronizeLabelProvider syncLabelProvider = new AbstractSynchronizeLabelProvider() {
			
			protected ILabelProvider getDelegateLabelProvider() {
				return workbenchLabelProvider;
			}

			protected boolean isDecorationEnabled() {
				return true;
			}

			protected IDiff getDiff(Object element) {
				IResource resource = (IResource)element;
				return new ResourceSelectionDiff(resource);
			}
			
		};
		
		public Image getImage(Object element) {
			if (resourceList.contains(element)) {					
				SVNStatusKind statusKind = ResourceWithStatusUtil.getStatusKind((IResource)element);
				Image image = null;
				if (element instanceof IContainer && (statusKind == null || !statusKind.equals(SVNStatusKind.DELETED))) {
					image = workbenchLabelProvider.getImage(element);
					image = compareConfiguration.getImage(image, Differencer.NO_CHANGE);
				} else {
					if (statusKind != null) {
						if (statusKind.hasTreeConflict()) {
							image = workbenchLabelProvider.getImage(element);
							image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.TREE_CONFLICT);
						}
						else if (statusKind != null && statusKind.equals(SVNStatusKind.CONFLICTED)) {
							image = workbenchLabelProvider.getImage(element);
							image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.TEXT_CONFLICTED);
						}
					}
					if (image == null) image = syncLabelProvider.getImage(element);
					if (element instanceof IContainer) return image;
					if (unversionedResourceList.contains(element)) {
						image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.UNVERSIONED);
					}
					if (statusKind != null && statusKind.equals(SVNStatusKind.MISSING)) {
						image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.MISSING);
					}
				}
				String propertyStatus = ResourceWithStatusUtil.getPropertyStatus((IResource)element);
				if (propertyStatus != null && propertyStatus.length() > 0) {
				  if (propertyStatus.equals("conflicted")) //$NON-NLS-1$
					  image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.PROPERTY_CONFLICTED);
				  else
					  image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.PROPERTY_CHANGE);
				}
				return image;
			}
			else {
				Image image = workbenchLabelProvider.getImage(element);	
				return compareConfiguration.getImage(image, Differencer.NO_CHANGE);
			}
		}

		public String getText(Object element) {
			if (statusMap == null) return workbenchLabelProvider.getText(element);
			String text = null;
			IResource resource = (IResource)element;
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (mode == MODE_FLAT) text = resource.getName() + " - " + resource.getFullPath().toString(); //$NON-NLS-1$
			else if (mode == MODE_COMPRESSED_FOLDERS) {
				if (element instanceof IContainer) {
					IContainer container = (IContainer)element;
					text = container.getFullPath().makeRelative().toString();
				}
				else text = resource.getName();
			}
			else {
				text = resource.getName();
			}
			if (svnResource != null) {
				try {
					LocalResourceStatus status = svnResource.getStatus();
					if (status != null) {
						if (status.getMovedFromAbspath() != null) {
							text = text + Policy.bind("ResourceSelectionTree.movedFrom") + status.getMovedFromAbspath().substring(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString().length()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						}
						else if (status.getMovedToAbspath() != null) {
							text = text + Policy.bind("ResourceSelectionTree.movedTo") + status.getMovedToAbspath().substring(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString().length()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				} catch (SVNException e) {}
			}
			return text;
		}
		
	}
	
	private class ResourceSelectionDiff implements IThreeWayDiff {
		private IResource resource;
		
		public ResourceSelectionDiff(IResource resource) {
			this.resource = resource;
		}
		
		public int getDirection() {
			return IThreeWayDiff.OUTGOING;
		}

		public ITwoWayDiff getLocalChange() {
			// TODO Auto-generated method stub
			return null;
		}

		public ITwoWayDiff getRemoteChange() {
			// TODO Auto-generated method stub
			return null;
		}

		public int getKind() {
			int kind = IDiff.NO_CHANGE;
			if (syncInfoSet != null) {
				SyncInfo syncInfo = syncInfoSet.getSyncInfo(resource);
				if (syncInfo != null) {
					int change = SyncInfo.getChange(syncInfo.getKind());
					if (change == SyncInfo.CONFLICTING) kind = IThreeWayDiff.CONFLICTING;
					else if (change == SyncInfo.CHANGE) kind = IDiff.CHANGE;
					else if (change == SyncInfo.ADDITION) kind = IDiff.ADD;
					else if (change == SyncInfo.DELETION) kind = IDiff.REMOVE;
				}
			} else {
				SVNStatusKind statusKind = (SVNStatusKind)statusMap.get(resource);
				if (statusKind == null) kind = IDiff.NO_CHANGE;
				else if (statusKind.equals(SVNStatusKind.CONFLICTED)) kind = IThreeWayDiff.CONFLICTING;
				else if (statusKind.equals(SVNStatusKind.MODIFIED)) kind = IDiff.CHANGE;
				else if (statusKind.equals(SVNStatusKind.ADDED)) kind = IDiff.ADD;
				else if (statusKind.equals(SVNStatusKind.DELETED)) kind = IDiff.REMOVE;
			}
			if (resource instanceof IContainer) return IDiff.REMOVE;
			return kind;
		}

		public IPath getPath() {
			return resource.getFullPath();
		}

		public String toDiffString() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	private class ResourceComparator implements Comparator {
		public int compare(Object obj0, Object obj1) {
			IResource resource0 = (IResource)obj0;
			IResource resource1 = (IResource)obj1;
			return resource0.getFullPath().toOSString().compareTo(resource1.getFullPath().toOSString());
		}			
	}
	
	public static interface IToolbarControlCreator {
		public void createToolbarControls(ToolBarManager toolbarManager);
		public int getControlCount();
	}
	
	public static interface IRemoveFromViewValidator {
		public boolean canRemove(ArrayList resourceList, IStructuredSelection selection);
		public String getErrorMessage();
	}

	public void setRemoveFromViewValidator(
			IRemoveFromViewValidator removeFromViewValidator) {
		this.removeFromViewValidator = removeFromViewValidator;
	}

	public void setShowRemoveFromViewAction(boolean showRemoveFromViewAction) {
		this.showRemoveFromViewAction = showRemoveFromViewAction;
	}

	public boolean isResourceRemoved() {
		if (checkbox) {
			resourceRemoved = resources.length > getSelectedResources().length;
		}
		return resourceRemoved;
	}
	
	public static IResource[] dedupeResources(IResource[] resources) {
		if (resources == null) {
			return null;
		}
		List<String> locations = new ArrayList<String>();
		List<IResource> uniqueResources = new ArrayList<IResource>();
		for (IResource resource : resources) {
			if (resource.getParent() == null || resource.getParent().exists()) {
				if (resource.getLocation() == null || !locations.contains(resource.getLocation().toString())) {
					uniqueResources.add(resource);
					locations.add(resource.getLocation().toString());
				}
			}
		}
		IResource[] uniqueResourceArray = new IResource[uniqueResources.size()];
		uniqueResources.toArray(uniqueResourceArray);
		return uniqueResourceArray;
	}

}
