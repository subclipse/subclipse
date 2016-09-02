package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardUpdatePage;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class UpdateDialogAction extends UpdateAction {
	private long revision;
	private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
	private boolean setDepth = false;

	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
	        IResource[] resources = getSelectedResources();         
	        String pageName;
	        if (resources.length > 1) pageName = "UpdateDialogWithConflictHandling.multiple"; //$NON-NLS-1$	
	        else pageName = "UpdateDialogWithConflictHandling"; //$NON-NLS-1$	        
	        SvnWizardUpdatePage updatePage = new SvnWizardUpdatePage(pageName, resources);
	        updatePage.setDefaultRevision(revision);
	        updatePage.setDepth(depth);
	        updatePage.setSetDepth(setDepth);
	        SvnWizard wizard = new SvnWizard(updatePage);
	        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
	        wizard.setParentDialog(dialog);
	        if (dialog.open() == SvnWizardDialog.OK) {	  
	        	SVNRevision svnRevision = updatePage.getRevision();
	        	UpdateOperation updateOperation = new UpdateOperation(getTargetPart(), resources, svnRevision);
	        	updateOperation.setDepth(updatePage.getDepth());
		    	updateOperation.setSetDepth(updatePage.isSetDepth());
		    	updateOperation.setForce(updatePage.isForce());
		    	updateOperation.setIgnoreExternals(updatePage.isIgnoreExternals());
		    	updateOperation.setCanRunAsJob(canRunAsJob);
		    	updateOperation.setConflictResolver(updatePage.getConflictResolver());
	        	updateOperation.run();
	        }
        } 		
	}
	
	public void setRevision(long revision) {
		this.revision = revision;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setSetDepth(boolean setDepth) {
		this.setDepth = setDepth;
	}
	
}
