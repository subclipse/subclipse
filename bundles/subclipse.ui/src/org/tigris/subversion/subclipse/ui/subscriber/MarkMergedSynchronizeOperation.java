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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.LocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class MarkMergedSynchronizeOperation extends SVNSynchronizeOperation {
    
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

    public MarkMergedSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
        super(configuration, elements);
    }

    protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
        return true;
    }

    protected void run(SVNTeamProvider provider, SyncInfoSet set, final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        final IResource[] resources = set.getResources();
        run(new WorkspaceModifyOperation() {
            protected void execute(IProgressMonitor mon) throws CoreException, InvocationTargetException, InterruptedException {
               for (int i = 0; i < resources.length; i++) {
                   File tempFile = null;
                   try {
                       tempFile = copyToTempFile(resources[i]);
	                } catch (Exception e) {
	                    SVNUIPlugin.log(e.getMessage());
	                    showErrorMessage(e);
	                    return;
	                }
	                if (monitor.isCanceled()) {
	                	if (tempFile != null) tempFile.delete();
	                	return;
	                }
                    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
					if (svnResource instanceof LocalResource) ((LocalResource)svnResource).revert(false);
					else svnResource.revert();
					new UpdateOperation(getPart(), resources[i], SVNRevision.HEAD).run(monitor);
					if (monitor.isCanceled()) {
						if (tempFile != null) tempFile.delete();
						return;
					}
					File file = new File(resources[i].getLocation().toString());
					try {
                        copy(tempFile, file);
                    } catch (Exception e1) {
                        SVNUIPlugin.log(e1.getMessage());
                        showErrorMessage(e1);
                    }
                    if (tempFile != null) tempFile.delete();
               }
            }          
        }, true /* cancelable */, PROGRESS_BUSYCURSOR);
    }
    
    protected boolean canRunAsJob() {
		return true;
	}

	protected String getJobName() {
		return Policy.bind("SyncAction.markMerged");
	}

	private void showErrorMessage(final Exception e) {
    	Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("SyncAction.markMerged"), e.getMessage()); //$NON-NLS-1$
			}   		
    	});
    }
    
    public File copyToTempFile(IResource resource) throws Exception {
        File tempFile = new File(resource.getLocation() + ".tmp"); //$NON-NLS-1$
        if (tempFile.exists()) tempFile = getTempFile(resource);
        File sourceFile = new File(resource.getLocation().toString());
        copy (sourceFile, tempFile);
        return tempFile;
    }
    
	private File getTempFile(IResource resource) {
	    int count = 1;
	    while (new File(resource.getLocation() + "." + count + ".tmp").exists())  //$NON-NLS-1$ //$NON-NLS-2$
	        count++;
	    File tempFile = new File(resource.getLocation() + "." + count + ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        return tempFile;
    }

    private void copy(File sourceFile, File destFile) throws Exception {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destFile);

            byte[] buffer = new byte[8 * 1024];
            int count = 0;
            do {
                out.write(buffer, 0, count);
                count = in.read(buffer, 0, buffer.length);
            } while (count != -1);
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
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
