package org.tigris.subversion.subclipse.graph.popup.actions;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.editors.GraphBackgroundTask;
import org.tigris.subversion.subclipse.graph.editors.RevisionEditPart;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class RefreshNodeAction extends Action {
	private List selectedRevisions;
	private RevisionGraphEditor editor;

	public RefreshNodeAction(List selectedRevisions, RevisionGraphEditor editor) {
		super();
		this.editor = editor;
		this.selectedRevisions = selectedRevisions;
		setText(Policy.bind("RefreshNodeAction.title")); //$NON-NLS-1$
	}
	
	public void run() {
		IResource resource = ((RevisionGraphEditorInput)editor.getEditorInput()).getResource();
		ISVNRemoteResource remoteResource = ((RevisionGraphEditorInput)editor.getEditorInput()).getRemoteResource();

		GraphBackgroundTask task;
		if (resource == null) task = new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), editor.getViewer(), editor, remoteResource);
		else task = new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), editor.getViewer(), editor, resource);	
		task.setGetNewRevisions(false);
		SVNRevision[] revisions = new SVNRevision[selectedRevisions.size()];
		Node[] nodes = new Node[selectedRevisions.size()];
		for (int i = 0; i < selectedRevisions.size(); i++) {
			nodes[i] = (Node)((RevisionEditPart)selectedRevisions.get(i)).getModel();
			revisions[i] = new SVNRevision.Number(nodes[i].getRevision());
		}
		task.setRefreshRevisions(revisions, nodes);
		try {
			task.run();
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("RefreshNodeAction.title"), e.getMessage()); //$NON-NLS-1$
		}	
	}

}
