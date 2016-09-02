package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNExternal;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.BranchTagPropertyUpdateDialog;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.LinkList;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagWizard extends Wizard implements IClosableWizard {
    private IResource[] resources;
    private ISVNRemoteResource[] remoteResources;
    private BranchTagWizardRepositoryPage repositoryPage;
    private BranchTagWizardCopyPage copyPage;
    private BranchTagWizardCommentPage commentPage;
    private boolean createOnServer;
    private boolean makeParents;
    private boolean switchAfterBranchTag;
    private String issue;
    private SVNUrl toUrl;
    private SVNRevision revision;
    private Alias newAlias;
    private long revisionNumber = 0;
    private String comment;
    private boolean alreadyExists;
    private boolean sameStructure;
    private ClosableWizardDialog parentDialog;
    private SVNExternal[] svnExternals;

	public BranchTagWizard(IResource[] resources) {
		super();
		this.resources = resources;
		setWindowTitle(Policy.bind("BranchTagDialog.title")); //$NON-NLS-1$
	}
	
	public BranchTagWizard(ISVNRemoteResource[] remoteResources) {
		super();
		this.remoteResources = remoteResources;
		setWindowTitle(Policy.bind("BranchTagDialog.title")); //$NON-NLS-1$
	}	
	
	public void addPages() {
		repositoryPage = new BranchTagWizardRepositoryPage();
		addPage(repositoryPage);
		copyPage = new BranchTagWizardCopyPage();
		copyPage.setRevisionNumber(revisionNumber);
		addPage(copyPage);
		commentPage = new BranchTagWizardCommentPage();
		addPage(commentPage);
	}

	public boolean performFinish() {
		
        if (confirmUserData() == false) {
        	return false;
        }
        
        svnExternals = copyPage.getSvnExternals();
        comment = commentPage.getComment();
        repositoryPage.saveUrl();
        createOnServer = !copyPage.workingCopyButton.getSelection();
        makeParents = repositoryPage.makeParentsButton.getSelection();
        sameStructure = repositoryPage.sameStructureButton != null && repositoryPage.sameStructureButton.getSelection();
        if(commentPage.switchAfterBranchTagCheckBox != null) {
        	switchAfterBranchTag = commentPage.switchAfterBranchTagCheckBox.getSelection();
        }
        
        if (copyPage.serverButton.getSelection()) revision = SVNRevision.HEAD;
        try {
        	toUrl = new SVNUrl(repositoryPage.getToUrl());
            if (copyPage.revisionButton.getSelection()) revision = SVNRevision.getRevision(copyPage.getRevision());
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), e.getMessage()); //$NON-NLS-1$
            return false;
        }
        if (!multipleSelections()) {
        	BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
				public void run() {
					ISVNInfo svnInfo = null;
					SVNUrl[] sourceUrls = getUrls();
					ISVNClientAdapter svnClient = null;
					ISVNRepositoryLocation repository = null;
					try {
						SVNProviderPlugin.disableConsoleLogging();
						repository = SVNProviderPlugin.getPlugin().getRepository(sourceUrls[0].toString());
						svnClient = repository.getSVNClient();
						svnInfo = svnClient.getInfo(toUrl);
					} catch (Exception e) {}
					finally { 
						SVNProviderPlugin.enableConsoleLogging(); 
						if (repository != null) {
							repository.returnSVNClient(svnClient);
						}
					}
					alreadyExists = svnInfo != null;
				}     		
        	});
        	if (alreadyExists) {
                MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), Policy.bind("BranchTagDialog.alreadyExists", toUrl.toString())); //$NON-NLS-1$ //$NON-NLS-2$
                return false;        		
        	}
        }
        if (resources != null) updateTagsProperty(toUrl);		
		return true;
	}
	
	private boolean confirmUserData() {
		
		ProjectProperties projectProperties = commentPage.getProjectProperties();
        if (projectProperties != null)  {
        	int issueCount = 0;
        	if (projectProperties.getMessage() != null) {
        		
        		issue = commentPage.getIssue();
        		if (issue.length() > 0) {
        		    String issueError = projectProperties.validateIssue(issue);
        		    if (issueError != null) {
        		        MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), issueError); //$NON-NLS-1$
        		        return false;
        		    }
        		    else {
        		    	issueCount++;
        		    }
        		}
        	}
        	if (projectProperties.getLogregex() != null) {        		

        		try {
        			LinkList linkList = projectProperties.getLinkList(commentPage.getComment());
        			String[] urls = linkList.getUrls();
        			issueCount += urls.length;

        		} catch (Exception e) {
        			handle(e, null, null);
        		}
        	}
    		if(projectProperties.isWarnIfNoIssue()) {

    			if (issueCount == 0) {
	    			if ((projectProperties.getMessage() != null) && (projectProperties.getLogregex() == null)) {
	        		    if (!MessageDialog.openQuestion(getShell(), Policy.bind("BranchTagDialog.title"), Policy.bind("BranchTagDialog.0", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
	        		        return false; //$NON-NLS-1$
	        		    }	
	    			}
	    			else if ((projectProperties.getMessage() == null) && (projectProperties.getLogregex() != null)) {
	    		        if (!MessageDialog.openQuestion(getShell(), Policy.bind("BranchTagDialog.title"), Policy.bind("BranchTagDialog.1", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
	    		            return false; //$NON-NLS-1$
	    		        }	    				
	    			}
	    			else if ((projectProperties.getMessage() != null) && (projectProperties.getLogregex() != null)) {
	    		        if (!MessageDialog.openQuestion(getShell(), Policy.bind("BranchTagDialog.title"), Policy.bind("BranchTagDialog.2", projectProperties.getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
	    		            return false; //$NON-NLS-1$
	    		        }	    					    				
	    			}
    			}
    		}
        }
		return true;
	}

	
	
    private void updateTagsProperty(SVNUrl toUrl) {
    	ISVNClientAdapter svnClient = null;
    	try {
    		if (resources.length > 1) return;
			ISVNProperty property = null;
			property = repositoryPage.getSvnResource().getSvnProperty("subclipse:tags"); //$NON-NLS-1$
			if (property == null) return;
			newAlias = new Alias();
			newAlias.setBranch(toUrl.toString().toUpperCase().indexOf("TAGS") == -1); //$NON-NLS-1$
			String relativePath = toUrl.toString().substring(repositoryPage.getSvnResource().getRepository().getUrl().toString().length());
			newAlias.setRelativePath(relativePath);
			SVNRevision revision = null;
			if (copyPage.revisionButton.getSelection()) revision = SVNRevision.getRevision(copyPage.getRevision());
			else {
				svnClient = repositoryPage.getSvnResource().getRepository().getSVNClient();
				ISVNInfo svnInfo = svnClient.getInfo(repositoryPage.getUrl());
				revision = SVNRevision.getRevision(svnInfo.getRevision().toString());
			}
			newAlias.setRevision(Integer.parseInt(revision.toString()));
			newAlias.setName(toUrl.getLastPathSegment());
			BranchTagPropertyUpdateDialog dialog = new BranchTagPropertyUpdateDialog(getShell(), getResource(), newAlias, "BranchTagPropertyUpdateDialog"); //$NON-NLS-1$
			if (dialog.open() == BranchTagPropertyUpdateDialog.OK) 
				newAlias = dialog.getNewAlias();
			else
				newAlias = null;
    	} catch (Exception e) {}
    	finally {
    		if (svnClient != null) {
    			repositoryPage.getSvnResource().getRepository().returnSVNClient(svnClient);
    		}
    	}
    }
    
    public boolean multipleSelections() {
    	return (resources != null && resources.length > 1) || (remoteResources != null && remoteResources.length > 1);
    }
    
    public IResource[] getResources() {
    	return resources;
    }
	
	public IResource getResource() {
		if (resources == null || resources.length < 1) return null;
		return resources[0];
	}
	
	public ISVNRemoteResource[] getRemoteResources() {
		return remoteResources;
	}
	
	public ISVNRemoteResource getRemoteResource() {
		if (remoteResources == null || remoteResources.length < 1) return null;
		return remoteResources[0];
	}
	
	public SVNUrl getUrl() {
		return repositoryPage.getUrl();
	}
	
	public SVNUrl[] getUrls() {
		return repositoryPage.getUrls();
	}
	
	public SVNUrl getToUrl() {
		return toUrl;
	}
	
	public String getUrlText() {
		return repositoryPage.getUrlText();
	}
	
	public String getComment() {
		return comment;
	}

	public boolean isCreateOnServer() {
		return createOnServer;
	}

	public SVNRevision getRevision() {
		return revision;
	}

	public boolean isMakeParents() {
		return makeParents;
	}
	
	public boolean isSameStructure() {
		return sameStructure;
	}

	public Alias getNewAlias() {
		return newAlias;
	}
	
	public String getCommonRoot() {
		return repositoryPage.getCommonRoot();
	}

	public boolean isSwitchAfterBranchTag() {
		return switchAfterBranchTag;
	}

	public void setRevisionNumber(long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	public SVNExternal[] getSvnExternals() {
		return svnExternals;
	}

	protected void handle(Exception exception, String title, String message) {
		SVNUIPlugin.openError(getShell(), title, message, exception, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS);
	}

	public void setParentDialog(ClosableWizardDialog parentDialog) {
		this.parentDialog = parentDialog;
	}

	public void finishAndClose() {
    	if (parentDialog != null && parentDialog instanceof ClosableWizardDialog && canFinish()) {
    		((ClosableWizardDialog)parentDialog).finishPressed();
    	}
	}	

}
