package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.Tool;
import org.eclipse.ui.IEditorPart;

public class RevisionGraphEditDomain extends DefaultEditDomain {
	private RevisionGraphSelectionTool selectionTool;

	public RevisionGraphEditDomain(IEditorPart editorPart) {
		super(editorPart);
	}

	public Tool getDefaultTool() {
		if (selectionTool == null)
			selectionTool = new RevisionGraphSelectionTool();
		return selectionTool;
	}

}
