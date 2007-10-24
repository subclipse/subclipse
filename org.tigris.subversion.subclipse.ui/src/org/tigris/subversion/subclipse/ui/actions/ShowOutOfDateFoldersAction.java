package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.jface.action.Action;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.subscriber.SVNSynchronizeParticipant;

public class ShowOutOfDateFoldersAction extends Action {
	private SVNSynchronizeParticipant svnSynchronizeParticipant;

	public ShowOutOfDateFoldersAction() {
		super();
		setText(Policy.bind("SyncAction.showOutOfDateFolders")); //$NON-NLS-1$	
		setChecked(SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHOW_OUT_OF_DATE_FOLDERS));
	}
	
	public void setChecked(boolean checked) {
		super.setChecked(checked);
		SVNProviderPlugin.getPlugin().getPluginPreferences().setValue(ISVNCoreConstants.PREF_SHOW_OUT_OF_DATE_FOLDERS, isChecked());
		if (svnSynchronizeParticipant != null) svnSynchronizeParticipant.refresh(svnSynchronizeParticipant.getResources(), Policy.bind("ShowOutOfDateFoldersAction.refreshTaskName"), Policy.bind("ShowOutOfDateFoldersAction.refreshTaskName"), SVNUIPlugin.getActivePage().getActivePart().getSite()); //$NON-NLS-1$, //$NON-NLS-1$		
	}

	public void setSvnSynchronizeParticipant(
			SVNSynchronizeParticipant svnSynchronizeParticipant) {
		this.svnSynchronizeParticipant = svnSynchronizeParticipant;
	}

}
