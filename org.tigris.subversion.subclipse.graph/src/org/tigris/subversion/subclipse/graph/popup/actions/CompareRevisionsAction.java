package org.tigris.subversion.subclipse.graph.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.dialogs.DifferencesDialog;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class CompareRevisionsAction extends Action {
	private Node node1;
	private Node node2;
	private RevisionGraphEditor editor;

	public CompareRevisionsAction(Node node1, Node node2, RevisionGraphEditor editor) {
		super();
		this.node1 = node1;
		this.node2 = node2;
		this.editor = editor;
		setText("Compare...");	
	}
	
	public void run() {
		RevisionGraphEditorInput input = (RevisionGraphEditorInput)editor.getEditorInput();
		boolean isFolder = (input.getResource() != null && input.getResource().getType() != IResource.FILE) ||
		(input.getRemoteResource() != null && input.getRemoteResource().isFolder());
		ISVNInfo info = input.getInfo();
		try {
			ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(info.getRepository().toString());
			ISVNRemoteResource remoteResource1;
			ISVNRemoteResource remoteResource2;
			if (isFolder) {
				remoteResource1 = new RemoteFolder(repository, new SVNUrl(repository.getLocation() + node1.getPath()), new SVNRevision.Number(node1.getRevision()));
				remoteResource2 = new RemoteFolder(repository, new SVNUrl(repository.getLocation() + node2.getPath()), new SVNRevision.Number(node2.getRevision()));
			} else {
				remoteResource1 = new RemoteFile(repository, new SVNUrl(repository.getLocation() + node1.getPath()), new SVNRevision.Number(node1.getRevision()));
				remoteResource2 = new RemoteFile(repository, new SVNUrl(repository.getLocation() + node2.getPath()), new SVNRevision.Number(node2.getRevision()));				
			}
			ISVNRemoteResource[] selectedResources = { remoteResource1, remoteResource2 };
			DifferencesDialog dialog = new DifferencesDialog(Display.getDefault().getActiveShell(), null, selectedResources, editor.getEditorSite().getPart());
			dialog.setFromRevision(Long.toString(node1.getRevision()));
			dialog.setToRevision(Long.toString(node2.getRevision()));
			dialog.open();
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Compare Revisions", e.getMessage());
		}		
	}

}
