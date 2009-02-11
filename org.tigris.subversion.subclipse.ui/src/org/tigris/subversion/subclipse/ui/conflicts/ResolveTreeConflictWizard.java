package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.CompareUI;
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
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.commands.RevertResourcesCommand;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
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
	private Exception mergeException;
	private Exception revertException;

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
				final SVNUrl url = new SVNUrl(mainPage.getMergeFromUrl());
				SVNRevision revision1;
				if (treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() == treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision())
					revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() - 1);
				else
					revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision());
				final SVNRevision revision2 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
				if (treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() == treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision())
					revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision() - 1);
				final IResource mergeTarget = mainPage.getMergeTarget();
				final SVNRevision rev1 = revision1;

				svnClient = getSvnClient();
				mergeException = null;
				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
					public void run() {
						try {
							File localPath = mergeTarget.getLocation().toFile();
							svnClient.merge(url, rev1, url, revision2, localPath, true, false, false, true);
				            try {
				                // Refresh the resource after merge
				            	if (mergeTarget.getParent() != null)
				            		mergeTarget.getParent().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				            	else
				            		mergeTarget.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				            } catch (CoreException e1) {}							
						} catch (Exception e) {
							mergeException = e;
						}						
					}				
				});			
				if (mergeException != null) {
					SVNUIPlugin.log(IStatus.ERROR, mergeException.getMessage(), mergeException);
					MessageDialog.openError(getShell(), "Merge Error", mergeException.getMessage());
					return false;				
				}
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), "Merge Error", e.getMessage());
				return false;
			}
		}
		if (mainPage.getCompare()) {
			
			if (mainPage.getCompareResource2() == null) {
				ISVNLocalResource svnCompareResource = mainPage.getSvnCompareResource();
				if (svnCompareResource == null) svnCompareResource = svnResource;
				ISVNRemoteResource remoteResource = mainPage.getRemoteResource();
				try {
					CompareUI.openCompareEditorOnPage(
							new SVNLocalCompareInput(svnCompareResource, remoteResource),
							targetPart.getSite().getPage());
					CompareCloseListener closeListener = new CompareCloseListener("Compare " + svnCompareResource.getName() + " <workspace>");
					targetPart.getSite().getPage().addPartListener(closeListener);										
				} catch (SVNException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
					MessageDialog.openError(getShell(), "Compare Error", e.getMessage());
					return false;
				}
			} else {
			
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
				CompareCloseListener closeListener = new CompareCloseListener("Compare ('" + mainPage.getCompareResource1().getName() + "' - '" + mainPage.getCompareResource2().getName() + "')");
				targetPart.getSite().getPage().addPartListener(closeListener);
			}
		}
		if (mainPage.getRevertResource() != null) {				
			revertException = null;
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					try {
						IResource[] revertResources = { mainPage.getRevertResource() };
						RevertResourcesCommand revertCommand = new RevertResourcesCommand(svnResource.getWorkspaceRoot(), revertResources);
						revertCommand.run(new NullProgressMonitor());							
					} catch (Exception e) {
						revertException = e;
					}					
				}					
			});	
			if (revertException != null) {
				SVNUIPlugin.log(IStatus.ERROR, revertException.getMessage(), revertException);
				MessageDialog.openError(getShell(), "Revert Error", revertException.getMessage());
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
						if ((svnResource.getUrl() != null && statuses[i].getUrlCopiedFrom().toString().equals(svnResource.getUrl().toString())) || (statuses[i].getUrlCopiedFrom().toString().endsWith(treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos()))) {
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
	
	class CompareCloseListener implements IPartListener2 {
		private String compareName;
		
		public CompareCloseListener(String compareName) {
			this.compareName = compareName;
		}
		
		public void partClosed(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof CompareEditor) {
				CompareEditor editor = (CompareEditor)part;
				IEditorInput input = editor.getEditorInput();
				String name = input.getName();
				if (name != null && name.startsWith(compareName)) {
					targetPart.getSite().getPage().removePartListener(this);
					if (MessageDialog.openQuestion(getShell(), "Compare Editor Closed", "Do you want to reopen the Resolve Tree Conflict dialog in order to resolve the conflict on " + treeConflict.getResource().getName() + "?")) {
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
	}

}
