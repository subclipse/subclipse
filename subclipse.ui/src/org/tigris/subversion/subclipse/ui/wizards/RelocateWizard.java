/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.repo.SVNRepositories;
import org.tigris.subversion.subclipse.core.repo.SVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public class RelocateWizard extends Wizard {
	private SVNRepositoryLocation repository;
	private IProject[] sharedProjects;
	private RelocateWizardWarningPage warningPage;
	private RelocateWizardUrlPage urlPage;

	public RelocateWizard(SVNRepositoryLocation repository) {
		super();
		this.repository = repository;
		setWindowTitle(Policy.bind("RelocateWizard.title")); //$NON-NLS-1$
		setSharedProjects();
	}
	
	public void addPages() {
        warningPage = new RelocateWizardWarningPage(
            "warningPage",  //$NON-NLS-1$ 
            Policy.bind("RelocateWizard.heading"), //$NON-NLS-1$
            SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION)); 
		addPage(warningPage);
        urlPage = new RelocateWizardUrlPage(
                "urlPage",  //$NON-NLS-1$ 
                Policy.bind("RelocateWizard.heading"), //$NON-NLS-1$
                SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION),
                repository.getUrl().toString()); 
        addPage(urlPage);
	}

	public boolean performFinish() {
		ISVNClientAdapter client = null;
		try {
			SVNRepositoryLocation newRepository = SVNRepositoryLocation.fromString(urlPage.getNewUrl());
			newRepository.setUsername(repository.getUsername());
			newRepository.setLabel(repository.getLabel());
			newRepository.validateConnection(new NullProgressMonitor());
			client = repository.getSVNClient();
			for (int i = 0; i < sharedProjects.length; i++) {
				client.relocate(repository.getUrl().toString(), newRepository.getUrl().toString(), sharedProjects[i].getLocation().toString(), true);
			}
            SVNRepositories repositories = SVNProviderPlugin.getPlugin().getRepositories();
            repositories.addOrUpdateRepository(newRepository);
			for (int i = 0; i < sharedProjects.length; i++) {
				SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(sharedProjects[i], true);
				sharedProjects[i].refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				RepositoryProvider provider = RepositoryProvider.getProvider(sharedProjects[i], SVNProviderPlugin.getTypeId());
				provider.setProject(sharedProjects[i]);
			}
			repositories.disposeRepository(repository);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy.bind("RelocateWizard.heading"), e.getMessage()); //$NON-NLS-1$
			return false;
		}
		finally {
			repository.returnSVNClient(client);
		}
		return true;
	}
	
	private void setSharedProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList shared = new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			RepositoryProvider teamProvider = RepositoryProvider.getProvider(projects[i], SVNProviderPlugin.getTypeId());
			if (teamProvider!=null) {
			    try {
					SVNTeamProvider svnProvider = (SVNTeamProvider)teamProvider;
					if (svnProvider.getSVNWorkspaceRoot().getRepository().equals(repository)) shared.add(projects[i]);
			    } catch(Exception e) {}				
			}
		}
		sharedProjects = new IProject[shared.size()];
		shared.toArray(sharedProjects);
	}

	public IProject[] getSharedProjects() {
		return sharedProjects;
	}

}
