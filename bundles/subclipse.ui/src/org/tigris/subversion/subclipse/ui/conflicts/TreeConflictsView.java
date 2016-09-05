package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
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
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
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
	private ArrayList folderList;
	
	private TreeViewer treeViewer;
	
	private Action refreshAction;
	private OpenFileInSystemEditorAction openAction;
	
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	private boolean disposed;
	private static TreeConflictsView view;
	
	private String[] columnHeaders = {Policy.bind("TreeConflictsView.resource"), Policy.bind("TreeConflictsView.description")}; //$NON-NLS-1$ //$NON-NLS-2$
	private ColumnLayoutData columnLayouts[] = {
			new ColumnWeightData(75, 75, true),
			new ColumnWeightData(450, 450, true)};
	
	public TreeConflictsView() {
		super();
		view = this;
	}

	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		parent.setLayout(layout);		

		treeViewer = new TreeViewer(parent, SWT.FULL_SELECTION);
		treeViewer.setLabelProvider(new ConflictsLabelProvider());
		treeViewer.setContentProvider(new ConflictsContentProvider());
		treeViewer.setUseHashlookup(true);
		GridData layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		treeViewer.getControl().setLayoutData(layoutData);
		TableLayout tableLayout = new TableLayout();
		treeViewer.getTree().setLayout(tableLayout);

		DisposeListener disposeListener = new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TreeColumn col = (TreeColumn)e.getSource();
				if (col.getWidth() > 0) settings.put("TreeConflictsView.col." + col.getText(), col.getWidth()); //$NON-NLS-1$
			}			
		};
		for (int i = 0; i < columnHeaders.length; i++) {
			TreeColumn tc = new TreeColumn(treeViewer.getTree(), SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
			setColumnWidth(tableLayout, disposeListener, tc, i);
		}
		
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLinesVisible(false);
		
		treeViewer.setInput(this);

		treeViewer.addOpenListener(new IOpenListener() {
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
			setContentDescription(resource.getFullPath().toString());
	}
	
	private void setColumnWidth(TableLayout layout,
			DisposeListener disposeListener, TreeColumn col, int colIndex) {
		String columnWidth = null;
		columnWidth = settings.get("TreeConflictsView.col." + col.getText()); //$NON-NLS-1$ //$NON-NLS-1$
		if (columnWidth == null || columnWidth.equals("0")) layout.addColumnData(columnLayouts[colIndex]); //$NON-NLS-1$
		else layout.addColumnData(new ColumnPixelData(Integer.parseInt(columnWidth), true));
		col.addDisposeListener(disposeListener);
	}

	public void setFocus() {
		treeViewer.getControl().setFocus();
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
	
	public void dispose() {
		disposed = true;
		super.dispose();
	}
	
	public boolean isDisposed() {
		return disposed;
	}

	public IResource getResource() {
		return resource;
	}

	public static boolean refresh(IResource[] resources) {
		if (view == null || view.isDisposed() || view.getResource() == null) return false;
		for (int i = 0; i < resources.length; i++) {
			if (view.getResource().equals(resources[i]) || resources[i].getFullPath().toString().startsWith(view.getResource().getFullPath().toString())) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						view.refresh();
					}					
				});
				return true;
			}
		}
		return false;
	}
	
	public void refresh() {
		if (disposed || resource == null) return;
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				folderList = new ArrayList();				
				treeConflicts = new ArrayList();
				ISVNClientAdapter client = null;
				try {
					client = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getSVNClient();
					ISVNStatus[] statuses = client.getStatus(resource.getLocation().toFile(), true, false, true);
					for (int i = 0; i < statuses.length; i++) {
						if (statuses[i].hasTreeConflict()) {
							SVNTreeConflict treeConflict = new SVNTreeConflict(statuses[i]);
							
							IResource treeConflictResource = treeConflict.getResource();
							if (treeConflictResource instanceof IContainer && !folderList.contains(treeConflictResource)) {
								folderList.add(treeConflict);
							}
							if (!(treeConflictResource instanceof IContainer)) {
								IContainer parent = treeConflictResource.getParent();
								if (parent != null && !(parent instanceof IWorkspaceRoot) && !folderList.contains(parent)) {	
									folderList.add(parent);
								}
							}
							
							treeConflicts.add(treeConflict);
						}
					}		
					treeViewer.refresh();
					treeViewer.expandAll();
				} catch (Exception e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}	
				finally {
					SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().returnSVNClient(client);
				}
			}		
		});
	}
	
	private void createMenus() {		
		openAction = new OpenFileInSystemEditorAction(getSite().getPage(), treeViewer);
		MenuManager menuMgr = new MenuManager("#TreeConflictsViewPopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		
		treeViewer.getControl().setMenu(menu);
		
		getSite().registerContextMenu(menuMgr, treeViewer);	
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
			boolean conflictSelected = false;
			boolean enableOpen = false;
			IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
			
			Iterator iter = selection.iterator();
			while (iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof SVNTreeConflict) {
					conflictSelected = true;
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
			if (selection.size() == 1) {
				if (conflictSelected) {
					manager.add(new ResolveTreeConflictAction(treeViewer, this));
				}
				PropertyDialogAction propertiesAction = new PropertyDialogAction(new SameShellProvider(Display.getDefault().getActiveShell()), treeViewer);
				manager.add(propertiesAction);
				manager.add(new Separator());
			}
			manager.add(getRefreshAction());
		}
	}
	
	class ConflictsContentProvider extends WorkbenchContentProvider {
		public Object getParent(Object element) {
			return null;
		}
		public boolean hasChildren(Object element) {
			if (element instanceof SVNTreeConflict) {
				return (((SVNTreeConflict)element).getResource() instanceof IContainer);
			}
			else return true;
		}
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
		public Object[] getChildren(Object parentElement) {
			if (treeConflicts == null) return new Object[0];
			if (parentElement instanceof TreeConflictsView) {
				Object[] folderArray = new Object[folderList.size()];
				folderList.toArray(folderArray);
				sort(folderArray);
				return folderArray;
			}
			if (parentElement instanceof IContainer) {
				return getFolderChildren((IResource)parentElement);
			}
			if (parentElement instanceof SVNTreeConflict) {
				if (((SVNTreeConflict)parentElement).getResource() instanceof IContainer) {
					return getFolderChildren(((SVNTreeConflict)parentElement).getResource());
				}
			}
			return new Object[0];
		}
		private Object[] getFolderChildren(IResource parentElement) {
			List childList = new ArrayList();
			Iterator iter = treeConflicts.iterator();
			while (iter.hasNext()) {
				SVNTreeConflict treeConflict = (SVNTreeConflict)iter.next();
				if ((!(treeConflict.getResource() instanceof IContainer)) && treeConflict.getResource().getParent() != null && treeConflict.getResource().getParent().getFullPath().toString().equals(parentElement.getFullPath().toString()))
					childList.add(treeConflict);
			}
			Object[] children = new Object[childList.size()];
			childList.toArray(children);
			sort(children);
			return children;
		}
		private void sort(Object[] items) {
			Arrays.sort(items, new Comparator() {
				public int compare(Object o1, Object o2) {
					IResource r1;
					IResource r2;
					if (o1 instanceof IResource) r1 = (IResource)o1;
					else {
						SVNTreeConflict tc = (SVNTreeConflict)o1;
						r1 = tc.getResource();
					}
					if (o2 instanceof IResource) r2 = (IResource)o2;
					else {
						SVNTreeConflict tc = (SVNTreeConflict)o2;
						r2 = tc.getResource();
					}					
					return r1.getFullPath().toString().compareTo(r2.getFullPath().toString());
				}				
			});
		}
	}
	
	class ConflictsLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				IResource elementResource;
				if (element instanceof SVNTreeConflict)
					elementResource = ((SVNTreeConflict)element).getResource();
				else
					elementResource = (IResource)element;
				Image image;
				if (!elementResource.exists() && elementResource.getName().indexOf(".") == -1) { //$NON-NLS-1$
					image = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(elementResource.getFullPath().toString())));			
				} else {
					image = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(elementResource);				
				}
				return image;
			}
			return null;
		}
		public String getColumnText(Object element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (element instanceof SVNTreeConflict) {
					SVNTreeConflict treeConflict = (SVNTreeConflict)element;
					if (treeConflict.getResource() instanceof IContainer)
						return treeConflict.getResource().getFullPath().toString();
					else
						return treeConflict.getResource().getName();
				}
				else {
					return ((IResource)element).getFullPath().toString();
				}	
			case 1:
				if (element instanceof SVNTreeConflict)
					return ((SVNTreeConflict)element).getDescription();
				else
					return ""; //$NON-NLS-1$
			default:
				break;
			}
			return null;
		}
	}

}
