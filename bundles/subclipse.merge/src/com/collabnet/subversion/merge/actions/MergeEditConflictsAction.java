/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.WorkspaceAction;
import org.tigris.subversion.subclipse.ui.conflicts.MergeFileAssociation;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.utils.Command;

import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.editors.MergeConflictsCompareInput;

public class MergeEditConflictsAction extends WorkspaceAction {
	private File conflictNewFile;
	private File conflictOldFile;
	private File conflictWorkingFile;
	private File mergedFile;
	private MergeConflictsCompareInput mergeConflictsCompareInput;
	private String fileName;
	private MergeResult mergeResult;
	private SVNConflictDescriptor conflictDescriptor;

	public MergeEditConflictsAction(File conflictNewFile, File conflictOldFile, File conflictWorkingFile, File mergedFile, String fileName, SVNConflictDescriptor conflictDescriptor) {
		super();
		this.conflictNewFile = conflictNewFile;
		this.conflictOldFile = conflictOldFile;
		this.conflictWorkingFile = conflictWorkingFile;
		this.mergedFile = mergedFile;
		this.fileName = fileName;
		this.conflictDescriptor = conflictDescriptor;
	}
	
    private void editConflictsInternal()
            throws InvocationTargetException, InterruptedException {
        CompareConfiguration cc = new CompareConfiguration();
        cc.setLeftEditable(true);
        mergeConflictsCompareInput = new MergeConflictsCompareInput(cc, conflictDescriptor);
        mergeConflictsCompareInput.setMergeResult(mergeResult);
        mergeConflictsCompareInput.setResources(conflictOldFile, conflictWorkingFile,
                conflictNewFile, mergedFile, fileName);
        CompareUI.openCompareEditorOnPage(mergeConflictsCompareInput, getTargetPage());
    }
    
    private void editConflictsExternal(String mergeProgramLocation, String mergeProgramParameters) throws CoreException, InvocationTargetException, InterruptedException {
        try {
        	mergeConflictsCompareInput = new MergeConflictsCompareInput(new CompareConfiguration(), conflictDescriptor);
        	mergeConflictsCompareInput.setMergeResult(mergeResult);
        	
            if (mergeProgramLocation.equals("")) { //$NON-NLS-1$
                throw new SVNException(Messages.MergeEditConflictsAction_noProgram);
            }
            File mergeProgramFile = new File(mergeProgramLocation);
            if (!mergeProgramFile.exists()) {
                throw new SVNException(Messages.MergeEditConflictsAction_programDoesNotExist);
            }

            Command command = new Command(mergeProgramLocation);
            String[] parameters = mergeProgramParameters.split(" "); //$NON-NLS-1$
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = replaceParameter(parameters[i], "${theirs}", //$NON-NLS-1$
                        conflictNewFile.getAbsolutePath());
                parameters[i] = replaceParameter(parameters[i], "${yours}", //$NON-NLS-1$
                        conflictWorkingFile.getAbsolutePath());
                parameters[i] = replaceParameter(parameters[i], "${base}", //$NON-NLS-1$
                        conflictOldFile.getAbsolutePath());
                parameters[i] = replaceParameter(parameters[i], "${merged}", //$NON-NLS-1$
                        mergedFile.getAbsolutePath());
            }
            command.setParameters(parameters);
            command.exec();

            command.waitFor();
            mergeConflictsCompareInput.handleExternalDispose();
        } catch (IOException e) {
        	throw new SVNException(Messages.MergeEditConflictsAction_0 + e.getMessage(), e);
        }
    }    

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
        try {

            MergeFileAssociation mergeFileAssociation = null;
            try {
				mergeFileAssociation = SVNUIPlugin.getPlugin().getMergeFileAssociation(fileName);
			} catch (BackingStoreException e) {
				mergeFileAssociation = new MergeFileAssociation();
			}
			
			if (mergeFileAssociation.getType() == MergeFileAssociation.BUILT_IN) {
                editConflictsInternal();						
			}
			else if (mergeFileAssociation.getType() == MergeFileAssociation.DEFAULT_EXTERNAL) {
	            IPreferenceStore preferenceStore = SVNUIPlugin.getPlugin().getPreferenceStore();
	            String mergeProgramLocation = preferenceStore.getString(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION);
	            String mergeProgramParameters = preferenceStore.getString(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS);						
                editConflictsExternal(mergeProgramLocation, mergeProgramParameters);						
			} else {
                editConflictsExternal(mergeFileAssociation.getMergeProgram(), mergeFileAssociation.getParameters());												
			}        	

        } catch (Exception e) {
        	if (mergeConflictsCompareInput == null) mergeConflictsCompareInput = new MergeConflictsCompareInput(new CompareConfiguration(), conflictDescriptor);
        	mergeConflictsCompareInput.setFinished(true);
            throw new InvocationTargetException(e);
        }
	}
	
    protected String getErrorTitle() {
        return Messages.MergeEditConflictsAction_title;
    }

    protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
        try {
            return conflictWorkingFile != null && svnResource.getStatus().isTextConflicted();
        } catch (SVNException e) {
            return false;
        }
    }

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

	public MergeConflictsCompareInput getMergeConflictsCompareInput() {
		return mergeConflictsCompareInput;
	}

	public void setMergeResult(MergeResult mergeResult) {
		this.mergeResult = mergeResult;
	}    
}
