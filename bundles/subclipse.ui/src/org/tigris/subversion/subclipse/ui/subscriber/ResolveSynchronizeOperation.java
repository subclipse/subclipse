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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardMarkResolvedPage;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNClientException;

public class ResolveSynchronizeOperation extends SVNSynchronizeOperation {
	boolean propertyConflicts = false;
	boolean textConflicts = false;
	boolean treeConflicts = false;
	private boolean canceled;
	private int selectedResolution;
    
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

    public ResolveSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
        super(configuration, elements);
    }

    protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
        return true;
    }

    protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    	boolean folderSelected = false; 
    	propertyConflicts = false;
    	textConflicts = false;
    	treeConflicts = false;
    	canceled = false;
    	final IResource[] resources = set.getResources();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IContainer) {
				folderSelected = true;
				break;
			}
			if (!propertyConflicts || !textConflicts || !treeConflicts) {
				ISVNLocalResource resource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
				try {
					LocalResourceStatus status = resource.getStatus();
					if (status != null && status.isPropConflicted()) propertyConflicts = true;
					if (status != null && status.isTextConflicted()) textConflicts = true;
					if (status != null && status.hasTreeConflict()) treeConflicts = true;
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}		
		}
		if (folderSelected) {
			selectedResolution = ISVNConflictResolver.Choice.chooseMerged;
		} else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (propertyConflicts && !textConflicts) {
						String message;
						if (resources.length > 1) message = Policy.bind("ResolveAction.confirmMultiple"); //$NON-NLS-1$
						else message = Policy.bind("ResolveAction.confirm", resources[0].getName()); //$NON-NLS-1$
						if (!MessageDialog.openConfirm(getShell(), Policy.bind("ResolveOperation.taskName"), message)) { //$NON-NLS-1$
							canceled = true;
							return;
						}
						selectedResolution = ISVNConflictResolver.Choice.chooseMerged;							
					} else {
						SvnWizardMarkResolvedPage markResolvedPage = new SvnWizardMarkResolvedPage(resources);
						markResolvedPage.setPropertyConflicts(propertyConflicts);
						markResolvedPage.setTreeConflicts(treeConflicts);
						SvnWizard wizard = new SvnWizard(markResolvedPage);
				        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
				        wizard.setParentDialog(dialog);
				        if (dialog.open() == SvnWizardDialog.CANCEL) {
				        	canceled = true;
				        	return;
				        }
				        selectedResolution = markResolvedPage.getResolution();	
					}
				}				
			});
		}
		if (canceled) return;
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException {
				ISVNRepositoryLocation repository = null;
				ISVNClientAdapter svnClient = null;
				try {
					for (int i = 0; i < resources.length; i++) {						
                        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
                        repository = svnResource.getRepository();
                        svnClient = repository.getSVNClient();
                        svnClient.resolve(resources[i].getLocation().toFile(), selectedResolution);
                        repository.returnSVNClient(svnClient);
                        repository = null;
                        svnClient = null;
                        //for some reason, just refreshing the file won't cut it.
                        resources[i].getParent().refreshLocal(IResource.DEPTH_INFINITE, monitor);
					}
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (SVNClientException e) {
					throw new InvocationTargetException(e);
				}
				finally {
					if (repository != null) {
						repository.returnSVNClient(svnClient);
					}
				}
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);        
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

}
