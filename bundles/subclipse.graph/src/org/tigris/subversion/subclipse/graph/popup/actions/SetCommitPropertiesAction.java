package org.tigris.subversion.subclipse.graph.popup.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.ChangeCommitPropertiesCommand;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.editors.NodeFigure;
import org.tigris.subversion.subclipse.graph.editors.NodeTooltipFigure;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.SetCommitPropertiesDialog;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SetCommitPropertiesAction extends Action {
	private NodeFigure nodeFigure;
	private Node node;
	private RevisionGraphEditor editor;

	public SetCommitPropertiesAction(NodeFigure nodeFigure, RevisionGraphEditor editor) {
		super();
		this.nodeFigure = nodeFigure;
		this.editor = editor;
		node = nodeFigure.getNode();
		setText("Set commit properties...");
	}

	public void run() {
		try {
			IResource resource = ((RevisionGraphEditorInput)editor.getEditorInput()).getResource();
			ISVNRemoteResource remoteResource = ((RevisionGraphEditorInput)editor.getEditorInput()).getRemoteResource();
	        ISVNInfo info = ((RevisionGraphEditorInput)editor.getEditorInput()).getInfo();
			final ProjectProperties projectProperties = (resource != null) ? ProjectProperties
	                .getProjectProperties(resource) : ProjectProperties.getProjectProperties(remoteResource);
			
			SVNRevision.Number revision = new SVNRevision.Number(node.getRevision());
	        SetCommitPropertiesDialog dialog = new SetCommitPropertiesDialog(Display.getDefault().getActiveShell(), revision, resource, projectProperties);
	        dialog.setOldAuthor(node.getAuthor());
	        dialog.setOldComment(node.getMessage());
	        if (dialog.open() == SetCommitPropertiesDialog.OK) {
              final String author;
              final String commitComment;
              if(node.getAuthor().equals(dialog.getAuthor()))
                author = null;
              else
                author = dialog.getAuthor();
              if(node.getMessage().equals(dialog.getComment()))
                commitComment = null;
              else
                commitComment = dialog.getComment();
              ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(info.getRepository().toString());
              final ChangeCommitPropertiesCommand command = new ChangeCommitPropertiesCommand(repository, revision, commitComment, author);
              
              PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
                  public void run(IProgressMonitor monitor) throws InvocationTargetException {
                    try {
                      command.run(monitor);
                    } catch(SVNException e) {
                      throw new InvocationTargetException(e);
                    } finally {
                    	if (command.isAuthorChanged()) node.setAuthor(author);
                    	if (command.isLogMessageChanged()) node.setMessage(commitComment);
                    	if (command.isAuthorChanged() || command.isLogMessageChanged()) {
                    		nodeFigure.setToolTip(new NodeTooltipFigure(node));
                    	}   	  
                    }
                  }
                });              
              
	        }
		} catch (Exception e) {
			SVNUIPlugin.openError(Display.getDefault().getActiveShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
		}
	}
	
	

}
