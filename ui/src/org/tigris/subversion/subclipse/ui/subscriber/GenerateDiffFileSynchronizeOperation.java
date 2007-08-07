package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.generatediff.GenerateDiffFileWizard;

public class GenerateDiffFileSynchronizeOperation extends SVNSynchronizeOperation {

	private ArrayList unaddedList;
	
	public GenerateDiffFileSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IResource[] resources = set.getResources();
		unaddedList = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
			try {
				if (!svnResource.isManaged() && !svnResource.isIgnored())
					unaddedList.add(resources[i]);
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}
		ArrayList dedupedList = new ArrayList();
		Iterator iter = unaddedList.iterator();
		while (iter.hasNext()) {
			IResource resource = (IResource)iter.next();
			if (!isDupe(resource)) dedupedList.add(resource);
		}
		
		IResource[] unversionedResources = new IResource[dedupedList.size()];
		dedupedList.toArray(unversionedResources);
		GenerateDiffFileWizard wizard = new GenerateDiffFileWizard(new StructuredSelection(resources), unversionedResources);
		wizard.setWindowTitle(Policy.bind("GenerateSVNDiff.title")); //$NON-NLS-1$
		final WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.setMinimumPageSize(350, 250);
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();	
			}
		});		
	}
	
	private boolean isDupe(IResource resource) {
		IResource parent = resource;
		while (parent != null) {
			parent = parent.getParent();
			if (unaddedList.contains(parent)) return true;
		}
		return false;
	}	

}
