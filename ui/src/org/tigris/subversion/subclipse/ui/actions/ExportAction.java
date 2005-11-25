package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public class ExportAction extends WorkspaceAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		dialog.setText(Policy.bind("ExportAction.exportTo"));
		final String directory = dialog.open();
		if (directory == null) return;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
					IResource[] resources = getSelectedResources();
					for (int i = 0; i < resources.length; i++) {	
						File srcPath = new File(resources[i].getLocation().toString());
						File destPath= new File(directory + File.separator + resources[i].getName());
						client.doExport(srcPath, destPath, true);
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("ExportAction.export"), e.getMessage());
				}					
			}			
		});
	}

}
