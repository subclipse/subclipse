package org.tigris.subversion.subclipse.test.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.ui.operations.CheckoutAsProjectOperation;

public class TestCheckoutAsProjectOperation extends CheckoutAsProjectOperation {

    public TestCheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders) {
    	super(part, remoteFolders, localFolders);
    }
     
    public TestCheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders, IPath projectRoot) {
        super(part, remoteFolders, localFolders, projectRoot);
    }

	protected boolean canRunAsJob() {
		return false;
	}	
	
}
