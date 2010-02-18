package org.tigris.subversion.subclipse.ui;

import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.clientadapter.ILoadErrorHandler;
import org.tigris.subversion.clientadapter.ISVNClientWrapper;
import org.tigris.subversion.subclipse.ui.dialogs.LoadErrorDialog;

public class LoadErrorHandler implements ILoadErrorHandler {
	private static boolean loadErrorHandled = false;

	public void handleLoadError(ISVNClientWrapper clientWrapper) {
		String svnInterface = SVNUIPlugin.getPlugin().getPreferenceStore().getString(ISVNUIConstants.PREF_SVNINTERFACE);
		if (svnInterface != null && !svnInterface.equals("javahl")) {
			return;
		}		
		final String loadErrors = clientWrapper.getLoadErrors();
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				loadErrorHandled = true;
				LoadErrorDialog dialog = new LoadErrorDialog(Display.getDefault().getActiveShell(), loadErrors);
				dialog.open();
			}			
		});
	}
	
	public static boolean loadErrorHandled() {
		return loadErrorHandled;
	}

}
