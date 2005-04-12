/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.ConflictsCompareInput;
import org.tigris.subversion.svnclientadapter.utils.Command;

/**
 * Action to edit conflicts
 */
public class EditConflictsAction extends WorkspaceAction {
    private IFile selectedResource;
    
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
            IFile conflictWorkingFile, IFile conflictNewFile)
            throws CoreException, InvocationTargetException, InterruptedException {
        try {
            IPreferenceStore preferenceStore = SVNUIPlugin.getPlugin()
                    .getPreferenceStore();
            String mergeProgramLocation = preferenceStore
                    .getString(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION);
            String mergeProgramParameters = preferenceStore
                    .getString(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS);

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
            String[] parameters = StringUtils
                    .split(mergeProgramParameters, ' ');
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = StringUtils.replace(parameters[i], "${theirs}", //$NON-NLS-1$
                        conflictNewFile.getLocation().toFile()
                                .getAbsolutePath());
                parameters[i] = StringUtils.replace(parameters[i], "${yours}", //$NON-NLS-1$
                        conflictWorkingFile.getLocation().toFile()
                                .getAbsolutePath());
                parameters[i] = StringUtils.replace(parameters[i], "${base}", //$NON-NLS-1$
                        conflictOldFile.getLocation().toFile()
                                .getAbsolutePath());
                parameters[i] = StringUtils.replace(parameters[i], "${merged}", //$NON-NLS-1$
                        resource.getLocation().toFile().getAbsolutePath());
            }
            command.setParameters(parameters);
            command.exec();

            command.waitFor();
            resource.refreshLocal(IResource.DEPTH_ZERO, null);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
     */
    protected void execute(final IAction action)
            throws InvocationTargetException, InterruptedException {

        run(new WorkspaceModifyOperation() {
            public void execute(IProgressMonitor monitor)
                    throws CoreException, InvocationTargetException, InterruptedException {
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
                                    .getFileConflictNew());
                    IFile conflictOldFile = (IFile) File2Resource
                            .getResource(svnResource.getStatus()
                                    .getFileConflictOld());
                    IFile conflictWorkingFile = (IFile) File2Resource
                            .getResource(svnResource.getStatus()
                                    .getFileConflictWorking());

                    IPreferenceStore preferenceStore = SVNUIPlugin.getPlugin()
                            .getPreferenceStore();
                    if (preferenceStore
                            .getBoolean(ISVNUIConstants.PREF_MERGE_USE_EXTERNAL)) {
                        editConflictsExternal(resource, conflictOldFile,
                                conflictWorkingFile, conflictNewFile);
                    } else {
                        editConflictsInternal(resource, conflictOldFile,
                                conflictWorkingFile, conflictNewFile);
                    }

                } catch (SVNException e) {
                    throw new InvocationTargetException(e);
                }
            }

        }, false /* cancelable */, PROGRESS_BUSYCURSOR);
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
            return svnResource.getStatus().isTextConflicted();
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

}