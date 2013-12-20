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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNRepositorySourceProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.CheckoutAsProjectAction;
import org.tigris.subversion.subclipse.ui.actions.CheckoutIntoAction;
import org.tigris.subversion.subclipse.ui.actions.CheckoutUsingProjectWizardAction;

public class CheckoutWizard extends Wizard implements INewWizard, IImportWizard {
	private CheckoutWizardLocationPage locationPage;

	private ConfigurationWizardRepositorySourceProviderPage repositorySourceProviderPage;
	private Map<ISVNRepositorySourceProvider, SVNRepositoryProviderWizardPage> wizardPageMap = new HashMap<ISVNRepositorySourceProvider, SVNRepositoryProviderWizardPage>();
	
	private ConfigurationWizardMainPage createLocationPage;

	private CheckoutWizardSelectionPage selectionPage;

	private CheckoutWizardCheckoutAsMultiplePage checkoutAsMultiplePage;

	private CheckoutWizardCheckoutAsWithProjectFilePage checkoutAsWithProjectFilePage;

	private CheckoutWizardCheckoutAsWithoutProjectFilePage checkoutAsWithoutProjectFilePage;

	private CheckoutWizardProjectPage projectPage;
	
	private List<String> createdRepositoryUrls = new ArrayList<String>();

	private IProject project;
	private String projectName;
	private String projectNamePrefix;
	private String projectNameSuffix;

    private boolean hasProjectFile;
	private ISVNRepositoryLocation repositoryLocation;

	private ISVNRemoteFolder[] remoteFolders;

	public CheckoutWizard() {
		super();
		setWindowTitle(Policy.bind("CheckoutWizard.title")); //$NON-NLS-1$
	}

	public CheckoutWizard(ISVNRemoteFolder[] remoteFolders) {
		this();
		this.remoteFolders = remoteFolders;
		if (remoteFolders.length == 1) {
				checkForProjectFile();
		}
	}

	public void addPages() {
		setNeedsProgressMonitor(true);
		if (remoteFolders == null) {
			locationPage = new CheckoutWizardLocationPage("locationPage", //$NON-NLS-1$
					Policy.bind("CheckoutWizardLocationPage.heading"), //$NON-NLS-1$
					SVNUIPlugin.getPlugin().getImageDescriptor(
							ISVNUIConstants.IMG_WIZBAN_SHARE));
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
			
			createLocationPage = new ConfigurationWizardMainPage(
					"createLocationPage", //$NON-NLS-1$
					Policy.bind("CheckoutWizardLocationPage.heading"), //$NON-NLS-1$
					SVNUIPlugin.getPlugin().getImageDescriptor(
							ISVNUIConstants.IMG_WIZBAN_SHARE));
			addPage(createLocationPage);
			selectionPage = new CheckoutWizardSelectionPage("selectionPage", //$NON-NLS-1$
					Policy.bind("CheckoutWizardSelectionPage.heading"), //$NON-NLS-1$
					SVNUIPlugin.getPlugin().getImageDescriptor(
							ISVNUIConstants.IMG_WIZBAN_SHARE));
			addPage(selectionPage);
		}
		if (remoteFolders == null || remoteFolders.length > 1) {
			checkoutAsMultiplePage = new CheckoutWizardCheckoutAsMultiplePage(
					"checkoutAsMultiplePage", //$NON-NLS-1$
					Policy.bind("CheckoutWizardCheckoutAsPage.heading"), //$NON-NLS-1$
					SVNUIPlugin.getPlugin().getImageDescriptor(
							ISVNUIConstants.IMG_WIZBAN_SHARE));
			addPage(checkoutAsMultiplePage);
		}
		if (remoteFolders == null || remoteFolders.length == 1) {
			if (remoteFolders == null || hasProjectFile) {
				checkoutAsWithProjectFilePage = new CheckoutWizardCheckoutAsWithProjectFilePage(
						"checkoutAsWithProjectFilePage", //$NON-NLS-1$
						Policy.bind("CheckoutWizardCheckoutAsPage.heading"), //$NON-NLS-1$
						SVNUIPlugin.getPlugin().getImageDescriptor(
								ISVNUIConstants.IMG_WIZBAN_SHARE));
				addPage(checkoutAsWithProjectFilePage);
				if (remoteFolders != null && remoteFolders.length == 1) {
					if (project != null) checkoutAsWithProjectFilePage.setProjectName(project.getName());
				}
			}
			if (remoteFolders == null || !hasProjectFile) {
				checkoutAsWithoutProjectFilePage = new CheckoutWizardCheckoutAsWithoutProjectFilePage(
						"checkoutAsWithoutProjectFilePage", //$NON-NLS-1$
						Policy.bind("CheckoutWizardCheckoutAsPage.heading"), //$NON-NLS-1$
						SVNUIPlugin.getPlugin().getImageDescriptor(
								ISVNUIConstants.IMG_WIZBAN_SHARE));
				addPage(checkoutAsWithoutProjectFilePage);
			}
		}
		projectPage = new CheckoutWizardProjectPage("projectPage", //$NON-NLS-1$
				Policy.bind("CheckoutWizardProjectPage.heading"), //$NON-NLS-1$
				SVNUIPlugin.getPlugin().getImageDescriptor(
						ISVNUIConstants.IMG_WIZBAN_SHARE));
		addPage(projectPage);
	}

