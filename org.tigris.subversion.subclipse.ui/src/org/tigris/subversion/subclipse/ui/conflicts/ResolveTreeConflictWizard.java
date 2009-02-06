package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.internal.CompareAction;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.MergeOperation;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;

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
	private boolean added;

	public ResolveTreeConflictWizard(SVNTreeConflict treeConflict, IWorkbenchPart targetPart) {
		super();
		this.treeConflict = treeConflict;
		this.targetPart = targetPart;
		svnResource =  SVNWorkspaceRoot.getSVNResourceFor(treeConflict.getResource());
		try {
			added = svnResource.isAdded();
		} catch (SVNException e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
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
				SVNUrl url = new SVNUrl(treeConflict.getConflictDescriptor().getSrcRightVersion().getReposURL() + "/" + treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos());
				SVNRevision revision1;
				if (treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() == treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision())
					revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() - 1);
				else
					revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision());
				SVNRevision revision2 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
				if (treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() == treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision())
					revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() - 1);
				IResource mergeTarget = mainPage.getMergeTarget();
				IResource[] resources = { mergeTarget };
				MergeOperation mergeOperation = new MergeOperation(targetPart, resources, url, revision1, url, revision2);
				mergeOperation.setForce(true);
				mergeOperation.setRecurse(false);
				mergeOperation.setIgnoreAncestry(true);
				mergeOperation.run();
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), "Merge Error", e.getMessage());
				return false;
			}
		}
		if (mainPage.getMergeFromWorkingCopy()) {
			ISelection selection = new IStructuredSelection() {
				public Object getFirstElement() {
					return mainPage.getCompareResource1();
				}

				public Iterator iterator() {
					return toList().iterator();
				}

				public int size() {
					return 2;
				}

				public Object[] toArray() {
					IResource[] compareResources = { mainPage.getCompareResource1(), mainPage.getCompareResource2() };
					return compareResources;
				}

				public List toList() {
					List compareList = new ArrayList();
					compareList.add(mainPage.getCompareResource1());
					compareList.add(mainPage.getCompareResource2());
					return compareList;
				}

				public boolean isEmpty() {
					return false;
				}
				
			};
			CompareAction compareAction = new CompareAction();
			compareAction.setActivePart(null, targetPart);
			IAction action = new Action() {				
			};
			compareAction.selectionChanged(action, selection);
			compareAction.run(selection);
			IPartListener2 closeListener = new IPartListener2() {
				
				public void partClosed(IWorkbenchPartReference partRef) {
					String compareName = "Compare ('" + mainPage.getCompareResource1().getName() + "' - '" + mainPage.getCompareResource2().getName() + "')";
					IWorkbenchPart part = partRef.getPart(false);
					if (part instanceof CompareEditor) {
						CompareEditor editor = (CompareEditor)part;
						IEditorInput input = editor.getEditorInput();
						String name = input.getName();
						if (name != null && name.equals(compareName)) {
							if (MessageDialog.openQuestion(getShell(), "Compare Editor Closed", "Do you want to reopen the Resolve Tree Conflict dialog in order to resolve the conflict on " + treeConflict.getResource().getName() + "?")) {
								targetPart.getSite().getPage().removePartListener(this);
								ResolveTreeConflictWizard wizard = new ResolveTreeConflictWizard(treeConflict, targetPart);
								WizardDialog dialog = new SizePersistedWizardDialog(Display.getDefault().getActiveShell(), wizard, "ResolveTreeConflict"); //$NON-NLS-1$
								dialog.open();
							}
						}
					}
				}							
				
				public void partActivated(IWorkbenchPartReference partRef) {}
				public void partBroughtToTop(IWorkbenchPartReference partRef) {}
				public void partDeactivated(IWorkbenchPartReference partRef) {}
				public void partHidden(IWorkbenchPartReference partRef) {}
				public void partInputChanged(IWorkbenchPartReference partRef) {}
				public void partOpened(IWorkbenchPartReference partRef) {}
				public void partVisible(IWorkbenchPartReference partRef) {}
				
			};
			targetPart.getSite().getPage().addPartListener(closeListener);			
		}
		if (mainPage.getRevertResource() != null) {
			try {
				IResource[] revertResources = { mainPage.getRevertResource() };
				RevertOperation revertOperation = new RevertOperation(targetPart, revertResources);
				revertOperation.run();
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), "Revert Error", e.getMessage());
				return false;
			}
		}
		if (mainPage.getDeleteResource() != null) {
			try {
				mainPage.getDeleteResource().delete(true, new NullProgressMonitor());
			} catch (CoreException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), "Delete Error", e.getMessage());
				return false;
			}			
		}
		if (mainPage.getMarkResolved()) {
			try {
				svnClient = getSvnClient();
				svnClient.resolve(treeConflict.getStatus().getFile(), ISVNConflictResolver.Choice.chooseMerged);
				IResource[] refreshResources = { svnResource.getResource() };
				TreeConflictsView.refresh(refreshResources);
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), "Mark Resolved Error", e.getMessage());
				return false;
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
	
	public boolean isAdded() {
		return added;
	}
	
	public ISVNStatus getLocalCopiedTo(boolean getAll) throws SVNException {
		if (copiedTo == null && !copiedToRetrieved) {
			statuses = getStatuses(getAll);
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
	
	public ISVNStatus getRemoteCopiedTo(boolean getAll) throws Exception {
		if (remoteCopiedTo == null && !remoteCopiedToRetrieved) {
			remoteCopiedToRetrieved = true;
			logMessages = getLogMessages();
			if (logMessages != null) {
				for (int i = 0; i < logMessages.length; i++) {
					ISVNLogMessageChangePath[] changePaths = logMessages[i].getChangedPaths();
					for (int j = 0; j < changePaths.length; j++) {
						
						System.out.println(changePaths[j].getAction() + " - " + changePaths[j].getPath());
						System.out.println("copySrcPath: " + changePaths[j].getCopySrcPath());
						
						if (changePaths[j].getAction() == 'A' && changePaths[j].getCopySrcPath() != null) {
							if ((svnResource.getUrl() != null && svnResource.getUrl().toString().endsWith(changePaths[j].getCopySrcPath())) || changePaths[j].getCopySrcPath().endsWith(svnResource.getIResource().getFullPath().toString())) {
								statuses = getStatuses(getAll);
								for (int k = 0; k < statuses.length; k++) {
									if (statuses[k].getUrl() != null && statuses[k].getUrl().toString().endsWith(changePaths[j].getPath())) {
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
	
	public ISVNStatus[] getAdds() throws SVNException {
		List adds = new ArrayList();
		statuses = getStatuses(false);
		for (int i = 0; i < statuses.length; i++) {
			if (statuses[i].getTextStatus().equals(SVNStatusKind.ADDED))
				adds.add(statuses[i]);
		}
		ISVNStatus[] addArray = new ISVNStatus[adds.size()];
		adds.toArray(addArray);
		return addArray;
	}
	
	private ISVNStatus[] getStatuses(boolean getAll) throws SVNException {
		if (statuses == null) {
			IProject project = treeConflict.getResource().getProject();
			if (project == null) return new ISVNStatus[0];
			ISVNLocalResource svnProject =  SVNWorkspaceRoot.getSVNResourceFor(project);
			GetStatusCommand command = new GetStatusCommand(svnProject, true, getAll);
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
			SVNRevision revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision());
			SVNRevision revision2 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
			logMessages = svnClient.getLogMessages(svnProject.getUrl(), revision1, revision2, true); 
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
