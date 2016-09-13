package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.IPropertyProvider;
import org.tigris.subversion.subclipse.ui.compare.PropertyCompareLocalResourceNode;
import org.tigris.subversion.subclipse.ui.compare.PropertyCompareRemoteResourceNode;
import org.tigris.subversion.subclipse.ui.dialogs.ComparePropertiesDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CompareSvnPropertiesAction extends WorkbenchWindowAction {
	private IPropertyProvider right;
	private Exception exception;

	public void execute(IAction action) {	
		exception = null;
		IResource[] resources = getSelectedResources();
		IPropertyProvider left = null;
		right = null;
		if (resources != null && resources.length > 0) {
			left = new PropertyCompareLocalResourceNode(resources[0], true, null);
			if (resources.length > 1) {
				right = new PropertyCompareLocalResourceNode(resources[1], true, null);
			}
			else {
				final ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);
				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {			
					public void run() {
						try {
							right = new PropertyCompareRemoteResourceNode(svnResource.getRemoteResource(SVNRevision.HEAD), SVNRevision.HEAD, true, null);
						} catch (SVNException e) {
							exception = e;
						}							
					}
				});
				if (exception != null) {
					MessageDialog.openError(getShell(), Policy.bind("CompareSvnPropertiesAction.0"), exception.getMessage()); //$NON-NLS-1$
					return;
				}
			}
		}
		else {
			ISVNRemoteResource[] remoteResources = getSelectedRemoteResources();
			if (remoteResources != null && remoteResources.length > 0) {
				left = new PropertyCompareRemoteResourceNode(remoteResources[0], SVNRevision.HEAD, true, null);
				if (remoteResources.length > 1) {
					right =  new PropertyCompareRemoteResourceNode(remoteResources[1], SVNRevision.HEAD, true, null);
				}
			}
		}
		ComparePropertiesDialog dialog = new ComparePropertiesDialog(getShell(), left, right);
		if (dialog.open() == ComparePropertiesDialog.OK) {
			CompareUI.openCompareEditorOnPage(dialog.getInput(), getTargetPage());
		}
	}

	@Override
	protected boolean isEnabled() throws TeamException {
		if (getSelectedResources() == null || getSelectedResources().length == 0) {
			return true;
		}
		return super.isEnabled();
	}
	
	
}
