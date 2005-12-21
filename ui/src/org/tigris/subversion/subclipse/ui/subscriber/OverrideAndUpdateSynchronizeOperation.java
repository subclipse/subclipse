package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class OverrideAndUpdateSynchronizeOperation extends SVNSynchronizeOperation {
	private IResource[] modifiedResources;
	private IResource[] resources;
	private boolean revertAndUpdate;
	
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

	public OverrideAndUpdateSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] modifiedResources, IResource[] resources) {
		super(configuration, elements);
		this.modifiedResources = modifiedResources;
		this.resources = resources;
	}

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				revertAndUpdate = MessageDialog.openQuestion(getShell(), Policy.bind("SyncAction.override"), Policy.bind("SyncAction.override.confirm"));
			}
		});
		if (!revertAndUpdate) return;
		if (modifiedResources != null && modifiedResources.length > 0) new RevertOperation(getPart(), modifiedResources).run();
		new UpdateOperation(getPart(), resources, SVNRevision.HEAD, true).run();
	}

}
