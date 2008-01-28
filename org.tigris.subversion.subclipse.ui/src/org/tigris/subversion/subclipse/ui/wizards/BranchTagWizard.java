package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.BranchTagPropertyUpdateDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagWizard extends Wizard {
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
        if (commentPage.getProjectProperties() != null) {
            issue = commentPage.getIssue();
            if (commentPage.getProjectProperties().isWarnIfNoIssue() && (issue == null || issue.length() == 0)) {
                if (!MessageDialog.openQuestion(getShell(), Policy.bind("BranchTagDialog.title"), Policy.bind("BranchTagDialog.0", commentPage.getProjectProperties().getLabel()))) { //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                }
            }
            if (issue != null && issue.length() > 0) {
                String issueError = commentPage.getProjectProperties().validateIssue(issue);
                if (issueError != null) {
                    MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), issueError); //$NON-NLS-1$
                    return false;
                }
            }
        }     
        comment = commentPage.getComment();
        repositoryPage.saveUrl();
        createOnServer = !copyPage.workingCopyButton.getSelection();
        makeParents = repositoryPage.makeParentsButton.getSelection();
        if(commentPage.switchAfterBranchTagCheckBox != null) {
        	switchAfterBranchTag = commentPage.switchAfterBranchTagCheckBox.getSelection();
        }
        
        if (copyPage.serverButton.getSelection()) revision = SVNRevision.HEAD;
        try {
            toUrl = new SVNUrl(repositoryPage.getUrlText());
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
					try {
						SVNProviderPlugin.disableConsoleLogging();
						ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(sourceUrls[0].toString());
						ISVNClientAdapter svnClient = repository.getSVNClient();
						svnInfo = svnClient.getInfo(toUrl);
					} catch (Exception e) {}
					finally { SVNProviderPlugin.enableConsoleLogging(); }
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
	
    private void updateTagsProperty(SVNUrl toUrl) {
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
				ISVNClientAdapter svnClient = repositoryPage.getSvnResource().getRepository().getSVNClient();
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

}
