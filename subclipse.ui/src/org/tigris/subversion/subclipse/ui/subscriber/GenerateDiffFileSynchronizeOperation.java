package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.wizards.WizardDialogWithPersistedLocation;
import org.tigris.subversion.subclipse.ui.wizards.generatediff.GenerateDiffFileWizard;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class GenerateDiffFileSynchronizeOperation extends SVNSynchronizeOperation {

	private ArrayList unaddedList;
	private IResource[] selectedResources;
	
	public GenerateDiffFileSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		SyncInfoSet syncSet = getSyncInfoSet();
		final Map projectSyncInfos = getProjectSyncInfoSetMap(syncSet);
		Iterator iter = projectSyncInfos.keySet().iterator();
		final IProject project = (IProject) iter.next();
		SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(project, SVNUIPlugin.PROVIDER_ID);
		monitor.beginTask(null, projectSyncInfos.size() * 100);
		run(provider, syncSet, Policy.subMonitorFor(monitor,100));
		monitor.done();
	}	

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IResource[] resources = set.getResources();
		HashMap statusMap = new HashMap();
		unaddedList = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
			SyncInfo syncInfo = set.getSyncInfo(resources[i]);
			SVNStatusKind statusKind = null;
			try {
				if (!svnResource.isManaged()) {
					statusKind = SVNStatusKind.UNVERSIONED;
				} else {
					switch (SyncInfo.getChange(syncInfo.getKind())) {
					case SyncInfo.ADDITION:
						statusKind = SVNStatusKind.ADDED;
						break;
					case SyncInfo.DELETION:
						statusKind = SVNStatusKind.DELETED;
						break;
					case SyncInfo.CONFLICTING:
						statusKind = SVNStatusKind.CONFLICTED;
						break;				
					default:
						statusKind = SVNStatusKind.MODIFIED;
						break;
					}
				}
				statusMap.put(resources[i], statusKind);				
				if (!svnResource.isManaged() && !svnResource.isIgnored())
					unaddedList.add(resources[i]);
			} catch (SVNException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
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
		GenerateDiffFileWizard wizard = new GenerateDiffFileWizard(new StructuredSelection(resources), unversionedResources, statusMap);
		wizard.setWindowTitle(Policy.bind("GenerateSVNDiff.title")); //$NON-NLS-1$
		wizard.setSelectedResources(selectedResources);
//		final WizardDialog dialog = new WizardDialog(getShell(), wizard);
//		dialog.setMinimumPageSize(350, 250);
		final WizardDialog dialog = new WizardDialogWithPersistedLocation(getShell(), wizard, "GenerateDiffFileWizard"); //$NON-NLS-1$
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

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}	

}
