package org.tigris.subversion.subclipse.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
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
	private CheckboxTreeViewer treeViewer;
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

	public final static String MODE_SETTING = "ResourceSelectionTree.mode"; //$NON-NLS-1$
	public final static int MODE_COMPRESSED_FOLDERS = 0;
	public final static int MODE_FLAT = 1;
	public final static int MODE_TREE = 2;

	public ResourceSelectionTree(Composite parent, int style, String label, IResource[] resources, HashMap statusMap, LabelProvider labelProvider) {
		super(parent, style);
		this.label = label;
		this.resources = resources;
		this.statusMap = statusMap;
		this.labelProvider = labelProvider;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		Arrays.sort(resources, comparator);
		resourceList = new ArrayList();
		for (int i = 0; i < resources.length; i++)
			resourceList.add(resources[i]);
		createControls();
	}
	
	public CheckboxTreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	public IResource[] getSelectedResources() {
		ArrayList selected = new ArrayList();	
		Object[] checkedResources = treeViewer.getCheckedElements();
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
		
		if (label != null) treeGroup.setText(label);
		
		Composite buttonGroup = new Composite(treeGroup, SWT.NONE);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 1;
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonGroup.setLayout(buttonLayout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonGroup.setLayoutData(gridData);		
		
		ToolBar toolbar = new ToolBar(buttonGroup, SWT.FLAT);
		GridLayout toolbarLayout = new GridLayout();
		toolbarLayout.numColumns = 3;
//		toolbarLayout.numColumns = 2;
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

		treeViewer = new CheckboxTreeViewer(treeGroup, SWT.BORDER);
		if (labelProvider == null) labelProvider = new ResourceSelectionLabelProvider();
		treeViewer.setLabelProvider(labelProvider);
		treeViewer.setContentProvider(new ResourceSelectionContentProvider());
		treeViewer.setUseHashlookup(true);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 125;
		treeViewer.getControl().setLayoutData(gd);
		treeViewer.setInput(this);
		
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
				if (e.getSource() == selectAllButton) treeViewer.setAllChecked(true);
				else treeViewer.setAllChecked(false);
			}			
		};
		selectAllButton.addSelectionListener(selectionListener);
		deselectAllButton.addSelectionListener(selectionListener);
		
		treeViewer.expandAll();
		treeViewer.setAllChecked(true);
		if (mode == MODE_TREE) treeViewer.collapseAll();
		
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                handleCheckStateChange(event);
            }
        });
	}
	
	private void handleCheckStateChange(CheckStateChangedEvent event) {
        IResource resource = (IResource) event.getElement();
        boolean state = event.getChecked();

        treeViewer.setGrayed(resource, false);
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
	                treeViewer.setChecked(element, true);
	                treeViewer.setGrayed(element, false);
	            } else {
	                treeViewer.setGrayChecked(element, false);
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
	            if (treeViewer.getChecked(members[i]) || treeViewer.getGrayed(members[i])) 
	            	childChecked = true;
	            else
	            	childUnchecked = true;
	            if (childChecked && childUnchecked) break;
        	}       
        }
        if (!childChecked) {
        	treeViewer.setGrayChecked(parent, false);
        	treeViewer.setChecked(parent, false);
        } else {
        	if (childUnchecked) {
        		treeViewer.setChecked(parent, false);
        		treeViewer.setGrayChecked(parent, true);
        	} else {
        		treeViewer.setGrayChecked(parent, false);
        		treeViewer.setChecked(parent, true);
        	}
        }
    }    

	private void refresh() {
		Object[] checkedElements = treeViewer.getCheckedElements();
		treeViewer.refresh();
		treeViewer.expandAll();
		treeViewer.setCheckedElements(checkedElements);
		if (mode == MODE_COMPRESSED_FOLDERS) {
			for (int i = 0; i < compressedFolders.length; i++) {
				IResource[] children = getChildResources(compressedFolders[i]);
				if (children.length > 0) {
					updateParentState(children[0]);
				}
			}
		}
		if (mode == MODE_TREE) {
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
		
		public Image getImage(Object element) {
			return workbenchLabelProvider.getImage(element);
		}

		public String getText(Object element) {
			if (statusMap == null) return workbenchLabelProvider.getText(element);
			SVNStatusKind statusKind = (SVNStatusKind)statusMap.get(element);
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
			if (statusKind == null) return text;
			else return text + " (" + statusKind.toString() + ")";
		}
		
	}
	
	private class ResourceComparator implements Comparator {
		public int compare(Object obj0, Object obj1) {
			IResource resource0 = (IResource)obj0;
			IResource resource1 = (IResource)obj1;
			return resource0.getFullPath().toOSString().compareTo(resource1.getFullPath().toOSString());
		}			
	}

}
