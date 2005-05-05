package org.tigris.subversion.subclipse.core.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.LockResourcesCommand;

public class SVNFileModificationValidator implements IFileModificationValidator {

    public IStatus validateEdit(IFile[] files, Object context) {
        String comment = "";
        boolean stealLock = false;
	    SVNTeamProvider svnTeamProvider = null;
	    RepositoryProvider provider = RepositoryProvider.getProvider(files[0].getProject());
	    if ((provider != null) && (provider instanceof SVNTeamProvider)) {
	       if (context != null) {
		       ISVNFileModificationValidatorPrompt svnFileModificationValidatorPrompt = 
		           SVNProviderPlugin.getPlugin().getSvnFileModificationValidatorPrompt();
		       if (svnFileModificationValidatorPrompt != null) {
		           if (!svnFileModificationValidatorPrompt.prompt(context))
		               return SVNStatus.CANCEL_STATUS;
		           comment = svnFileModificationValidatorPrompt.getComment();
		           stealLock = svnFileModificationValidatorPrompt.isStealLock();
		       }
	       }
           svnTeamProvider = (SVNTeamProvider)provider;
           LockResourcesCommand command = new LockResourcesCommand(svnTeamProvider.getSVNWorkspaceRoot(), files, stealLock, comment);
           try { 
               command.run(new NullProgressMonitor());
            } catch (SVNException e) {
                e.printStackTrace();
                return SVNStatus.CANCEL_STATUS;
            }
       }
	    return SVNStatus.OK_STATUS;
    }

    public IStatus validateSave(IFile file) {
        return SVNStatus.OK_STATUS;
    }

}
