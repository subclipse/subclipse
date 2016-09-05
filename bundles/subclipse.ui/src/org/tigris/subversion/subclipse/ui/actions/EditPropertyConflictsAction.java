package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.conflicts.EditPropertyConflictsWizard;
import org.tigris.subversion.subclipse.ui.conflicts.PropertyConflict;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class EditPropertyConflictsAction extends WorkbenchWindowAction {
	private ISVNLocalResource svnResource;
	private String conflictSummary;
	private PropertyConflict[] propertyConflicts;
	private ISVNProperty[] remoteProperties;
	private Exception error;

	public EditPropertyConflictsAction() {
		super();
	}
	
    protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
    	error = null;
    	BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
		    	IResource resource = getSelectedResources()[0];
		        svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		        ISVNClientAdapter client = null;
		    	try {
		    		conflictSummary = PropertyConflict.getConflictSummary(svnResource);
					propertyConflicts = PropertyConflict.getPropertyConflicts(svnResource);
					client = svnResource.getRepository().getSVNClient();
					remoteProperties = client.getProperties(svnResource.getUrl(), svnResource.getRevision(), svnResource.getRevision());
				} catch (Exception e) {
					error = e;
				}
		    	finally {
		    		svnResource.getRepository().returnSVNClient(client);
		    	}
			}   		
    	});
    	if (error != null) {
    		handle(error);
    		return;
    	}
    	EditPropertyConflictsWizard wizard = new EditPropertyConflictsWizard(svnResource, conflictSummary, propertyConflicts, remoteProperties, getTargetPart());
    	WizardDialog dialog = new SizePersistedWizardDialog(Display.getDefault().getActiveShell(), wizard, "EditPropertyConflicts"); //$NON-NLS-1$    
    	dialog.open();
    }
	
    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
        try {
        	if (!super.isEnabledForSVNResource(svnResource)) {
        		return false;
        	}
            return svnResource.getStatusFromCache().isPropConflicted();
        } catch (SVNException e) {
            return false;
        }
    }
    
    protected boolean isEnabledForMultipleResources() {
        return false;
    }
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
}
