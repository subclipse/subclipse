package org.tigris.subversion.subclipse.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.actions.WorkbenchWindowAction;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.LinkList;

public class OpenBugUrlAction extends WorkbenchWindowAction {

	public OpenBugUrlAction() {
	}
	
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		Object[] selectedObjects = selection.toArray();
		HashSet urlsSet = new HashSet();
		for (int i = 0; i < selectedObjects.length; i++) {
			if (selectedObjects[i] instanceof ILogEntry)
			{
				ILogEntry logEntry = (ILogEntry)selectedObjects[i];
				try {
					ProjectProperties projectProperties = ProjectProperties.getProjectProperties(logEntry.getResource().getResource());
					LinkList linkList = projectProperties.getLinkList(logEntry.getComment());
					String[] urls = linkList.getUrls();
					for (int j = 0; j < urls.length; j++) {
						urlsSet.add(urls[j]);
					}
				} catch (Exception e) {
					handle(e, null, null);
				}
			}
		}
		int i = 0;
		Iterator it = urlsSet.iterator();
		while (it.hasNext()) 
		{
			try {
				PlatformUI.getWorkbench().getBrowserSupport().createBrowser("bugurlbrowser" + (i++)).openURL(new URL((String) it.next()));
			} catch (Exception e) {
				handle(e, null, null);
			}
		}
		return;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	protected boolean isEnabled() throws TeamException {
		Object[] selectedObjects = selection.toArray();
		for (int i = 0; i < selectedObjects.length; i++) {
			if (selectedObjects[i] instanceof ILogEntry)
			{
				ILogEntry logEntry = (ILogEntry)selectedObjects[i];
				ProjectProperties projectProperties = ProjectProperties.getProjectProperties(logEntry.getResource().getResource());
				if (projectProperties == null) return false;
				LinkList linkList = projectProperties.getLinkList(logEntry.getComment());
				return linkList.getUrls().length != 0;
			}
		}
		return false;
	}
}
