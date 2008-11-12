package org.tigris.subversion.subclipse.graph.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.cache.Graph;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.popup.actions.ImageAction;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class GraphActionBarContributor extends ActionBarContributor {
	private RevisionGraphEditor editor;
	private IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	private static ToggleShowDeletedAction[] toggleShowDeletedActions;
	private static RefreshAction[] refreshActions;

	public void setActiveEditor(IEditorPart editor) {
		super.setActiveEditor(editor);
		this.editor = (RevisionGraphEditor)editor;
	}

	protected void buildActions() {
	}

	protected void declareGlobalActionKeys() {
	}
	
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		super.contributeToToolBar(toolBarManager);
		toolBarManager.add(new Separator());
        toolBarManager.add(new ZoomComboContributionItem(getPage()));
        toolBarManager.add(new Separator());

        refreshActions = new RefreshAction[] {
        	new RefreshAction("All new revisions", RefreshAction.TYPE_NEW), 
        	new RefreshAction("Graph revisions", RefreshAction.TYPE_NODES),
        	new RefreshAction("New revisions and graph revisions", RefreshAction.TYPE_BOTH)
        };
        RefreshMenuAction refreshAction = new RefreshMenuAction();
        toolBarManager.add(refreshAction);
        
        toggleShowDeletedActions = new ToggleShowDeletedAction[] {
        	new ToggleShowDeletedAction("If modified", RevisionGraphEditor.SHOW_DELETED_MODIFIED),
        	new ToggleShowDeletedAction("Yes", RevisionGraphEditor.SHOW_DELETED_YES),
        	new ToggleShowDeletedAction("No", RevisionGraphEditor.SHOW_DELETED_NO)
        };
        ShowDeletedAction showDeletedAction = new ShowDeletedAction();
        toolBarManager.add(showDeletedAction);
        
        Action filterConnectionsAction = new Action() {
        	public void run() {
        		store.setValue(RevisionGraphEditor.FILTER_CONNECTIONS, isChecked());
        		GraphEditPart graphEditPart = (GraphEditPart)editor.getViewer().getContents();
        		graphEditPart.setConnectionVisibility(graphEditPart.getSelectedNode());
        	}
        };
        filterConnectionsAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_FILTER_CONNECTIONS));
        filterConnectionsAction.setToolTipText("Show connections only for selected revision");
        filterConnectionsAction.setChecked(store.getBoolean(RevisionGraphEditor.FILTER_CONNECTIONS));
        toolBarManager.add(filterConnectionsAction);
        
        Action imageAction = new Action() {
			public void run() {
				Action action = new ImageAction(editor);
				action.run();
			}            	
        };
        imageAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_EXPORT_IMAGE));
        toolBarManager.add(imageAction);
	}
	
	/*
	public void contributeToMenu(IMenuManager menuManager) {
		super.contributeToMenu(menuManager);
		MenuManager viewMenu = new MenuManager("View");
		viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
		viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));
	}
	*/
	
	public static class RefreshMenuAction extends Action implements IMenuCreator {
		private Menu menu;
		
		public RefreshMenuAction() {
			setText("Refresh");
			setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
			setMenuCreator(this);
		}
		
		public void dispose() {
			if (menu != null)  {
				menu.dispose();
				menu= null;
			}
		}
		
		public Menu getMenu(Control parent) {
			if (menu != null) menu.dispose();
			menu = new Menu(parent);
			addActionToMenu(menu, refreshActions[0]);
			addActionToMenu(menu, refreshActions[1]);
			addActionToMenu(menu, refreshActions[2]);
			return menu;
		}
		
		public Menu getMenu(Menu parent) {
			return null;
		}
		
		private void addActionToMenu(Menu parent, Action action) {
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(parent, -1);			
		}
		
	}	
	
	public static class ShowDeletedAction extends Action implements IMenuCreator {
		private Menu menu;
		
		public ShowDeletedAction() {
			setText("Show deleted branches");
			setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_SHOW_DELETED));			
			setMenuCreator(this);
		}
		
		public void dispose() {
			if (menu != null)  {
				menu.dispose();
				menu= null;
			}
		}
		
		public Menu getMenu(Control parent) {
			if (menu != null) menu.dispose();
			menu = new Menu(parent);
			addActionToMenu(menu, toggleShowDeletedActions[0]);
			addActionToMenu(menu, toggleShowDeletedActions[1]);
			addActionToMenu(menu, toggleShowDeletedActions[2]);
			return menu;
		}
		
		public Menu getMenu(Menu parent) {
			return null;
		}
		
		private void addActionToMenu(Menu parent, Action action) {
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(parent, -1);			
		}
		
	}
	
	public class ToggleShowDeletedAction extends Action {
		private final int show;

		public ToggleShowDeletedAction(String text, int show) {
			super(text, AS_RADIO_BUTTON);
			this.show = show;
//			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			setChecked(show == store.getInt(RevisionGraphEditor.SHOW_DELETED_PREFERENCE));
		}
		
	    public int getShow() {
	        return show;
	    }	
	    
	    public void run() {
	    	if (isChecked()) {
	    		Activator.getDefault().getPreferenceStore().setValue(RevisionGraphEditor.SHOW_DELETED_PREFERENCE, show);
//	    		editor.refresh();
	    		GraphEditPart graphEditPart = (GraphEditPart)editor.getViewer().getContents();
	    		Graph graph = (Graph)graphEditPart.getModel();
	    		editor.getViewer().setContents("Redrawing graph...");
	    		editor.getViewer().setContents(graph);
	    	}
	    }
		
	}
	
	public class RefreshAction extends Action {
		private final int type;
		
		public final static int TYPE_NEW = 0;
		public final static int TYPE_NODES = 1;
		public final static int TYPE_BOTH = 2;
		
		public RefreshAction(String text, int type) {
			super(text);
			this.type = type;
		}
		
		public void run() {
			SVNRevision[] refreshRevisions = null;
			Node[] nodes = null;
			if (type == TYPE_NODES || type == TYPE_BOTH) {
				List refreshList = new ArrayList();
				GraphEditPart graphEditPart = (GraphEditPart)editor.getViewer().getContents();
				Graph graph = (Graph)graphEditPart.getModel();
				nodes = graph.getNodes();
				for (int i = 0; i < nodes.length; i++) {
					SVNRevision.Number revision = new SVNRevision.Number(nodes[i].getRevision());
					refreshList.add(revision);
				}
				refreshRevisions = new SVNRevision[refreshList.size()];
				refreshList.toArray(refreshRevisions);
			}
			IResource resource = ((RevisionGraphEditorInput)editor.getEditorInput()).getResource();
			ISVNRemoteResource remoteResource = ((RevisionGraphEditorInput)editor.getEditorInput()).getRemoteResource();
			GraphBackgroundTask task;
			if (resource == null) task = new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), editor.getViewer(), editor, remoteResource);
			else task = new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), editor.getViewer(), editor, resource);	
			if (type == TYPE_NODES) task.setGetNewRevisions(false);
			if (refreshRevisions != null) task.setRefreshRevisions(refreshRevisions, nodes);
			try {
				task.run();
			} catch (Exception e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Refresh " + getText(), e.getMessage()); //$NON-NLS-1$
			}	
			//			editor.refresh();
		}		
	}

}
