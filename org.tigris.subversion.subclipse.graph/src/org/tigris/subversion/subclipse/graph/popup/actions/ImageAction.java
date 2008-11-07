package org.tigris.subversion.subclipse.graph.popup.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.graph.dialogs.SaveImageDialog;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class ImageAction extends Action {
	private RevisionGraphEditor editor;

	public ImageAction(RevisionGraphEditor editor) {
		super();
		this.editor = editor;
		setText("Save image to file...");
		setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_EXPORT_IMAGE));
	}

	public void run() {
		SaveImageDialog dialog = new SaveImageDialog(Display.getDefault().getActiveShell(), editor);
		dialog.open();
	}

}
