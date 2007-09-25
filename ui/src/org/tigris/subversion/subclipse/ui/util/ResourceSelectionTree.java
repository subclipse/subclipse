package org.tigris.subversion.subclipse.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ResourceWithStatusUtil;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class ResourceSelectionTree extends Composite {
	private int mode;
	private IResource[] resources;
	private ArrayList resourceList;
	private IContainer[] compressedFolders;
	private IContainer[] folders;
	private ArrayList folderList;
	private IContainer[] rootFolders;
	private ArrayList compressedFolderList;
	private TreeViewer treeViewer;
	private LabelProvider labelProvider;
	private String label;
	private Button selectAllButton;
	private Button deselectAllButton;
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
	
	public final static String MODE_SETTING = "ResourceSelectionTree.mode"; //$NON-NLS-1$
	public final static int MODE_COMPRESSED_FOLDERS = 0;
	public final static int MODE_FLAT = 1;
	public final static int MODE_TREE = 2;

	public ResourceSelectionTree(Composite parent, int style, String label, IResource[] resources, HashMap statusMap, LabelProvider labelProvider, boolean checkbox, IToolbarControlCreator toolbarControlCreator, SyncInfoSet syncInfoSet) {
		super(parent, style);
		this.label = label;
		this.resources = resources;
		this.statusMap = statusMap;
		this.labelProvider = labelProvider;
		this.checkbox = checkbox;
		this.toolbarControlCreator = toolbarControlCreator;
		this.syncInfoSet = syncInfoSet;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		Arrays.sort(resources, comparator);
		resourceList = new ArrayList();
		for (int i = 0; i < resources.length; i++)
			resourceList.add(resources[i]);
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

	private void createControls() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		setLayoutData(gridData);

		Group treeGroup = new Group(this, SWT.NONE);
		GridLayout treeLayout = new GridLayout();
		treeLayout.numColumns = 1;
		treeLayout.marginWidth = 0;
		treeLayout.marginHeight = 0;
		treeGroup.setLayout(treeLayout);
		gridData = new GridData(GridData.FILL_BOTH);
		treeGroup.setLayoutData(gridData);	

		Composite toolbarGroup = new Composite(treeGroup, SWT.NONE);
		GridLayout toolbarGroupLayout = new GridLayout();
		toolbarGroupLayout.numColumns = 2;
		toolbarGroupLayout.marginWidth = 0;
		toolbarGroupLayout.marginHeight = 0;
		toolbarGroup.setLayout(toolbarGroupLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		toolbarGroup.setLayoutData(gridData);	
		
		Label toolbarLabel = new Label(toolbarGroup, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalAlignment = SWT.BEGINNING;
		toolbarLabel.setLayoutData(gridData);
		if (label != null) toolbarLabel.setText(label);
		
		int buttonGroupColumns = 1;
		if (toolbarControlCreator != null) buttonGroupColumns = buttonGroupColumns + toolbarControlCreator.getControlCount();
		Composite buttonGroup = new Composite(toolbarGroup, SWT.NONE);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = buttonGroupColumns;
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonGroup.setLayout(buttonLayout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonGroup.setLayoutData(gridData);
		if (toolbarControlCreator != null) toolbarControlCreator.createToolbarControls(buttonGroup);
		
		ToolBar toolbar = new ToolBar(buttonGroup, SWT.FLAT);
		GridLayout toolbarLayout = new GridLayout();
		toolbarLayout.numColumns = 3;
		toolbar.setLayout(toolbarLayout);
		gridData = new GridData(GridData.FILL_BOTH);
		toolbar.setLayoutData(gridData);

		ToolBarManager toolbarManager = new ToolBarManager(toolbar);
		
		flatAction = new Action(Policy.bind("ResourceSelectionTree.flat"), Action.AS_CHECK_BOX) {  //$NON-NLS-1$
			public void run() {
				mode = MODE_FLAT;
				settings.put(MODE_SETTING, MODE_FLAT);
				treeAction.setChecked(false);
				compressedAction.setChecked(false);
				refresh();
			}			
		};
		flatAction .setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_MODE));
		toolbarManager.add(flatAction);
		
		treeAction = new Action(Policy.bind("ResourceSelectionTree.tree"), Action.AS_CHECK_BOX) {  //$NON-NLS-1$
			public void run() {
				mode = MODE_TREE;
				settings.put(MODE_SETTING, MODE_TREE);
				flatAction.setChecked(false);
				compressedAction.setChecked(false);
				refresh();
			}					
		};
		treeAction .setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_TREE_MODE));
		toolbarManager.add(treeAction);
		
		compressedAction = new Action(Policy.bind("ResourceSelectionTree.compressedFolders"), Action.AS_CHECK_BOX) {  //$NON-NLS-1$
			public void run() {
				mode = MODE_COMPRESSED_FOLDERS;
				settings.put(MODE_SETTING, MODE_COMPRESSED_FOLDERS);
				treeAction.setChecked(false);
				flatAction.setChecked(false);
				refresh();
			}					
		};
		compressedAction .setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_COMPRESSED_MODE));
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

		if (checkbox) treeViewer = new CheckboxTreeViewer(treeGroup, SWT.BORDER | SWT.MULTI);
		else treeViewer = new TreeViewer(treeGroup, SWT.BORDER | SWT.MULTI);
		if (labelProvider == null) labelProvider = new ResourceSelectionLabelProvider();
		treeViewer.setLabelProvider(labelProvider);
		treeViewer.setContentProvider(new ResourceSelectionContentProvider());
		treeViewer.setUseHashlookup(true);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 125;
		treeViewer.getControl().setLayoutData(gd);
		treeViewer.setInput(this);

		if (checkbox) {
			Composite selectGroup = new Composite(this, SWT.NONE);
			GridLayout selectLayout = new GridLayout();
			selectLayout.numColumns = 2;
			selectGroup.setLayout(selectLayout);
			gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
			selectGroup.setLayoutData(gridData);	
	
			selectAllButton = new Button(selectGroup, SWT.PUSH);
			selectAllButton.setText(Policy.bind("ResourceSelectionTree.SelectAll")); //$NON-NLS-1$
			gridData = new GridData();
			gridData.widthHint = 75;
			selectAllButton.setLayoutData(gridData);
			deselectAllButton = new Button(selectGroup, SWT.PUSH);
			deselectAllButton.setText(Policy.bind("ResourceSelectionTree.DeselectAll")); //$NON-NLS-1$
			gridData = new GridData();
			gridData.widthHint = 75;
			deselectAllButton.setLayoutData(gridData);	
			
			SelectionListener selectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() == selectAllButton) ((CheckboxTreeViewer)treeViewer).setAllChecked(true);
					else ((CheckboxTreeViewer)treeViewer).setAllChecked(false);
				}			
			};
			selectAllButton.addSelectionListener(selectionListener);
			deselectAllButton.addSelectionListener(selectionListener);
		}
		
		treeViewer.expandAll();
		if (checkbox) ((CheckboxTreeViewer)treeViewer).setAllChecked(true);
		if (checkbox && mode == MODE_TREE) treeViewer.collapseAll();
		
		if (checkbox) {
			((CheckboxTreeViewer)treeViewer).addCheckStateListener(new ICheckStateListener() {
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
	
	protected void fillTreeMenu(IMenuManager menuMgr) {
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
        IResource resource = (IResource) event.getElement();
        boolean state = event.getChecked();

        ((CheckboxTreeViewer)treeViewer).setGrayed(resource, false);
        if (resource instanceof IContainer) {
        	if (!(resourceList.contains(resource)))
        		setSubtreeChecked((IContainer)resource, state);
        }
        else updateParentState(resource);
	}
	
	private void setSubtreeChecked(IContainer container, boolean state) {
		IResource[] members = getChildResources(container);
        for (int i = members.length - 1; i >= 0; i--) {
            IResource element = members[i];
            if (!(element instanceof IContainer)) {
	            if (state) {
	            	((CheckboxTreeViewer)treeViewer).setChecked(element, true);
	            	((CheckboxTreeViewer)treeViewer).setGrayed(element, false);
	            } else {
	            	((CheckboxTreeViewer)treeViewer).setGrayChecked(element, false);
	            }
            }
        }
    }
    
    private void updateParentState(IResource child) {
        if (mode == MODE_FLAT || child == null || child.getParent() == null || resourceList.contains(child.getParent())) {
			return;
		}
        IContainer parent = child.getParent();
        boolean childChecked = false;
        boolean childUnchecked = false;
        IResource[] members;
        if (mode == MODE_COMPRESSED_FOLDERS) members = getChildResources(parent);
        else members = getFolderChildren(parent);
        for (int i = members.length - 1; i >= 0; i--) {      	
        	if (!(members[i] instanceof IContainer)) { 	
	            if (((CheckboxTreeViewer)treeViewer).getChecked(members[i]) || ((CheckboxTreeViewer)treeViewer).getGrayed(members[i])) 
	            	childChecked = true;
	            else
	            	childUnchecked = true;
	            if (childChecked && childUnchecked) break;
        	}       
        }
        if (!childChecked) {
        	((CheckboxTreeViewer)treeViewer).setGrayChecked(parent, false);
        	((CheckboxTreeViewer)treeViewer).setChecked(parent, false);
        } else {
        	if (childUnchecked) {
        		((CheckboxTreeViewer)treeViewer).setChecked(parent, false);
        		((CheckboxTreeViewer)treeViewer).setGrayChecked(parent, true);
        	} else {
        		((CheckboxTreeViewer)treeViewer).setGrayChecked(parent, false);
        		((CheckboxTreeViewer)treeViewer).setChecked(parent, true);
        	}
        }
    }    

	private void refresh() {
		Object[] checkedElements = null;
		if (checkbox) checkedElements = ((CheckboxTreeViewer)treeViewer).getCheckedElements();
		treeViewer.refresh();
		treeViewer.expandAll();
		if (checkbox) ((CheckboxTreeViewer)treeViewer).setCheckedElements(checkedElements);
		if (checkbox && mode == MODE_COMPRESSED_FOLDERS) {
			for (int i = 0; i < compressedFolders.length; i++) {
				IResource[] children = getChildResources(compressedFolders[i]);
				if (children.length > 0) {
					updateParentState(children[0]);
				}
			}
		}
		if (checkbox && mode == MODE_TREE) {
			for (int i = 0; i < folders.length; i++) {
				IResource[] children = getFolderChildren(folders[i]);
				if (children.length > 0) {
					updateParentState(children[0]);
				}
			}
			treeViewer.collapseAll();
		}
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
				IContainer parent = resources[i].getParent();
				if (parent != null && !(parent instanceof IWorkspaceRoot) && !compressedFolderList.contains(parent)) {
					compressedFolderList.add(parent);
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
		if (folders == null) {
			folderList = new ArrayList();
			for (int i = 0; i < resources.length; i++) {
				if (resources[i] instanceof IContainer) folderList.add(resources[i]);
				IResource parent = resources[i];
				while (parent != null && !(parent instanceof IWorkspaceRoot)) {
					if (folderList.contains(parent.getParent())) break;
					if (parent.getParent() == null || parent.getParent() instanceof IWorkspaceRoot) {
						rootFolders = new IContainer[1];
						rootFolders[0] = (IContainer)parent;
					}
					parent = parent.getParent();
					folderList.add(parent);
				}
			}
			folders = new IContainer[folderList.size()];
			folderList.toArray(folders);
			Arrays.sort(folders, comparator);
		}
		return folders;
	}
	
	private class ResourceSelectionContentProvider extends WorkbenchContentProvider {
		public Object getParent(Object element) {
			return null;
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
				Image image = null;
				if (element instanceof IContainer)
					image = workbenchLabelProvider.getImage(element);
				else {
					String textStatus = ResourceWithStatusUtil.getStatus((IResource)element);
					if (textStatus != null && textStatus.length() > 0) {
						SVNStatusKind statusKind = SVNStatusKind.fromString(textStatus);
						if (statusKind.equals(SVNStatusKind.CONFLICTED)) {
							image = workbenchLabelProvider.getImage(element);
							image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.TEXT_CONFLICTED);
						}
					}
					if (image == null) image = syncLabelProvider.getImage(element);
				}
				String propertyStatus = ResourceWithStatusUtil.getPropertyStatus((IResource)element);
				if (propertyStatus != null && propertyStatus.length() > 0) image = resourceSelectionTreeDecorator.getImage(image, ResourceSelectionTreeDecorator.PROPERTY_CHANGE);
				return image;
			}
			else {
				Image image = workbenchLabelProvider.getImage(element);	
				return compareConfiguration.getImage(image, Differencer.NO_CHANGE);
			}
		}

		public String getText(Object element) {
			if (statusMap == null) return workbenchLabelProvider.getText(element);
//			SVNStatusKind statusKind = (SVNStatusKind)statusMap.get(element);
			String text = null;
			IResource resource = (IResource)element;
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
		public void createToolbarControls(Composite composite);
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

}
