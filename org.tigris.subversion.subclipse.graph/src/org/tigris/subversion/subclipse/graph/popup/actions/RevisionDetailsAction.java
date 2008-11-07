package org.tigris.subversion.subclipse.graph.popup.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.editors.NodeFigure;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ShowRevisionsDialog;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class RevisionDetailsAction extends Action {
	private NodeFigure nodeFigure;
	private Node node;
	private RevisionGraphEditor editor;
	private ISVNRemoteResource remoteResource;
	private ILogEntry logEntry;
	private boolean includeTags;

	public RevisionDetailsAction(NodeFigure nodeFigure, RevisionGraphEditor editor) {
		super();
		this.nodeFigure = nodeFigure;
		this.editor = editor;
		node = nodeFigure.getNode();
		setText("Revision info...");		
	}
	
	public void run() {
		remoteResource = null;
		logEntry = null;
		includeTags = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE);
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				try {
					RevisionGraphEditorInput input = (RevisionGraphEditorInput)editor.getEditorInput();
					ISVNInfo info = input.getInfo();
					ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(info.getRepository().toString());
//					remoteResource = repository.getRemoteFile(node.getPath());
					
					remoteResource = new RemoteFile(repository, new SVNUrl(repository.getLocation() + node.getPath()), new SVNRevision.Number(node.getRevision()));
					
					AliasManager tagManager = null;
	            	if (includeTags) tagManager = new AliasManager(remoteResource.getUrl());
	            	SVNRevision pegRevision = new SVNRevision.Number(node.getRevision());
	            	SVNRevision revisionStart = new SVNRevision.Number(node.getRevision());
	            	SVNRevision revisionEnd = new SVNRevision.Number(node.getRevision());
					GetLogsCommand logCmd = new GetLogsCommand(remoteResource, pegRevision, revisionStart, revisionEnd, false, 0, tagManager, true);
					logCmd.run(null);
					ILogEntry[] logEntries = logCmd.getLogEntries(); 
					if (logEntries != null && logEntries.length > 0) {
						logEntry = logEntries[0];
					} 
				} catch (Exception e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Revision Info", e.getMessage());
				}
			}
			
		});
		if (logEntry != null) {
			ShowRevisionsDialog dialog = new ShowRevisionsDialog(Display.getDefault().getActiveShell(), logEntry, remoteResource, includeTags, null);
			dialog.setTitle("Revision Info");
			dialog.setSelectFirst(true);
			dialog.open();
		}
	}

}
