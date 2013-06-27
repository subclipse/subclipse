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
package org.tigris.subversion.subclipse.ui.wizards.sharing;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.ui.IWorkbench;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalFolder;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNRepositorySourceProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.WorkspacePathValidator;
import org.tigris.subversion.subclipse.ui.actions.SynchronizeAction;
import org.tigris.subversion.subclipse.ui.subscriber.SVNSynchronizeParticipant;
import org.tigris.subversion.subclipse.ui.wizards.ConfigurationWizardMainPage;
import org.tigris.subversion.subclipse.ui.wizards.ConfigurationWizardRepositorySourceProviderPage;
import org.tigris.subversion.subclipse.ui.wizards.SVNRepositoryProviderWizardPage;

/**
 * This wizard helps the user to import a new project in their workspace
 * into a SVN repository for the first time.
 */
public class SharingWizard extends Wizard implements IConfigurationWizard {
	// The project to configure
	private IProject project;

	// The autoconnect page is used if .svn/ directories already exist.
	private ConfigurationWizardAutoconnectPage autoconnectPage;
	
	// Warning page if .svn/ directories do not exist in root, but exist in subdirectories.
	private SvnFoldersExistWarningPage warningPage;
	
	// The import page is used if .svn/ directories do not exist.
	private RepositorySelectionPage locationPage;
	
	private ConfigurationWizardRepositorySourceProviderPage repositorySourceProviderPage;
	private Map<ISVNRepositorySourceProvider, SVNRepositoryProviderWizardPage> wizardPageMap = new HashMap<ISVNRepositorySourceProvider, SVNRepositoryProviderWizardPage>();
	
	// The page that prompts the user for connection information.
	private ConfigurationWizardMainPage createLocationPage;
	
	// The page that prompts the user for module name.
	private DirectorySelectionPage directoryPage;
	
	// The page that tells the user what's going to happen.
	private SharingWizardFinishPage finishPage;
	
	// The status of the project directory
	private LocalResourceStatus projectStatus;  
	
	// The repository locations
	private ISVNRepositoryLocation[] locations;
	
	private boolean shareCanceled;
	
	public SharingWizard() {
		IDialogSettings workbenchSettings = SVNUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("NewLocationWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("NewLocationWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		setNeedsProgressMonitor(true);
		setWindowTitle(Policy.bind("SharingWizard.title")); //$NON-NLS-1$
	}	

    /**
     * add pages
     */		
	public void addPages() {
		ImageDescriptor sharingImage = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SHARE);
		if (project.getLocation() == null) {
			CannotSharePage cannotSharePage = new CannotSharePage("cannotSharePage", Policy.bind("SharingWizard.importTitle"), sharingImage, project); //$NON-NLS-1$ //$NON-NLS-2$
			addPage(cannotSharePage);
		}
		else if (doesSVNDirectoryExist()) {
            // if .svn directory exists, we add the autoconnect page
			autoconnectPage = new ConfigurationWizardAutoconnectPage("autoconnectPage", Policy.bind("SharingWizard.autoConnectTitle"), sharingImage, projectStatus); //$NON-NLS-1$ //$NON-NLS-2$
			autoconnectPage.setProject(project);
			autoconnectPage.setDescription(Policy.bind("SharingWizard.autoConnectTitleDescription")); //$NON-NLS-1$
			addPage(autoconnectPage);
		}
        else {
        	try {
        		ISVNLocalFolder localFolder = SVNWorkspaceRoot.getSVNFolderFor(project);
        		if (localFolder instanceof LocalFolder) {
        			IFolder[] svnFolders = ((LocalFolder)localFolder).getSVNFolders(null, false);
        			if (svnFolders.length > 0) {
        				warningPage = new SvnFoldersExistWarningPage("warningPage", Policy.bind("SharingWizard.importTitle"), sharingImage, svnFolders); //$NON-NLS-1$ //$NON-NLS-2$
        				warningPage.setDescription(Policy.bind("SharingWizard.svnFolderExists")); //$NON-NLS-1$
        				addPage(warningPage);        				
        				// Remember to update getNextPage.
        			}
        		}
			} catch (SVNException e) {
				SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.PERFORM_SYNC_EXEC);
			}
        	
            // otherwise we add : 
            // - the repository selection page
        	// - any contributed repository source pages
            // - the create location page
            // - the module selection page
            // - the finish page 
    		
           	IRunnableWithProgress runnable = new IRunnableWithProgress() {
    			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                	locations = SVNUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations(monitor);			}
        	};
            try {
    			new ProgressMonitorDialog(getShell()).run(true, false, runnable);
    		} catch (Exception e) {
                SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
    		}
            
