package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.swt.events.MouseEvent;

public class RevisionGraphSelectionTool extends SelectionTool {

	public RevisionGraphSelectionTool() {
		super();
	}
	
	public void mouseUp(MouseEvent e, EditPartViewer viewer) {
		if (getTargetEditPart() instanceof RevisionEditPart)
			super.mouseUp(e, viewer);
	}
	
	public void mouseDown(MouseEvent e, EditPartViewer viewer) {
		if (getTargetEditPart() instanceof RevisionEditPart)
			super.mouseDown(e, viewer);
	}

}
