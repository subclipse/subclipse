/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 
*******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.AddIgnoredPatternCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.IgnoreResourcesDialog;

/**
 * @author Martin Letenay (letenay at tigris dot org)
 */
public class IgnoreSynchronizeOperation extends SVNSynchronizeOperation {
    private SyncInfoSet syncSet;
    private IResource[] resourcesToIgnore;
    private String[] ignorePatterns;
    private boolean ignore;
    
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

	protected IgnoreSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}
	
	protected SyncInfoSet getSyncInfoSet() {
		if (syncSet == null) {
			syncSet = super.getSyncInfoSet();
			if (!promptForConflictHandling(getShell(), syncSet)) {
				syncSet.clear();
				return syncSet;
			}
			if (!confirmIgnore()) {
			    syncSet.clear();
			    return syncSet;
			}
		}
	    return syncSet;
	}
	
	private boolean confirmIgnore() {
	    ignore = false;
	    resourcesToIgnore = syncSet.getResources();
	    ignorePatterns = new String[resourcesToIgnore.length];
	    if (resourcesToIgnore.length > 0) {
            final IgnoreResourcesDialog dialog = new IgnoreResourcesDialog(getShell(), resourcesToIgnore);
    		getShell().getDisplay().syncExec(new Runnable() {
    			public void run() {
    				ignore = (dialog.open() == IgnoreResourcesDialog.OK);
    			}
    		});
    		if (ignore) 
    		{
    			for (int i = 0; i < resourcesToIgnore.length; i++) {
					ignorePatterns[i] = dialog.getIgnorePatternFor(resourcesToIgnore[i]);
				}
    		}
	    }
	    return ignore;
	}

    protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
        return true;
    }

    protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	    if (ignore) {
		    run(new WorkspaceModifyOperation() {
				public void execute(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						for (int i = 0; i < resourcesToIgnore.length; i++) {
							
							ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resourcesToIgnore[i]);
			                new AddIgnoredPatternCommand(svnResource.getParent(), ignorePatterns[i]).run(monitor);
							
			                resourcesToIgnore[i].getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
						}
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	    }   
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
