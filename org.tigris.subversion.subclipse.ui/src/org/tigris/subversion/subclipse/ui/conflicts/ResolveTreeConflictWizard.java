package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class ResolveTreeConflictWizard extends Wizard {
	private SVNTreeConflict treeConflict;
	private IWorkbenchPart targetPart;
	private ISVNLocalResource svnResource;

	private ResolveTreeConflictWizardMainPage mainPage;
	
	private ISVNStatus[] statuses;
	private boolean copiedToRetrieved;
	private ISVNStatus copiedTo;

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
			System.out.println("Revert " + treeConflict.getResource().getName());
		}
		if (mainPage.getRemoveUnversionedConflictedResource()) {
			System.out.println("Remove " + treeConflict.getResource().getName() + " from working copy");
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
				ISVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
				svnClient.resolve(treeConflict.getStatus().getFile(), ISVNConflictResolver.Choice.chooseMerged);
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		return true;
	}
	
	public SVNTreeConflict getTreeConflict() {
		return treeConflict;
	}
	
	public ISVNStatus getCopiedTo() throws SVNException {
		if (copiedTo == null && !copiedToRetrieved) {
			statuses = getStatuses();
			for (int i = 0; i < statuses.length; i++) {
				if (statuses[i].getTextStatus().equals(SVNStatusKind.ADDED)) {
					if (statuses[i].getUrlCopiedFrom() != null) {
						if (statuses[i].getUrlCopiedFrom().toString().equals(svnResource.getUrl().toString())) {
							copiedTo = statuses[i];
						}
					}
				}
			}
		}
		copiedToRetrieved = true;
		return copiedTo;
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

}