	public IWizardPage getNextPage(IWizardPage page) {
		return getNextPage(page, true);
	}

	public IWizardPage getNextPage(IWizardPage page, boolean aboutToShow) {
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
			if (aboutToShow) {
				ISVNRepositoryLocation newLocation = createLocation();
				if (newLocation != null) {
					locationPage.refreshLocations();
					selectionPage.setLocation(newLocation);
				}
			}
			return selectionPage;
		}
		if (page == locationPage) {
			if (locationPage.createNewLocation()) {
				if (repositorySourceProviderPage == null) {
					return createLocationPage;
				}
				else {
					return repositorySourceProviderPage;
				}
			}
			else {
				if (aboutToShow) selectionPage.setLocation(repositoryLocation);
				return selectionPage;
			}
		}
		if (page == createLocationPage) {
			if (aboutToShow) {
				ISVNRepositoryLocation newLocation = createLocation();
				if (newLocation != null) {
					locationPage.refreshLocations();
					selectionPage.setLocation(newLocation);
				}
			}
			return selectionPage;
		}
		if (page == selectionPage) {
			if (aboutToShow) {
				if (remoteFolders.length == 1) {
					BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
						public void run() {
							checkForProjectFile();
							if (hasProjectFile) {
								if (checkoutAsWithProjectFilePage != null) {
									checkoutAsWithProjectFilePage.setText(Policy.bind("CheckoutWizardCheckoutAsPage.single", remoteFolders[0].getName())); //$NON-NLS-1$
									if (project == null) {
										try {
											project = SVNWorkspaceRoot.getProject(remoteFolders[0],null);
										} catch (Exception e) {
											project = SVNWorkspaceRoot.getProject(remoteFolders[0].getName());
										}
									}
									if (project != null) checkoutAsWithProjectFilePage.setProject(project.getName());
								}
							} else {
								if (checkoutAsWithoutProjectFilePage != null) {
									checkoutAsWithoutProjectFilePage.setText(Policy.bind("CheckoutWizardCheckoutAsPage.single", remoteFolders[0].getName())); //$NON-NLS-1$
									IProject project = null;
									try {
										project = SVNWorkspaceRoot.getProject(remoteFolders[0],null);
									} catch (Exception e) {
										project = SVNWorkspaceRoot.getProject(remoteFolders[0].getName());
									}
									checkoutAsWithoutProjectFilePage.setProject(project.getName());
//									checkoutAsWithoutProjectFilePage.setProject(remoteFolders[0].getName());
								}
							}
						}
					});
				} else {
					if (checkoutAsMultiplePage != null) checkoutAsMultiplePage.setText(Policy.bind("CheckoutWizardCheckoutAsPage.multiple", Integer.toString(remoteFolders.length))); //$NON-NLS-1$
				}
			}
			if (remoteFolders.length > 1) return checkoutAsMultiplePage;
			else {
				if (hasProjectFile) return checkoutAsWithProjectFilePage;
				else return checkoutAsWithoutProjectFilePage;
			}
		}
		if (page == checkoutAsWithoutProjectFilePage) {
			if (checkoutAsWithoutProjectFilePage.useWizard()) return null;
			else return projectPage;
		}
		if (page == checkoutAsMultiplePage || page == checkoutAsWithProjectFilePage)
			return projectPage;
		return super.getNextPage(page);
	}

	private ISVNRepositoryLocation createLocation() {
		createLocationPage.finish(new NullProgressMonitor());
		Properties properties = createLocationPage.getProperties();
		if (repositorySourceProviderPage != null) {
			ISVNRepositorySourceProvider selectedRepositorySourceProvider = repositorySourceProviderPage.getSelectedRepositorySourceProvider();
			if (selectedRepositorySourceProvider != null) {
				SVNRepositoryProviderWizardPage wizardPage = wizardPageMap.get(selectedRepositorySourceProvider);
				if (wizardPage != null) {
					properties.setProperty("url", wizardPage.getSelectedUrl()); //$NON-NLS-1$
				}
			}
		}
		String url = properties.getProperty("url");
		if (createdRepositoryUrls.contains(url)) {
			return null;
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
				createdRepositoryUrls.add(url);
			} catch (InterruptedException e) {
				return null;
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
				return null;
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
					return null;
				}
				if (keep) return root[0];
			}
		}
		return root[0];
	}

	private void checkForProjectFile() {

		if(!hasProjectFile && project == null)
		{
			try
			{
				new ProgressMonitorDialog(getShell()).run(true, false, new IRunnableWithProgress()
				{
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
					{

						monitor = Policy.monitorFor(monitor);
						monitor.beginTask("Getting remote project info", 100);
						ISVNRemoteFolder folder = remoteFolders[0];
	//					String url = folder.getUrl().toString() + "/.project"; //$NON-NLS-1$
						try {
//							ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
//							client.getInfo(new SVNUrl(url));
							hasProjectFile = true;
							monitor.worked(50);
							project = SVNWorkspaceRoot.getProject(folder,null);
						} catch (Exception e) {
							hasProjectFile = false;
							project = SVNWorkspaceRoot.getProject(folder.getName());
						}
						finally
						{
							monitor.done();
						}
					}
				});
	     } catch (InterruptedException e) {
	         // operation canceled
	     } catch (InvocationTargetException e) {
	         SVNUIPlugin.openError(getShell(), Policy.bind("exception"), null, e.getCause()); //$NON-NLS-1$	    
	     }
		}
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		if (page == checkoutAsMultiplePage || page == projectPage) {
			return projectPage.isPageComplete();
		}
		if (page == checkoutAsWithoutProjectFilePage) {
			if (checkoutAsWithoutProjectFilePage.useWizard())
				return true;
			else
				return checkoutAsWithoutProjectFilePage.isPageComplete()
						&& projectPage.isPageComplete();
		}
		if (page == checkoutAsWithProjectFilePage) {
			return checkoutAsWithProjectFilePage.isPageComplete()
					&& projectPage.isPageComplete();
		}
		if (page == selectionPage || page instanceof SVNRepositoryProviderWizardPage) {
			return selectionPage.isPageComplete();
		}
		return super.canFinish();
	}

	public void setLocation(ISVNRepositoryLocation repositoryLocation) {
//		selectionPage.setLocation(repositoryLocation);
		this.repositoryLocation = repositoryLocation;
	}

	public boolean performFinish() {
		if (remoteFolders.length == 1) {
			checkForProjectFile();
			boolean useWizard = false;
			if (!hasProjectFile)
				useWizard = checkoutAsWithoutProjectFilePage.useWizard();
			if (useWizard)
				return checkoutUsingWizard();
		}
		if (projectPage.getLocation().equals(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()))
			return checkoutAsProject();
		else
			return checkoutAsProjectInto();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
		projectPage.setLocation();
	}

	public void setRemoteFolders(ISVNRemoteFolder[] remoteFolders) {
		this.remoteFolders = remoteFolders;
		this.hasProjectFile = false;
		this.project = null;
	}

	public ISVNRemoteFolder[] getRemoteFolders() {
		return remoteFolders;
	}

	private boolean checkoutUsingWizard() {
		CheckoutUsingProjectWizardAction checkoutAction = new CheckoutUsingProjectWizardAction(remoteFolders);
		try {
			if (remoteFolders.length == 1) {
				if (hasProjectFile) {
					checkoutAction.setSvnRevision(checkoutAsWithProjectFilePage.getRevision());
					checkoutAction.setDepth(checkoutAsWithProjectFilePage.getDepth());
					checkoutAction.setIgnoreExternals(checkoutAsWithProjectFilePage.isIgnoreExternals());
					checkoutAction.setForce(checkoutAsWithProjectFilePage.isForce());
				} else {
					checkoutAction.setSvnRevision(checkoutAsWithoutProjectFilePage.getRevision());
					checkoutAction.setDepth(checkoutAsWithoutProjectFilePage.getDepth());
					checkoutAction.setIgnoreExternals(checkoutAsWithoutProjectFilePage.isIgnoreExternals());
					checkoutAction.setForce(checkoutAsWithoutProjectFilePage.isForce());
				}
			} else {
				checkoutAction.setSvnRevision(checkoutAsMultiplePage.getRevision());
				checkoutAction.setDepth(checkoutAsMultiplePage.getDepth());
				checkoutAction.setIgnoreExternals(checkoutAsMultiplePage.isIgnoreExternals());
				checkoutAction.setForce(checkoutAsMultiplePage.isForce());
			}
			checkoutAction.execute(null);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy
					.bind("CheckoutAsAction.title"), e.getMessage());
			return false;
		}
		return true;
	}

	private boolean checkoutAsProjectInto() {
		CheckoutIntoAction checkoutAction;
		if (shouldRenameMultipleProjects()) {
		    checkoutAction = new CheckoutIntoAction(remoteFolders, getProjectNamePrefix(), getProjectNameSuffix(), projectPage.getCanonicalLocation(), getShell());
		} else {
		    checkoutAction = new CheckoutIntoAction(remoteFolders, projectName, projectPage.getCanonicalLocation(), getShell());
		}
		try {
			if (remoteFolders.length == 1) {
				if (hasProjectFile) {
					checkoutAction.setSvnRevision(checkoutAsWithProjectFilePage.getRevision());
					checkoutAction.setDepth(checkoutAsWithProjectFilePage.getDepth());
					checkoutAction.setIgnoreExternals(checkoutAsWithProjectFilePage.isIgnoreExternals());
					checkoutAction.setForce(checkoutAsWithProjectFilePage.isForce());
				} else {
					checkoutAction.setSvnRevision(checkoutAsWithoutProjectFilePage.getRevision());
					checkoutAction.setDepth(checkoutAsWithoutProjectFilePage.getDepth());
					checkoutAction.setIgnoreExternals(checkoutAsWithoutProjectFilePage.isIgnoreExternals());
					checkoutAction.setForce(checkoutAsWithoutProjectFilePage.isForce());
				}
			} else {
				checkoutAction.setSvnRevision(checkoutAsMultiplePage.getRevision());
				checkoutAction.setDepth(checkoutAsMultiplePage.getDepth());
				checkoutAction.setIgnoreExternals(checkoutAsMultiplePage.isIgnoreExternals());
				checkoutAction.setForce(checkoutAsMultiplePage.isForce());
			}
			checkoutAction.setWorkingSets(projectPage.getWorkingSets());
			checkoutAction.execute(null);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy
					.bind("CheckoutAsAction.title"), e.getMessage());
			return false;
		}
		return true;
	}

	private boolean checkoutAsProject() {
		CheckoutAsProjectAction checkoutAction;
		if (shouldRenameMultipleProjects()) {
		    checkoutAction = new CheckoutAsProjectAction(remoteFolders, getProjectNamePrefix(), getProjectNameSuffix(), getShell());
		} else {
		    checkoutAction = new CheckoutAsProjectAction(remoteFolders, projectName, getShell());
		}
		try {
			if (remoteFolders.length == 1) {
				if (hasProjectFile) {
					checkoutAction.setSvnRevision(checkoutAsWithProjectFilePage.getRevision());
					checkoutAction.setDepth(checkoutAsWithProjectFilePage.getDepth());
					checkoutAction.setIgnoreExternals(checkoutAsWithProjectFilePage.isIgnoreExternals());
					checkoutAction.setForce(checkoutAsWithProjectFilePage.isForce());
				} else {
					checkoutAction.setSvnRevision(checkoutAsWithoutProjectFilePage.getRevision());
					checkoutAction.setDepth(checkoutAsWithoutProjectFilePage.getDepth());
					checkoutAction.setIgnoreExternals(checkoutAsWithoutProjectFilePage.isIgnoreExternals());
					checkoutAction.setForce(checkoutAsWithoutProjectFilePage.isForce());
				}
			} else {
				checkoutAction.setSvnRevision(checkoutAsMultiplePage.getRevision());
				checkoutAction.setDepth(checkoutAsMultiplePage.getDepth());
				checkoutAction.setIgnoreExternals(checkoutAsMultiplePage.isIgnoreExternals());
				checkoutAction.setForce(checkoutAsMultiplePage.isForce());
			}
			checkoutAction.setWorkingSets(projectPage.getWorkingSets());
			checkoutAction.execute(null);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy
					.bind("CheckoutAsAction.title"), e.getMessage());
			return false;
		}
		return true;
	}

	public String getProjectName() {
		return projectName;
	}

    public String getProjectNamePrefix() {
        return projectNamePrefix;
    }

    public void setProjectNamePrefix(String projectNamePrefix) {
        this.projectNamePrefix = projectNamePrefix;
    }

    public String getProjectNameSuffix() {
        return projectNameSuffix;
    }

    public void setProjectNameSuffix(String projectNameSuffix) {
        this.projectNameSuffix = projectNameSuffix;
    }

    /**
     * @return <code>true</code>, if multiple projects should be checked out and project name
     * prefix and/or suffix is set
     */
    protected boolean shouldRenameMultipleProjects() {
        return ((remoteFolders != null) && (remoteFolders.length > 1)
                && ((getProjectNamePrefix() != null) || (getProjectNameSuffix() != null)));
    }

}
