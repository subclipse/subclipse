/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.repository;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;
import org.tigris.subversion.subclipse.ui.util.DetailsDialogWithProjects;


/**
 * RemoveRootAction removes a repository
 */
public class RemoveRootAction extends SelectionListenerAction {
	private IStructuredSelection selection;
	private Shell shell;
	
	public RemoveRootAction(Shell shell) {
		super(Policy.bind("RemoteRootAction.label")); //$NON-NLS-1$
		this.shell = shell;
	}
	
	/**
	 * Returns the selected remote files
	 */
	protected ISVNRepositoryLocation[] getSelectedRemoteRoots() {
		ArrayList resources = null;
		if (selection!=null && !selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = SVNAction.getAdapter(elements.next(), ISVNRepositoryLocation.class);
				if (next instanceof ISVNRepositoryLocation) {
					resources.add(next);
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ISVNRepositoryLocation[] result = new ISVNRepositoryLocation[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ISVNRepositoryLocation[0];
	}
	
	protected String getErrorTitle() {
		return Policy.bind("RemoveRootAction.removeRoot_3"); //$NON-NLS-1$
	}

	public void run() {
		ISVNRepositoryLocation[] roots = getSelectedRemoteRoots();
		if (roots.length == 0) return;
		SVNProviderPlugin provider = SVNProviderPlugin.getPlugin();
		for (int i = 0; i < roots.length; i++) {
			try {	
				// Check if any projects are shared with the repository
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				final ArrayList shared = new ArrayList();
				for (int j = 0; j < projects.length; j++) {
					RepositoryProvider teamProvider = RepositoryProvider.getProvider(projects[j], SVNProviderPlugin.getTypeId());
					if (teamProvider!=null) {
					    try {
							SVNTeamProvider svnProvider = (SVNTeamProvider)teamProvider;
							if (svnProvider.getSVNWorkspaceRoot().getRepository().equals(roots[i])) {
								shared.add(projects[j]);
							}
					    } catch(Exception e) {
					        // Don't let any exception prevent from
					        // continuing
					    }
					}
				}
			
				// This will notify the RepositoryManager of the removal
				if (!shared.isEmpty()) {
					final String location = roots[i].getLocation();
					shell.getDisplay().syncExec(new Runnable() {
						public void run() {
							DetailsDialogWithProjects dialog = new DetailsDialogWithProjects(
								shell, 
								Policy.bind("RemoteRootAction.Unable_to_Discard_Location_1"), //$NON-NLS-1$
								Policy.bind("RemoteRootAction.Projects_in_the_local_workspace_are_shared_with__2", location), //$NON-NLS-1$
								Policy.bind("RemoteRootAction.The_projects_that_are_shared_with_the_above_repository_are__4"), //$NON-NLS-1$
								(IProject[]) shared.toArray(new IProject[shared.size()]),
								false,
								SVNUIPlugin.getStandardDisplay().getSystemImage(SWT.ICON_ERROR) );
							dialog.open();
						}
					});
				} else {
					provider.getRepositories().disposeRepository(roots[i]);
				}
			} catch (SVNException e) {
				SVNUIPlugin.openError(shell,null,null,e);
                SVNUIPlugin.log(e);
			}
		}
	}

    /**
     * updates the selection. this selection will be used during run
     * returns true if action can be enabled
     */
	protected boolean updateSelection(IStructuredSelection selection) {
		this.selection = selection;

		ISVNRepositoryLocation[] roots = getSelectedRemoteRoots();
		return roots.length > 0;
	}

}

