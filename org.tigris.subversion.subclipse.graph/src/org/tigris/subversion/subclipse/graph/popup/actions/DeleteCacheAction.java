package org.tigris.subversion.subclipse.graph.popup.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.graph.cache.Cache;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;

public class DeleteCacheAction extends SVNAction {
	private ISVNInfo info;
	private Exception error;

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final ISVNRemoteFolder[] selectedFolders = getSelectedRemoteFolders();
		error = null;
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				ISVNClientAdapter client = null;
				try {
					client = SVNProviderPlugin.getPlugin().getSVNClient();
					info = client.getInfo(selectedFolders[0].getUrl());
				} catch (Exception e) {
					error = e;
				} finally {
					SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
				}
			}				
		});
		if (error != null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("DeleteCacheAction.title"), error.getMessage()); //$NON-NLS-1$
			return;
		}
		String uuid = info.getUuid();
		File f = Cache.getCacheDirectory(null);
		f = new File(f, uuid);
		if (!f.exists()) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Policy.bind("DeleteCacheAction.title"), Policy.bind("DeleteCacheAction.noCache")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		String[] url = { selectedFolders[0].getUrl().toString() };
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Policy.bind("DeleteCacheAction.title"), Policy.bind("DeleteCacheAction.confirm", url))) return; //$NON-NLS-1$ //$NON-NLS-2$
		File revisionsFile = new File(f, "revisions"); //$NON-NLS-1$
		if (!deleteFile(revisionsFile, true)) return;
		File logMessagesFile = new File(f, "logMessages"); //$NON-NLS-1$
		if (!deleteFile(logMessagesFile, true)) return;
		
		// Just in case of a failed refresh
		File revisionsTempFile = new File(f, "revisionsTemp"); //$NON-NLS-1$
		deleteFile(revisionsTempFile, false);
		File logMessagesTempFile = new File(f, "logMessagesTemp"); //$NON-NLS-1$
		deleteFile(logMessagesTempFile, false);
		
		deleteFile(f, true);
	}

	protected boolean isEnabled() throws TeamException {
		return true;
	}
	
	private boolean deleteFile(File f, boolean showErrorMessage) {
		if (!f.delete()) {
			if (showErrorMessage) {
				String[] file = { f.getPath() };
				MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("DeleteCacheAction.title"), Policy.bind("DeleteCacheAction.deleteError", file)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return false;
		}
		return true;
	}

}
