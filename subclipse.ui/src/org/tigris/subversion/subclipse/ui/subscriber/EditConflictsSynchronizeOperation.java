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
package org.tigris.subversion.subclipse.ui.subscriber;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.conflicts.ConflictsCompareInput;
import org.tigris.subversion.svnclientadapter.utils.Command;

public class EditConflictsSynchronizeOperation extends SVNSynchronizeOperation {

	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;
    
    public EditConflictsSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
        super(configuration, elements);
    }

    protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
        return true;
    }

    protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        IResource[] resources = set.getResources();
        if (resources[0] instanceof IFile) {
            final IFile resource = (IFile)resources[0];
	        run(new WorkspaceModifyOperation() {
	            public void execute(IProgressMonitor monitor)
	                    throws CoreException, InvocationTargetException, InterruptedException {
	
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
        final ConflictsCompareInput fInput = new ConflictsCompareInput(cc);
        fInput.setResources(conflictOldFile, conflictWorkingFile,
                conflictNewFile, (IFile) resource);
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
			    CompareUI.openCompareEditorOnPage(fInput, getPart().getSite().getPage());
			}
		});        
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
    private void editConflictsExternal(final IFile resource, IFile conflictOldFile,
            IFile conflictWorkingFile, IFile conflictNewFile)
            throws CoreException, InvocationTargetException, InterruptedException {

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

            final Command command = new Command(mergeProgramLocation);
            String[] parameters = mergeProgramParameters.split(" ");
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
            
    		getShell().getDisplay().syncExec(new Runnable() {
    			public void run() {
    	            try {
                        command.exec();
                        command.waitFor();
        	            resource.refreshLocal(IResource.DEPTH_ZERO, null);
                    } catch (Exception e) {
                    	SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
                    }
    			}
    		});    
    }
    
	final protected void run(final IRunnableWithProgress runnable, boolean cancelable, int progressKind) throws InvocationTargetException, InterruptedException {
		final Exception[] exceptions = new Exception[] {null};
		
		// Ensure that no repository view refresh happens until after the action
		final IRunnableWithProgress innerRunnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                SVNUIPlugin.getPlugin().getRepositoryManager().run(runnable, monitor);
			}
		};
		
		switch (progressKind) {
			case PROGRESS_BUSYCURSOR :
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					public void run() {
						try {
							innerRunnable.run(new NullProgressMonitor());
						} catch (InvocationTargetException e) {
							exceptions[0] = e;
						} catch (InterruptedException e) {
							exceptions[0] = e;
						}
					}
				});
				break;
			case PROGRESS_DIALOG :
			default :
				new ProgressMonitorDialog(getShell()).run(true, cancelable,/*cancelable, true, */innerRunnable);	
				break;
		}
		if (exceptions[0] != null) {
			if (exceptions[0] instanceof InvocationTargetException)
				throw (InvocationTargetException)exceptions[0];
			else
				throw (InterruptedException)exceptions[0];
		}
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
        
}
