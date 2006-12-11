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
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.LockResourcesCommand;

public class SVNFileModificationValidator implements IFileModificationValidator {
	
	/*
	 * A validator plugged in the the Team UI that will prompt
	 * the user to make read-only files writtable. In the absense of
	 * this validator, edit/save fail on read-only files.
	 */
	private IFileModificationValidator uiValidator;
	
	// The id of the core team plug-in
	private static final String ID = "org.eclipse.team.core"; //$NON-NLS-1$
	// The id of the default file modification vaidator extension point
	private static final String DEFAULT_FILE_MODIFICATION_VALIDATOR_EXTENSION = "defaultFileModificationValidator"; //$NON-NLS-1$

    public IStatus validateEdit(IFile[] files, Object context) {
        String comment = "";
        boolean stealLock = false;
        // reduce the array to just read only files
        IFile[] readOnlyFiles = getReadOnly(files);
        int managedCount = readOnlyFiles.length; 
	    SVNTeamProvider svnTeamProvider = null;
	    RepositoryProvider provider = RepositoryProvider.getProvider(files[0].getProject());
	    if ((provider != null) && (provider instanceof SVNTeamProvider)) {
            IFile[] managedFiles = checkManaged(files);
            managedCount = managedFiles.length;
            if (managedCount > 0) {
                if (context != null) {
                    ISVNFileModificationValidatorPrompt svnFileModificationValidatorPrompt = 
                        SVNProviderPlugin.getPlugin().getSvnFileModificationValidatorPrompt();
                    if (svnFileModificationValidatorPrompt != null) {
                        if (!svnFileModificationValidatorPrompt.prompt(managedFiles, context))
                            return Status.CANCEL_STATUS;
                        comment = svnFileModificationValidatorPrompt.getComment();
                        stealLock = svnFileModificationValidatorPrompt.isStealLock();
                    }
                }
                svnTeamProvider = (SVNTeamProvider) provider;
                LockResourcesCommand command = new LockResourcesCommand(svnTeamProvider.getSVNWorkspaceRoot(), managedFiles, stealLock, comment);
                try {
                    command.run(new NullProgressMonitor());
                } catch (SVNException e) {
                    e.printStackTrace();
                    return Status.CANCEL_STATUS;
                }
            }
        }
	    // This is to prompt the user to flip the read only bit
	    // on files that are not managed by SVN
	    if (readOnlyFiles.length > managedCount) {
		    synchronized (this) {
		        if (uiValidator == null) 
		            uiValidator = loadUIValidator();
		    }
		    if (uiValidator != null) {
		        return uiValidator.validateEdit(files, context);
		    }
		    // There was no plugged in validator so fail gracefully
			return getStatus(files); 
	    }
	    return Status.OK_STATUS;
    }

    public IStatus validateSave(IFile file) {
        return Status.OK_STATUS;
    }
    
    
    /**
     * This method does a second check on the files in the array
     * to verify tey are managed.  
     */
    private IFile[] checkManaged(IFile[] files) {
        List result = new ArrayList(files.length);
        for (int i = 0; i < files.length; i++) {
    	    try {
    	    	if (SVNWorkspaceRoot.getSVNResourceFor(files[i]).isManaged()) {
	                    result.add(files[i]);
    	    	}
            } catch (SVNException e) {
            }
        }
        return (IFile[]) result.toArray(new IFile[result.size()]);
    }

    private IFile[] getReadOnly(IFile[] files) {
        List result = new ArrayList(files.length);
        for (int i = 0; i < files.length; i++) {
            if (isReadOnly(files[i])) {
                result.add(files[i]);
            }
        }
        return (IFile[]) result.toArray(new IFile[result.size()]);
    }

	private boolean isReadOnly(IFile file) {
		if (file == null) return false;
		File fsFile = file.getFullPath().toFile();
		if (fsFile == null || fsFile.canWrite())
			return false;
		else
			return true;
	}

	private IStatus getDefaultStatus(IFile file) {
		return 
		    isReadOnly(file)
			? new Status(IStatus.ERROR, SVNProviderPlugin.ID, IResourceStatus.READ_ONLY_LOCAL, Policy.bind("FileModificationValidator.fileIsReadOnly", new String[] { file.getFullPath().toString() }), null) 
				: Status.OK_STATUS;
	}

    protected IStatus getStatus(IFile[] files) {
        if (files.length == 1) {
			return getDefaultStatus(files[0]);
		}
		
		IStatus[] stati = new Status[files.length];
		boolean allOK = true;
		
		for (int i = 0; i < files.length; i++) {
			stati[i] = getDefaultStatus(files[i]);	
			if(! stati[i].isOK())
				allOK = false;
		}
		
		return new MultiStatus(SVNProviderPlugin.ID,
			0, stati,
			allOK
					? Policy.bind("ok")
					: Policy.bind("FileModificationValidator.someReadOnly"),
			null);
    }
	
    private IFileModificationValidator loadUIValidator() {
        IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(ID, DEFAULT_FILE_MODIFICATION_VALIDATOR_EXTENSION);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			if (extensions.length > 0) {
				IConfigurationElement[] configElements = extensions[0].getConfigurationElements();
				if (configElements.length > 0) {
					try {
                        Object o = configElements[0].createExecutableExtension("class"); //$NON-NLS-1$
                        if (o instanceof IFileModificationValidator) {
                            return (IFileModificationValidator)o;
                        }
                    } catch (CoreException e) {
                        SVNProviderPlugin.log(e.getStatus().getSeverity(), e.getMessage(), e);
                    }
				}
			}
		}
		return null;
    }

}
