package org.tigris.subversion.subclipse.graph.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.editors.GraphBackgroundTask;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class RefreshNodeAction extends Action {
	private Node node;
	private RevisionGraphEditor editor;

	public RefreshNodeAction(Node node, RevisionGraphEditor editor) {
		super();
		this.editor = editor;
		this.node = node;
		setText(Policy.bind("RefreshNodeAction.title")); //$NON-NLS-1$
	}
	
	public void run() {
		IResource resource = ((RevisionGraphEditorInput)editor.getEditorInput()).getResource();
		ISVNRemoteResource remoteResource = ((RevisionGraphEditorInput)editor.getEditorInput()).getRemoteResource();
		GraphBackgroundTask task;
		if (resource == null) task = new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), editor.getViewer(), editor, remoteResource);
		else task = new GraphBackgroundTask(SVNUIPlugin.getActivePage().getActivePart(), editor.getViewer(), editor, resource);	
		task.setGetNewRevisions(false);
		SVNRevision.Number revision = new SVNRevision.Number(node.getRevision());
		task.setRefreshRevision(revision, node);
		try {
			task.run();
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("RefreshNodeAction.title"), e.getMessage()); //$NON-NLS-1$
		}	
	}

}
