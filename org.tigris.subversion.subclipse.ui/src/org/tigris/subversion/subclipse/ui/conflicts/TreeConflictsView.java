package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.subclipse.core.resources.ISVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

public class TreeConflictsView extends ViewPart {
	public static final String VIEW_ID = "org.tigris.subversion.subclipse.ui.conflicts.TreeConflictsView"; //$NON-NLS-1$

	private IResource resource;
	private List treeConflicts;
	
	private Table table;
	private TableViewer tableViewer;
	
	private Action refreshAction;
	private OpenFileInSystemEditorAction openAction;
	
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	
	private String[] columnHeaders = {Policy.bind("TreeConflictsView.resource"), Policy.bind("TreeConflictsView.description")}; //$NON-NLS-1$ //$NON-NLS-2$
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(75, 75, true),
		new ColumnWeightData(450, 450, true)};
	
	public TreeConflictsView() {
		super();
	}

	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		parent.setLayout(layout);		

		table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData);
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);
        
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new TreeConflictsContentProvider());
		tableViewer.setLabelProvider(new TreeConflictsLabelProvider());
		
		for (int i = 0; i < columnHeaders.length; i++) {
			tableLayout.addColumnData(columnLayouts[i]);
			TableColumn tc = new TableColumn(table, SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
		}
		
		tableViewer.setInput(this);
		
		tableViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent evt) {
				openAction.run();
			}	
		});
		
		createMenus();
		createToolbar();
		
		String path = settings.get("TreeConflictsView.resource"); //$NON-NLS-1$
		if (path != null) {
			boolean container = settings.getBoolean("TreeConflictsView.container"); //$NON-NLS-1$
			if (container) resource = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(path));
			else resource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(path));
		}
		if (resource == null)
			setContentDescription(Policy.bind("TreeConflictsView.noResource")); //$NON-NLS-1$
		else
			showTreeConflictsFor(resource);
	}

	public void setFocus() {
		tableViewer.getControl().setFocus();
	}
	
	public void showTreeConflictsFor(IResource resource) {
		this.resource = resource;
		refreshAction.setEnabled(true);
		setContentDescription(resource.getFullPath().toString());
		if (resource.getFullPath() != null) {
			settings.put("TreeConflictsView.resource", resource.getLocation().toString()); //$NON-NLS-1$
			settings.put("TreeConflictsView.container", resource instanceof IContainer); //$NON-NLS-1$
		}
		refresh();
	}
	
	private void refresh() {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				treeConflicts = new ArrayList();
				try {
					ISVNClientAdapter client = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getSVNClient();
					ISVNStatus[] statuses = client.getStatus(resource.getLocation().toFile(), true, true, true);
					for (int i = 0; i < statuses.length; i++) {
						if (statuses[i].hasTreeConflict()) {
							SVNTreeConflict treeConflict = new SVNTreeConflict(statuses[i]);
							treeConflicts.add(treeConflict);
						}
					}
					tableViewer.refresh();
				} catch (Exception e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}		
			}		
		});
	}
	
	private void createMenus() {
		openAction = new OpenFileInSystemEditorAction(getSite().getPage(), tableViewer);
		MenuManager menuMgr = new MenuManager("#TreeConflictsViewPopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);		
		getSite().registerContextMenu(menuMgr, tableViewer);		
	}
	
	private void createToolbar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(getRefreshAction());
		toolbarManager.add(new Separator());	
		toolbarManager.update(false);
	}
	
	private Action getRefreshAction() {
		if (refreshAction == null) {
			refreshAction = new Action(Policy.bind("TreeConflictsView.refresh"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
				public void run() {
					refresh();
				}
			};
			refreshAction.setToolTipText(Policy.bind("TreeConflictsView.refreshView")); //$NON-NLS-1$
			refreshAction.setDisabledImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
			refreshAction.setHoverImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH));			
			if (resource == null) refreshAction.setEnabled(false);
		}
		return refreshAction;
	}
	
	private void fillContextMenu(IMenuManager manager) {
		if (resource != null) {
			boolean enableOpen = false;
			IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
			Iterator iter = selection.iterator();
			while (iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof SVNTreeConflict) {
					SVNTreeConflict treeConflict = (SVNTreeConflict)element;
					if (treeConflict.getResource() instanceof IFile && treeConflict.getResource().exists()) {
						enableOpen = true;
						break;
					}
				}
			}
			if (enableOpen) {
				manager.add(openAction);
			}	
			if (enableOpen && selection.size() == 1) {
	            MenuManager submenu =
	                new MenuManager(Policy.bind("TreeConflictsView.openWith")); //$NON-NLS-1$
	            SVNTreeConflict treeConflict = (SVNTreeConflict)selection.getFirstElement();
	            submenu.add(new OpenWithMenu(getSite().getPage(), treeConflict.getResource()));
	            manager.add(submenu);
			}
			manager.add(new Separator());
			manager.add(getRefreshAction());
		}
	}
	
	class TreeConflictsContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			if (treeConflicts == null) return new Object[0];
			ISVNTreeConflict[] treeConflictArray = new ISVNTreeConflict[treeConflicts.size()];
			treeConflicts.toArray(treeConflictArray);
			return treeConflictArray;
		}
	}	
	
	class TreeConflictsLabelProvider extends LabelProvider implements ITableLabelProvider {
		WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		
		public String getColumnText(Object element, int columnIndex) {
			ISVNTreeConflict treeConflict = (ISVNTreeConflict)element;
			switch (columnIndex) {
			case 0:
				return treeConflict.getStatus().getFile().getName();
			case 1:
				return treeConflict.getDescription();
			default:
				return ""; //$NON-NLS-1$
			}
		}
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) return workbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(((SVNTreeConflict)element).getResource());
			return null;
		}		
	}

}
