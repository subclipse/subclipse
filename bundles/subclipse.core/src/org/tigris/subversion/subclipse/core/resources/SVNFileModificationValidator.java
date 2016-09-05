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
import java.util.ArrayList;
import java.util.List;

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
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.LockResourcesCommand;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

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
        ReadOnlyFiles readOnlyFiles = processFileArray(files);
        if (readOnlyFiles.size() == 0) return Status.OK_STATUS;
        // of the read-only files, get array of ones which are versioned
	    IFile[] managedFiles = readOnlyFiles.getManaged();
	    if (managedFiles.length > 0) {
	    	// Prompt user to lock files
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
	    	// Run the svn lock command
	    	RepositoryProvider provider = RepositoryProvider.getProvider(managedFiles[0].getProject());
	    	if ((provider != null) && (provider instanceof SVNTeamProvider)) {
	    		SVNTeamProvider svnTeamProvider = (SVNTeamProvider) provider;
	    		LockResourcesCommand command = new LockResourcesCommand(svnTeamProvider.getSVNWorkspaceRoot(), managedFiles, stealLock, comment, false);
	    		try {
	    			command.run(new NullProgressMonitor());
	    		} catch (SVNException e) {
	    			SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
	    			return Status.CANCEL_STATUS;
	    		}
	    	}
        }
	    // Process any unmanaged but read-only files.  For
	    // those we need to prompt the user to flip the read only bit
	    IFile[] unManagedFiles = readOnlyFiles.getUnManaged();
	    if (unManagedFiles.length > 0) {
		    synchronized (this) {
		        if (uiValidator == null) 
		            uiValidator = loadUIValidator();
		    }
		    if (uiValidator != null) {
		        return uiValidator.validateEdit(unManagedFiles, context);
		    }
		    // There was no plugged in validator so fail gracefully
			return getStatus(unManagedFiles); 
	    }
	    return Status.OK_STATUS;
    }

    public IStatus validateSave(IFile file) {
        return Status.OK_STATUS;
    }
    
    
    /**
     * This method processes the file array and separates
     * the read-only files into managed and unmanaged lists.
     */
    private ReadOnlyFiles processFileArray(IFile[] files) {
    	ReadOnlyFiles result = new ReadOnlyFiles();
    	for (IFile file : files) {
			if (isReadOnly(file)) {
	    	    try {
	    	    	ISVNLocalResource resource = SVNWorkspaceRoot.getSVNResourceFor(file);
	    	    	if (resource.isManaged()) {
	    	    		if (hasNeedsLockProperty(resource))
	    	    			result.addManaged(file);
	    	    		else
	    	    			result.addUnManaged(file);
	    	    	}
	    	    	else
	    	    		result.addUnManaged(file);
	            } catch (SVNException e) {
	            	result.addUnManaged(file);
	            }
			}
		}
        return result;
    }


	private boolean hasNeedsLockProperty(ISVNLocalResource resource) {
		try {
			ISVNProperty needsLock = resource.getSvnProperty("svn:needs-lock"); //$NON-NLS-1$
			if (needsLock != null && needsLock.getValue().length() > 0)
				return true;
		} catch (SVNException e) {
			return false;
		}
		return false;
	}

	private boolean isReadOnly(IFile file) {
		if (file == null) return false;
		File fsFile = file.getLocation().toFile();
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
    
    private class ReadOnlyFiles {
    	private List<IFile> managed;
    	private List<IFile> unManaged;
		public ReadOnlyFiles() {
			super();
			managed = new ArrayList<IFile>();
			unManaged = new ArrayList<IFile>();
		}
		public void addManaged(IFile file) {
			managed.add(file);
		}
		public void addUnManaged(IFile file) {
			unManaged.add(file);
		}
		public IFile[] getManaged() {
			return managed.toArray(new IFile[managed.size()]);
		}
		public IFile[] getUnManaged() {
			return unManaged.toArray(new IFile[unManaged.size()]);
		}
		public int size() {
			return managed.size() + unManaged.size();
		}
    }

}
