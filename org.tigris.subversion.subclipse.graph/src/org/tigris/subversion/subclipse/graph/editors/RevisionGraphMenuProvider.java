package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.tigris.subversion.subclipse.graph.popup.actions.BranchTagAction;
import org.tigris.subversion.subclipse.graph.popup.actions.ImageAction;
import org.tigris.subversion.subclipse.graph.popup.actions.RefreshNodeAction;
import org.tigris.subversion.subclipse.graph.popup.actions.RevisionDetailsAction;
import org.tigris.subversion.subclipse.graph.popup.actions.SetCommitPropertiesAction;

public class RevisionGraphMenuProvider extends ContextMenuProvider {
	private RevisionGraphEditor editor;

	public RevisionGraphMenuProvider(EditPartViewer viewer, RevisionGraphEditor editor) {
		super(viewer);
		this.editor = editor;
	}

	public void buildContextMenu(IMenuManager menu) {
		GraphEditPart2 graphEditPart = (GraphEditPart2)getViewer().getContents();
		NodeFigure nodeFigure = graphEditPart.getSelectedNode();
		if (nodeFigure != null) {
			menu.add(new RevisionDetailsAction(nodeFigure, editor));
			menu.add(new SetCommitPropertiesAction(nodeFigure, editor));
			menu.add(new BranchTagAction("Create Branch/Tag from Revision " + nodeFigure.getNode().getRevision() + "...", editor, nodeFigure));
			menu.add(new RefreshNodeAction(nodeFigure, editor));		
		}
		menu.add(new Separator());
		menu.add(new ImageAction(editor));
	}

}