			locationPage = new RepositorySelectionPage("importPage", Policy.bind("SharingWizard.importTitle"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
			locationPage.setDescription(Policy.bind("SharingWizard.importTitleDescription")); //$NON-NLS-1$
			addPage(locationPage);
			
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
				
			createLocationPage = new ConfigurationWizardMainPage("createLocationPage", Policy.bind("SharingWizard.enterInformation"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
			createLocationPage.setDescription(Policy.bind("SharingWizard.enterInformationDescription")); //$NON-NLS-1$
			addPage(createLocationPage);
			createLocationPage.setDialogSettings(getDialogSettings());
			ISVNRepositoryLocationProvider repositoryLocationProvider = new ISVNRepositoryLocationProvider() {
				public ISVNRepositoryLocation getLocation() throws TeamException {
					return SharingWizard.this.getLocation();
				}
				public IProject getProject() {
					return SharingWizard.this.getProject();
				}				
			};
			directoryPage = new DirectorySelectionPage("modulePage", Policy.bind("SharingWizard.enterModuleName"), sharingImage, repositoryLocationProvider); //$NON-NLS-1$ //$NON-NLS-2$
			directoryPage.setDescription(Policy.bind("SharingWizard.enterModuleNameDescription")); //$NON-NLS-1$
			addPage(directoryPage);
			finishPage = new SharingWizardFinishPage("finishPage", Policy.bind("SharingWizard.readyToFinish"), sharingImage, repositoryLocationProvider); //$NON-NLS-1$ //$NON-NLS-2$
			finishPage.setDescription(Policy.bind("SharingWizard.readyToFinishDescription")); //$NON-NLS-1$
			addPage(finishPage);
		}
	}
    
    /**
     * check if wizard can finish 
     */
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		if (page == directoryPage) {
			return directoryPage.useProjectName() || directoryPage.getDirectoryName() != null;
		} else if (page == finishPage) {
			return true;
		}
		return super.canFinish();
	}
    
    /**
     * get the next page
     */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == warningPage) {
			return locationPage;
		}
		if (page == autoconnectPage) return null;
		if (page == locationPage) {
			if (locationPage.getLocation() == null) {
				if (repositorySourceProviderPage == null) {
					return createLocationPage;
				}
				else {
					return repositorySourceProviderPage;
				}
			} else {
				return directoryPage;
			}
		}
		if (page == repositorySourceProviderPage) {
			ISVNRepositorySourceProvider selectedRepositorySourceProvider = repositorySourceProviderPage.getSelectedRepositorySourceProvider();
			if (selectedRepositorySourceProvider != null) {
				return wizardPageMap.get(selectedRepositorySourceProvider);
			}
			else {
				return createLocationPage;
			}
		}
		if (page instanceof SVNRepositoryProviderWizardPage) {
			return directoryPage;
		}
		if (page == createLocationPage) {
			return directoryPage;
		}
		if (page == directoryPage) {
			return finishPage;
		}
		return null;
	}
    
	/*
	 * @see IWizard#performFinish
	 */
	public boolean performFinish() {
		shareCanceled = false;
		if (!WorkspacePathValidator.validateWorkspacePath()) return true;
		final boolean[] result = new boolean[] { true };
		try {
			final boolean[] doSync = new boolean[] { false };
			getContainer().run(true /* fork */, true /* cancel */, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						monitor.beginTask("", 100); //$NON-NLS-1$
						if (autoconnectPage != null && (projectStatus != null)) {
							// Autoconnect to the repository using svn/ directories
							
							// Get the repository location (the get will add the locatin to the provider)
							boolean isPreviouslyKnown = SVNProviderPlugin.getPlugin().getRepositories().isKnownRepository(projectStatus.getUrlString(), false);
	
							// Validate the connection if the user wants to
							boolean validate = autoconnectPage.getValidate();					
							
                            if (validate && !isPreviouslyKnown) {
								ISVNRepositoryLocation location = SVNProviderPlugin.getPlugin().getRepository(projectStatus.getUrlString());                            	
								// Do the validation
								try {
									location.validateConnection(new SubProgressMonitor(monitor, 50));
								} catch (final TeamException e) {
									// Exception validating. We can continue if the user wishes.
									final boolean[] keep = new boolean[] { false };
									getShell().getDisplay().syncExec(new Runnable() {
										public void run() {
											keep[0] = MessageDialog.openQuestion(getContainer().getShell(),
												Policy.bind("SharingWizard.validationFailedTitle"), //$NON-NLS-1$
												Policy.bind("SharingWizard.validationFailedText", new Object[] {e.getStatus().getMessage()})); //$NON-NLS-1$
										}
									});
									if (!keep[0]) {
										// Remove the root
										try {
											if (!isPreviouslyKnown) {
												SVNProviderPlugin.getPlugin().getRepositories().disposeRepository(location);
											}
										} catch (TeamException e1) {
											SVNUIPlugin.openError(getContainer().getShell(), Policy.bind("exception"), null, e1, SVNUIPlugin.PERFORM_SYNC_EXEC); //$NON-NLS-1$
										}
										result[0] = false;
										return;
									}
									// They want to keep the connection anyway. Fall through.
								}
							}
							
							// Set the sharing
							SVNWorkspaceRoot.setSharing(project, new SubProgressMonitor(monitor, 50));
						} 
                        else {
							// No svn directory : Share the project
							doSync[0] = true;
							// Check if the directory exists on the server
							ISVNRepositoryLocation location = null;
							boolean isKnown = false;
							boolean createDirectory = true;
							try {
								location = getLocation();
								isKnown = SVNProviderPlugin.getPlugin().getRepositories().isKnownRepository(location.getLocation(), false);
								
                                // Purge any svn folders that may exists in subfolders
                                SVNWorkspaceRoot.getSVNFolderFor(project).unmanage(null);

                                // check if the remote directory already exist
								String remoteDirectoryName = getRemoteDirectoryName();
								ISVNRemoteFolder folder = location.getRemoteFolder(remoteDirectoryName);
								if (folder.exists(new SubProgressMonitor(monitor, 50))) {
									if (autoconnectPage == null) {
										getShell().getDisplay().syncExec(new Runnable() {
											public void run() {
											    if (!MessageDialog.openQuestion(getShell(), Policy.bind("SharingWizard.couldNotImport"), Policy.bind("SharingWizard.couldNotImportLong"))) {  //$NON-NLS-1$ //$NON-NLS-2$
											    	shareCanceled = true;
											    	return;
											    }
											}
										});
										if (shareCanceled) return;
									}
									createDirectory = false;
								}
							} catch (TeamException e) {
								SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.PERFORM_SYNC_EXEC);
								result[0] = false;
								doSync[0] = false;
								return;
							}
							
                            // Add the location to the provider if it is new
							if (!isKnown) {
								SVNProviderPlugin.getPlugin().getRepositories().addOrUpdateRepository(location);
							}
							
							// Create the remote module for the project
							SVNWorkspaceRoot.shareProject(location, project, getRemoteDirectoryName(), finishPage.getComment(), createDirectory, new SubProgressMonitor(monitor, 50));
							
							try{
								project.refreshLocal(IProject.DEPTH_INFINITE, new SubProgressMonitor(monitor, 50));
							}
							catch(CoreException ce){
								throw new TeamException(ce.getStatus());
							}
							
						}
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});

			if (shareCanceled) return false;
			
			if (doSync[0]) {
				final List syncList = new ArrayList();
				syncList.add(project);
				try {
					ISynchronizeParticipantReference[] references = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
					if (references != null) {
						for (int i = 0; i < references.length; i++) {
							ISynchronizeParticipantReference reference = references[i];
							ISynchronizeParticipant participant = reference.getParticipant();
							if (participant instanceof SVNSynchronizeParticipant) {
								SVNSynchronizeParticipant svnParticipant = (SVNSynchronizeParticipant)participant;
								IResource[] resources = svnParticipant.getResources();
								if (resources != null) {
									for (int j = 0; j < resources.length; j++) {
										IResource resource = resources[j];
										if (!resource.equals(project)) {
											syncList.add(resource);
										}
									}
								}
								break;
							}
						}
					}
				} catch (Exception e) {}
				SynchronizeAction synchronizeAction = new SynchronizeAction() {
					protected IResource[] getSelectedResources() {
						IResource[] selection = new IResource[syncList.size()];
						syncList.toArray(selection);
						return selection;
					}				
				};
				synchronizeAction.run(null);
			}
		} catch (InterruptedException e) {
			return true;
		} catch (InvocationTargetException e) {
			SVNUIPlugin.openError(getContainer().getShell(), null, null, e);
			return false;
		}

		return result[0];
	}

	/**
	 * Return an ISVNRepositoryLocation
	 */
	protected ISVNRepositoryLocation getLocation() throws TeamException {
		// If there is an autoconnect page then it has the location
		if (autoconnectPage != null) {
			return autoconnectPage.getLocation();
		}
		
		// If the import page has a location, use it.
		if (locationPage != null) {
			ISVNRepositoryLocation location = locationPage.getLocation();
			if (location != null) return location;
		}
		
		// Otherwise, get the location from the create location page
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				createLocationPage.finish(new NullProgressMonitor());
			}
		});
		final Properties properties = createLocationPage.getProperties();
		if (repositorySourceProviderPage != null) {
			ISVNRepositorySourceProvider selectedRepositorySourceProvider = repositorySourceProviderPage.getSelectedRepositorySourceProvider();
			if (selectedRepositorySourceProvider != null) {
				final SVNRepositoryProviderWizardPage wizardPage = wizardPageMap.get(selectedRepositorySourceProvider);
				if (wizardPage != null) {
					getShell().getDisplay().syncExec(new Runnable() {
						public void run() {
							properties.setProperty("url", wizardPage.getSelectedUrl()); //$NON-NLS-1$
						}
					});
				}
			}
		}
		ISVNRepositoryLocation location = SVNProviderPlugin.getPlugin().getRepositories().createRepository(properties);
		return location;
	}
	/**
	 * Return the directory name in the remote repository where to put the project
	 */
	private String getRemoteDirectoryName() {
		// If there is an autoconnect page then it has the module name
		if (autoconnectPage != null) {
//			return autoconnectPage.getSharing().getRepository();
            return Util.getLastSegment(autoconnectPage.getSharingStatus().getUrlString());
		}
		String moduleName = directoryPage.getDirectoryName();
		if (moduleName == null) moduleName = project.getName();
		return moduleName;
	}
	/*
	 * @see IConfigurationWizard#init(IWorkbench, IProject)
	 */
	public void init(IWorkbench workbench, IProject project) {
		this.project = project;
	}
    
    /**
     * check if there is a valid svn directory
     */
	private boolean doesSVNDirectoryExist() {
		// Determine if there is an existing .svn/ directory from which configuration
		// information can be retrieved.
        boolean isSVNFolder = false;
		try {
		  projectStatus = SVNWorkspaceRoot.peekResourceStatusFor(project);;
		  isSVNFolder = (projectStatus != null) && projectStatus.hasRemote();
          
		} catch (final SVNException e) {
			Shell shell = null;
			// If this is called before the pages have been added, getContainer will return null
			if (getContainer() != null) {
				shell = getContainer().getShell();
			}
            SVNUIPlugin.openError(shell, null, null, e);
		}
        return isSVNFolder; 
	}
	
    public IProject getProject() {
        return project;
    }
    
}
