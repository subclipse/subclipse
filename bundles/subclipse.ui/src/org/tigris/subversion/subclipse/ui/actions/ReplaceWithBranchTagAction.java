package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.tigris.subversion.subclipse.ui.operations.ReplaceWithRemoteOperation;
import org.tigris.subversion.subclipse.ui.operations.SwitchOperation;
import org.tigris.subversion.subclipse.ui.wizards.ReplaceWithBranchTagWizard;
import org.tigris.subversion.subclipse.ui.wizards.ReplaceWithBranchTagWizardMainPage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardSwitchPage;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ReplaceWithBranchTagAction extends WorkbenchWindowAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();		
		ReplaceWithBranchTagWizard wizard = new ReplaceWithBranchTagWizard(resources);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		if (dialog.open() == WizardDialog.OK) {	
			ReplaceWithBranchTagWizardMainPage mainPage = wizard.getMainPage();
			if (wizard.isReplaceContents()) {				
		    	SVNUrl url = mainPage.getUrl();
		    	SVNRevision svnRevision = mainPage.getRevision();
		    	ReplaceWithRemoteOperation replaceOperation = new ReplaceWithRemoteOperation(getTargetPart(), resources[0], url, svnRevision);
		    	replaceOperation.run();
			}
			else {
				SvnWizardSwitchPage switchPage = wizard.getSwitchPage();
				SVNUrl[] svnUrls;
				SVNRevision svnRevision;
				if (mainPage == null) {
		            svnUrls = switchPage.getUrls();
		            svnRevision = switchPage.getRevision();
				}
				else {
					svnUrls = new SVNUrl[] { mainPage.getUrl() };
		            svnRevision = mainPage.getRevision();
				}
	            SwitchOperation switchOperation = new SwitchOperation(getTargetPart(), resources, svnUrls, svnRevision);
	            switchOperation.setDepth(switchPage.getDepth());
	            switchOperation.setSetDepth(switchPage.isSetDepth());
	            switchOperation.setIgnoreExternals(switchPage.isIgnoreExternals());
	            switchOperation.setForce(switchPage.isForce());
	            switchOperation.setIgnoreAncestry(switchPage.isIgnoreAncestry());
	            switchOperation.setCanRunAsJob(true);
	            switchOperation.setConflictResolver(switchPage.getConflictResolver());
	            switchOperation.run();				
			}
		}
	}
	
	protected boolean isEnabledForManagedResources() {
		return true;
	}
	
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	protected boolean isEnabledForMultipleResources() {
		return true;
	}

}
