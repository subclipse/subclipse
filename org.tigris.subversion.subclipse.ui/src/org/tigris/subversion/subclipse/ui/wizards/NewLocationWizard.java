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
package org.tigris.subversion.subclipse.ui.wizards;


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.ISVNRepositorySourceProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Wizard to add a new location. Uses ConfigurationWizardMainPage for entering informations
 * about SVN repository location 
 */
public class NewLocationWizard extends Wizard {
	private ConfigurationWizardRepositorySourceProviderPage repositorySourceProviderPage;
	private ConfigurationWizardMainPage mainPage;
	private Map<ISVNRepositorySourceProvider, SVNRepositoryProviderWizardPage> wizardPageMap = new HashMap<ISVNRepositorySourceProvider, SVNRepositoryProviderWizardPage>();

	private Properties properties = null;
	
	public NewLocationWizard() {
		IDialogSettings workbenchSettings = SVNUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("NewLocationWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("NewLocationWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		setWindowTitle(Policy.bind("NewLocationWizard.title")); //$NON-NLS-1$
	}
	
	public NewLocationWizard(Properties initialProperties) {
		this();
		this.properties = initialProperties;
	}

	/**
	 * Creates the wizard pages
	 */
	public void addPages() {
		ISVNRepositorySourceProvider[] repositorySourceProviders = null;
		try {
			repositorySourceProviders = SVNUIPlugin.getRepositorySourceProviders();
		} catch (Exception e) {}
		if (repositorySourceProviders != null && repositorySourceProviders.length > 0) {
			repositorySourceProviderPage = new ConfigurationWizardRepositorySourceProviderPage("source", Policy.bind("NewLocationWizard.heading"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION), repositorySourceProviders); //$NON-NLS-1$ //$NON-NLS-2$
			repositorySourceProviderPage.setDescription(Policy.bind("NewLocationWizard.0")); //$NON-NLS-1$
			addPage(repositorySourceProviderPage);
			for (ISVNRepositorySourceProvider repositorySourceProvider : repositorySourceProviders) {
				SVNRepositoryProviderWizardPage wizardPage = repositorySourceProvider.getWizardPage();
				addPage(wizardPage);
				wizardPageMap.put(repositorySourceProvider, wizardPage);
			}
		}
		mainPage = new ConfigurationWizardMainPage("main", Policy.bind("NewLocationWizard.heading"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION)); //$NON-NLS-1$ //$NON-NLS-2$
		if (properties != null) {
			mainPage.setProperties(properties);
		}
		mainPage.setDescription(Policy.bind("NewLocationWizard.description")); //$NON-NLS-1$
		mainPage.setDialogSettings(getDialogSettings());
		addPage(mainPage);
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == repositorySourceProviderPage) {
			ISVNRepositorySourceProvider selectedRepositorySourceProvider = repositorySourceProviderPage.getSelectedRepositorySourceProvider();
			if (selectedRepositorySourceProvider != null) {
				return wizardPageMap.get(selectedRepositorySourceProvider);
			}
			else {
				return mainPage;
			}
		}
		return null;
	}

	/*
	 * @see IWizard#performFinish
	 */
	public boolean performFinish() {
		mainPage.finish(new NullProgressMonitor());
		Properties properties = mainPage.getProperties();
		if (repositorySourceProviderPage != null) {
			ISVNRepositorySourceProvider selectedRepositorySourceProvider = repositorySourceProviderPage.getSelectedRepositorySourceProvider();
			if (selectedRepositorySourceProvider != null) {
				SVNRepositoryProviderWizardPage wizardPage = wizardPageMap.get(selectedRepositorySourceProvider);
				if (wizardPage != null) {
					properties.setProperty("url", wizardPage.getSelectedUrl()); //$NON-NLS-1$
				}
			}
		}
		final ISVNRepositoryLocation[] root = new ISVNRepositoryLocation[1];
		SVNProviderPlugin provider = SVNProviderPlugin.getPlugin();
		try {
			root[0] = provider.getRepositories().createRepository(properties);
			// Validate the connection info.  This process also determines the rootURL
			try {
				new ProgressMonitorDialog(getShell()).run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException {
						try {
							root[0].validateConnection(monitor);
						} catch (TeamException e) {
							throw new InvocationTargetException(e);
						}
					}
				});
			} catch (InterruptedException e) {
				return false;
			} catch (InvocationTargetException e) {
				Throwable t = e.getTargetException();
				if (t instanceof TeamException) {
					throw (TeamException)t;
				}
			}
			provider.getRepositories().addOrUpdateRepository(root[0]);
		} catch (TeamException e) {
			if (root[0] == null) {
				// Exception creating the root, we cannot continue
				SVNUIPlugin.openError(getContainer().getShell(), Policy.bind("NewLocationWizard.exception"), null, e); //$NON-NLS-1$
				return false;
			} else {
				// Exception validating. We can continue if the user wishes.
				IStatus error = e.getStatus();
				if (error.isMultiStatus() && error.getChildren().length == 1) {
					error = error.getChildren()[0];
				}
					
				boolean keep = false;
				if (error.isMultiStatus()) {
					SVNUIPlugin.openError(getContainer().getShell(), Policy.bind("NewLocationWizard.validationFailedTitle"), null, e); //$NON-NLS-1$
				} else {
					keep = MessageDialog.openQuestion(getContainer().getShell(),
						Policy.bind("NewLocationWizard.validationFailedTitle"), //$NON-NLS-1$
						Policy.bind("NewLocationWizard.validationFailedText", new Object[] {error.getMessage()})); //$NON-NLS-1$
				}
				try {
					if (keep) {
						provider.getRepositories().addOrUpdateRepository(root[0]);
					} else {
						provider.getRepositories().disposeRepository(root[0]);
					}
				} catch (TeamException e1) {
					SVNUIPlugin.openError(getContainer().getShell(), Policy.bind("exception"), null, e1); //$NON-NLS-1$
					return false;
				}
				return keep;
			}
		}
		return true;	
	}

	@Override
	public boolean canFinish() {
		if (repositorySourceProviderPage != null) {
			ISVNRepositorySourceProvider selectedSourceProvider = repositorySourceProviderPage.getSelectedRepositorySourceProvider();
			if (selectedSourceProvider == null) {
				return mainPage.isPageComplete();
			}
			else {
				SVNRepositoryProviderWizardPage wizardPage = wizardPageMap.get(selectedSourceProvider);
				if (wizardPage != null) {
					return wizardPage.isPageComplete();
				}
			}
		}
		return super.canFinish();
	}
	
}
