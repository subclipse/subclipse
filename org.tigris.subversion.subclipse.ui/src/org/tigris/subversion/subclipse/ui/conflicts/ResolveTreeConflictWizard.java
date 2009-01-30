package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.MergeOperation;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class ResolveTreeConflictWizard extends Wizard {
	private SVNTreeConflict treeConflict;
	private IWorkbenchPart targetPart;
	private ISVNLocalResource svnResource;

	private ResolveTreeConflictWizardMainPage mainPage;
	
	private ISVNClientAdapter svnClient;
	private ISVNStatus[] statuses;
	private boolean copiedToRetrieved;
	private boolean remoteCopiedToRetrieved;
	private ISVNStatus copiedTo;
	private ISVNStatus remoteCopiedTo;
	private ISVNLogMessage[] logMessages;

	public ResolveTreeConflictWizard(SVNTreeConflict treeConflict, IWorkbenchPart targetPart) {
		super();
		this.treeConflict = treeConflict;
		this.targetPart = targetPart;
		svnResource =  SVNWorkspaceRoot.getSVNResourceFor(treeConflict.getResource());
	}
	
	public void addPages() {
		super.addPages();
		setWindowTitle("Resolve Tree Conflict on " + treeConflict.getResource().getName());
		
		mainPage = new ResolveTreeConflictWizardMainPage();
		addPage(mainPage);
	}
	
	public boolean needsProgressMonitor() {
		return true;
	}

	public boolean performFinish() {
		if (mainPage.getMergeFromRepository()) {
			try {
				SVNRevision revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision());
				SVNRevision revision2 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
				IResource mergeTarget = mainPage.getMergeTarget();
				IResource[] resources = { mergeTarget };
				MergeOperation mergeOperation = new MergeOperation(targetPart, resources, svnResource.getUrl(), revision1, svnResource.getUrl(), revision2);
				mergeOperation.setForce(true);
				mergeOperation.setRecurse(false);
				mergeOperation.setIgnoreAncestry(true);
				mergeOperation.run();
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		if (mainPage.getMergeFromWorkingCopy()) {
			System.out.println("Merge " + treeConflict.getResource().getName() + " in working copy into selected working copy resource");
		}
		if (mainPage.getRevertConflictedResource()) {
			try {
				IResource[] revertResources = { treeConflict.getResource() };
				RevertOperation revertOperation = new RevertOperation(targetPart, revertResources);
				revertOperation.run();
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		if (mainPage.getRemoveUnversionedConflictedResource()) {
			try {
				treeConflict.getResource().delete(true, new NullProgressMonitor());
			} catch (CoreException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		if (mainPage.getDeleteSelectedResource()) {
			System.out.println("Delete selected resource");
		}
		if (mainPage.getDeleteConflictedResource()) {
			System.out.println("Delete " + treeConflict.getResource().getName());
		}
		if (mainPage.getRevertSelectedResource()) {
			System.out.println("Revert selected resource");
		}
		if (mainPage.getRemoveUnversionedSelectedResource()) {
			System.out.println("Remove selected resource from working copy");
		}
		if (mainPage.getMarkResolved()) {
			try {
				svnClient = getSvnClient();
				svnClient.resolve(treeConflict.getStatus().getFile(), ISVNConflictResolver.Choice.chooseMerged);
				IResource[] refreshResources = { svnResource.getResource() };
				TreeConflictsView.refresh(refreshResources);
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		return true;
	}
	
	public SVNTreeConflict getTreeConflict() {
		return treeConflict;
	}
	
	public ISVNLocalResource getSvnResource() {
		return svnResource;
	}
	
	public ISVNStatus getLocalCopiedTo() throws SVNException {
		if (copiedTo == null && !copiedToRetrieved) {
			statuses = getStatuses();
			for (int i = 0; i < statuses.length; i++) {
				if (statuses[i].getTextStatus().equals(SVNStatusKind.ADDED)) {
					if (statuses[i].getUrlCopiedFrom() != null) {
						if (statuses[i].getUrlCopiedFrom().toString().equals(svnResource.getUrl().toString())) {
							copiedTo = statuses[i];
							break;
						}
					}
				}
			}
		}
		copiedToRetrieved = true;
		return copiedTo;
	}
	
	public ISVNStatus getRemoteCopiedTo() throws Exception {
		if (remoteCopiedTo == null && !remoteCopiedToRetrieved) {
			remoteCopiedToRetrieved = true;
			logMessages = getLogMessages();
			if (logMessages != null) {
				for (int i = 0; i < logMessages.length; i++) {
					ISVNLogMessageChangePath[] changePaths = logMessages[i].getChangedPaths();
					for (int j = 0; j < changePaths.length; j++) {
						if (changePaths[j].getAction() == 'A') {
							if (svnResource.getUrl().toString().endsWith(changePaths[j].getCopySrcPath())) {
								statuses = getStatuses();
								for (int k = 0; k < statuses.length; k++) {
									if (statuses[k].getUrl().toString().endsWith(changePaths[j].getPath())) {
										remoteCopiedTo = statuses[k];
										return remoteCopiedTo;
									}
								}
							}
						}
					}
				}
			}
		}		
		return remoteCopiedTo;
	}
	
	private ISVNStatus[] getStatuses() throws SVNException {
		if (statuses == null) {
			IProject project = treeConflict.getResource().getProject();
			if (project == null) return new ISVNStatus[0];
			ISVNLocalResource svnProject =  SVNWorkspaceRoot.getSVNResourceFor(project);
			GetStatusCommand command = new GetStatusCommand(svnProject, true, false);
			command.run(new NullProgressMonitor());
			statuses = command.getStatuses();
		}
		return statuses;
	}
	
	private ISVNLogMessage[] getLogMessages() throws Exception {
		if (logMessages == null) {
			svnClient = getSvnClient();
			IProject project = treeConflict.getResource().getProject();
			ISVNLocalResource svnProject =  SVNWorkspaceRoot.getSVNResourceFor(project);
			SVNRevision revision = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
			logMessages = svnClient.getLogMessages(svnProject.getUrl(), revision, revision, true); 
		}
		return logMessages;
	}
	
	private ISVNClientAdapter getSvnClient() throws SVNException {
		if (svnClient == null) {
			svnClient = svnResource.getRepository().getSVNClient();
		}
		return svnClient;
	}

}
