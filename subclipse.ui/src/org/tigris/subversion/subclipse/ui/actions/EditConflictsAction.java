/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.ConflictsCompareInput;
import org.tigris.subversion.subclipse.ui.conflicts.MergeFileAssociation;
import org.tigris.subversion.svnclientadapter.utils.Command;

/**
 * Action to edit conflicts
 */
public class EditConflictsAction extends WorkbenchWindowAction {
    private IFile selectedResource;
    
    private Exception exception;
    
    public EditConflictsAction() {
        super();
    }
    
    public EditConflictsAction(IFile selectedResource) {
        this();
        this.selectedResource = selectedResource;
    }

    /**
     * edit the conflicts using built-in merger
     * 
     * @param resource
     * @param conflictOldFile
     * @param conflictWorkingFile
     * @param conflictNewFile
     * @throws InvocationTargetException
     */
    private void editConflictsInternal(IFile resource, IFile conflictOldFile,
            IFile conflictWorkingFile, IFile conflictNewFile)
            throws InvocationTargetException, InterruptedException {
        CompareConfiguration cc = new CompareConfiguration();
        ConflictsCompareInput fInput = new ConflictsCompareInput(cc);
        fInput.setResources(conflictOldFile, conflictWorkingFile,
                conflictNewFile, (IFile) resource);
        CompareUI.openCompareEditorOnPage(fInput, getTargetPage());
    }

    /**
     * edit the conflicts using an external merger
     * 
     * @param resource
     * @param conflictOldFile
     * @param conflictWorkingFile
     * @param conflictNewFile
     * @throws InvocationTargetException
     */
    private void editConflictsExternal(IFile resource, IFile conflictOldFile,
            IFile conflictWorkingFile, IFile conflictNewFile, String mergeProgramLocation, String mergeProgramParameters)
            throws CoreException, InvocationTargetException, InterruptedException {
        try {
            if (mergeProgramLocation.equals("")) { //$NON-NLS-1$
                throw new SVNException(Policy
                        .bind("EditConflictsAction.noMergeProgramConfigured")); //$NON-NLS-1$
            }
            File mergeProgramFile = new File(mergeProgramLocation);
            if (!mergeProgramFile.exists()) {
                throw new SVNException(Policy
                        .bind("EditConflictsAction.mergeProgramDoesNotExist")); //$NON-NLS-1$
            }

            Command command = new Command(mergeProgramLocation);
            String[] parameters = mergeProgramParameters.split(" "); //$NON-NLS-1$
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = replaceParameter(parameters[i], "${theirs}", //$NON-NLS-1$
                        conflictNewFile.getLocation().toFile()
                                .getAbsolutePath());
                parameters[i] = replaceParameter(parameters[i], "${yours}", //$NON-NLS-1$
                        conflictWorkingFile.getLocation().toFile()
                                .getAbsolutePath());
                parameters[i] = replaceParameter(parameters[i], "${base}", //$NON-NLS-1$
                        conflictOldFile.getLocation().toFile()
                                .getAbsolutePath());
                parameters[i] = replaceParameter(parameters[i], "${merged}", //$NON-NLS-1$
                        resource.getLocation().toFile().getAbsolutePath());
            }
            command.setParameters(parameters);
            command.exec();

            command.waitFor();
            resource.refreshLocal(IResource.DEPTH_ZERO, null);
        } catch (IOException e) {
        	throw new SVNException(Policy.bind("EditConflictsAction.1") + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
     */
    protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
        IFile resource;
        if (selectedResource == null)
            resource = (IFile) getSelectedResources()[0];
        else
            resource = selectedResource;
        ISVNLocalResource svnResource = SVNWorkspaceRoot
                .getSVNResourceFor(resource);
        try {
            IFile conflictNewFile = (IFile) File2Resource
                    .getResource(svnResource.getStatus()
                            .getConflictNew());
            IFile conflictOldFile = (IFile) File2Resource
                    .getResource(svnResource.getStatus()
                            .getConflictOld());
            IFile conflictWorkingFile = (IFile) File2Resource
                    .getResource(svnResource.getStatus()
                            .getConflictWorking());
            
            if (conflictWorkingFile == null) {
            	conflictWorkingFile = resource;
            }

            MergeFileAssociation mergeFileAssociation = null;
            try {
				mergeFileAssociation = SVNUIPlugin.getPlugin().getMergeFileAssociation(resource.getName());
			} catch (BackingStoreException e) {
				mergeFileAssociation = new MergeFileAssociation();
			}
			
			if (mergeFileAssociation.getType() == MergeFileAssociation.BUILT_IN) {
                editConflictsInternal(resource, conflictOldFile,
                        conflictWorkingFile, conflictNewFile);						
			}
			else if (mergeFileAssociation.getType() == MergeFileAssociation.DEFAULT_EXTERNAL) {
	            IPreferenceStore preferenceStore = SVNUIPlugin.getPlugin().getPreferenceStore();
	            String mergeProgramLocation = preferenceStore.getString(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION);
	            String mergeProgramParameters = preferenceStore.getString(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS);						
                editConflictsExternal(resource, conflictOldFile,
                        conflictWorkingFile, conflictNewFile, mergeProgramLocation, mergeProgramParameters);						
			} else {
                editConflictsExternal(resource, conflictOldFile,
                        conflictWorkingFile, conflictNewFile, mergeFileAssociation.getMergeProgram(), mergeFileAssociation.getParameters());												
			}
        } catch (Exception e) {
        	exception = e;
        } 
        if (exception != null) {
        	throw new InvocationTargetException(exception);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
     */
    protected String getErrorTitle() {
        return Policy.bind("EditConflictsAction.errorTitle"); //$NON-NLS-1$
    }

    /**
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNResource)
     */
    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
        try {
        	if (!super.isEnabledForSVNResource(svnResource)) {
        		return false;
        	}
            return svnResource.getStatusFromCache().isTextConflicted();
        } catch (SVNException e) {
            return false;
        }
    }

    /**
     * Method isEnabledForAddedResources.
     * 
     * @return boolean
     */
    protected boolean isEnabledForMultipleResources() {
        return false;
    }
    
    private String replaceParameter(String input, String pattern, String value) {
         StringBuffer result = new StringBuffer();
         //startIdx and idxOld delimit various chunks of input; these
         //chunks always end where pattern begins
         int startIdx = 0;
         int idxOld = 0;
         while ((idxOld = input.indexOf(pattern, startIdx)) >= 0) {
           //grab a part of input which does not include pattern
           result.append( input.substring(startIdx, idxOld) );
           //add value to take place of pattern
           result.append( value );

           //reset the startIdx to just after the current match, to see
           //if there are any further matches
           startIdx = idxOld + pattern.length();
         }
         //the final chunk will go to the end of input
         result.append( input.substring(startIdx) );
         return result.toString();
      }

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_EDITCONFLICT;
	}
}