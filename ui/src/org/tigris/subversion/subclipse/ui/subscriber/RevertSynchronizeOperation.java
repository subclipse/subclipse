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
import org.tigris.subversion.subclipse.core.resources.LocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.RevertDialog;

public class RevertSynchronizeOperation extends SVNSynchronizeOperation {
    private SyncInfoSet syncSet;
    private IResource[] resourcesToRevert;
    private String url;
    private boolean revert;
    
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

	protected RevertSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, String url) {
		super(configuration, elements);
		this.url = url;
	}
	
	protected SyncInfoSet getSyncInfoSet() {
		if (syncSet == null) {
			syncSet = super.getSyncInfoSet();
			if (!promptForConflictHandling(getShell(), syncSet)) {
				syncSet.clear();
				return syncSet;
			}
			if (!confirmRevert()) {
			    syncSet.clear();
			    return syncSet;
			}
		}
	    return syncSet;
	}
	
	private boolean confirmRevert() {
	    revert = false;
	    IResource[] modified = syncSet.getResources();
	    if (modified.length > 0) {
	        final RevertDialog dialog = new RevertDialog(getShell(), modified, url);
    		getShell().getDisplay().syncExec(new Runnable() {
    			public void run() {
    				revert = (dialog.open() == RevertDialog.OK);
    			}
    		});
    		if (revert) resourcesToRevert = dialog.getSelectedResources(); 
	    }
	    return revert;
	}

    protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
        return true;
    }

    protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	    if (revert) {
		    run(new WorkspaceModifyOperation() {
				public void execute(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						for (int i = 0; i < resourcesToRevert.length; i++) {
							
							ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resourcesToRevert[i]);
							if (svnResource instanceof LocalResource) ((LocalResource)svnResource).revert(false);
							else svnResource.revert();
							
							// Revert on a file can also be used to resolve a merge conflict
							if (resourcesToRevert[i].getType() == IResource.FILE) {
								resourcesToRevert[i].getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
							} else {
								resourcesToRevert[i].refreshLocal(IResource.DEPTH_INFINITE, monitor);
							}
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
