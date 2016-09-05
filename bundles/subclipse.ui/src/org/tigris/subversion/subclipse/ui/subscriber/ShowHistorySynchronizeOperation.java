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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;


public class ShowHistorySynchronizeOperation extends SVNSynchronizeOperation {
    
    private IResource resource;
    private ISVNRemoteResource remoteResource;
    
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

    public ShowHistorySynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource resource) {
        super(configuration, elements);
        this.resource = resource;
    }
    
    public ShowHistorySynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, ISVNRemoteResource remoteResource) {
        super(configuration, elements);
        this.remoteResource = remoteResource;
    }

    protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
        return true;
    }

    protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						IHistoryView view = (IHistoryView)showView(ISVNUIConstants.HISTORY_VIEW_ID);
						if (view != null) {
							if (resource == null) {
                              view.showHistoryFor(remoteResource); 
                            } else {
                              view.showHistoryFor(resource);
                            }
						}					   
					}
				});   
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
	
	protected IViewPart showView(String viewId) {
		try {
		    return getPart().getSite().getPage().showView(viewId);
		} catch (PartInitException pe) {
			return null;
		}
	}

}
