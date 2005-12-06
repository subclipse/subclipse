package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.CheckoutAsProjectAction;
import org.tigris.subversion.subclipse.ui.actions.CheckoutIntoAction;
import org.tigris.subversion.subclipse.ui.actions.CheckoutUsingProjectWizardAction;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class CheckoutWizard extends Wizard implements INewWizard, IImportWizard {
	private CheckoutWizardLocationPage locationPage;

	private ConfigurationWizardMainPage createLocationPage;

	private CheckoutWizardSelectionPage selectionPage;

	private CheckoutWizardCheckoutAsMultiplePage checkoutAsMultiplePage;

	private CheckoutWizardCheckoutAsWithProjectFilePage checkoutAsWithProjectFilePage;

	private CheckoutWizardCheckoutAsWithoutProjectFilePage checkoutAsWithoutProjectFilePage;

	private CheckoutWizardProjectPage projectPage;

	private String projectName;
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
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					checkForProjectFile();
				}				
			});			
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
		if (page == locationPage) {
			if (locationPage.createNewLocation()) return createLocationPage;
			else {
				if (aboutToShow) selectionPage.setLocation(repositoryLocation);
				return selectionPage;
			}
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
									IProject project = SVNWorkspaceRoot.getProject(remoteFolders[0],null);
									checkoutAsWithProjectFilePage.setProject(project.getName());
//									checkoutAsWithProjectFilePage.setProject(remoteFolders[0].getName());
								}
							} else {
								if (checkoutAsWithoutProjectFilePage != null) {
									checkoutAsWithoutProjectFilePage.setText(Policy.bind("CheckoutWizardCheckoutAsPage.single", remoteFolders[0].getName())); //$NON-NLS-1$
									IProject project = SVNWorkspaceRoot.getProject(remoteFolders[0],null);
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

	private void checkForProjectFile() {
		ISVNRemoteFolder folder = remoteFolders[0];
		String url = folder.getUrl().toString() + "/.project"; //$NON-NLS-1$ 
		try {
			ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
			client.getInfo(new SVNUrl(url));
			hasProjectFile = true;
		} catch (Exception e) {
			hasProjectFile = false;
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
		if (page == selectionPage) {
			return projectPage.isPageComplete();
		}
		return super.canFinish();
	}

	public void setLocation(ISVNRepositoryLocation repositoryLocation) {
//		selectionPage.setLocation(repositoryLocation);
		this.repositoryLocation = repositoryLocation;
	}

	public boolean performFinish() {
		if (remoteFolders.length == 1) {
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					checkForProjectFile();
				}				
			});
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
	}

	public ISVNRemoteFolder[] getRemoteFolders() {
		return remoteFolders;
	}

	private boolean checkoutUsingWizard() {
		CheckoutUsingProjectWizardAction checkoutAction = new CheckoutUsingProjectWizardAction(remoteFolders);
		try {
			checkoutAction.execute(null);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy
					.bind("CheckoutAsAction.title"), e.getMessage());
			return false;
		}
		return true;
	}

	private boolean checkoutAsProjectInto() {
		CheckoutIntoAction checkoutAction = new CheckoutIntoAction(remoteFolders, projectName, projectPage.getLocation(), getShell());
		try {
			checkoutAction.execute(null);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy
					.bind("CheckoutAsAction.title"), e.getMessage());
			return false;
		}
		return true;
	}
	
	private boolean checkoutAsProject() {
		CheckoutAsProjectAction checkoutAction = new CheckoutAsProjectAction(remoteFolders, projectName, getShell());
		try {
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

}
