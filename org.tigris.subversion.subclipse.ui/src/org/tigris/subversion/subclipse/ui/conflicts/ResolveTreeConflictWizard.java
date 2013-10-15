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
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.operations.ResolveOperation;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperationWC;
import org.tigris.subversion.subclipse.ui.wizards.SizePersistedWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ResolveTreeConflictWizard extends Wizard {
	private SVNTreeConflict treeConflict;
	private IWorkbenchPart targetPart;
	private ISVNLocalResource svnResource;

	private ResolveTreeConflictWizardMainPage mainPage;
	
//	private ISVNClientAdapter svnClient;
	private ISVNStatus[] statuses;
	private boolean copiedToRetrieved;
	private boolean remoteCopiedToRetrieved;
	private ISVNStatus copiedTo;
	private ISVNStatus remoteCopiedTo;
	private ISVNLogMessage[] logMessages;
	private boolean added;
	private Exception mergeException;
	private Exception revertException;
	
	private boolean compare;
	
	private File mergePath;
	
	private ISVNClientAdapter svnClient;

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
		setWindowTitle(Messages.ResolveTreeConflictWizard_title + treeConflict.getResource().getName());
		
		mainPage = new ResolveTreeConflictWizardMainPage();
		addPage(mainPage);
	}
	
	public boolean needsProgressMonitor() {
		return true;
	}

	public boolean performFinish() {
		if (mainPage.getReplace()) {
			mergeException = null;
			try {
				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {				
					public void run() {
						try {
							svnClient = svnResource.getRepository().getSVNClient();
							File file = svnResource.getResource().getLocation().toFile();
							svnClient.remove(new File[] { file }, true);
							SVNUrl url = new SVNUrl(treeConflict.getConflictDescriptor().getSrcRightVersion().getReposURL() + "/" + treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos()); //$NON-NLS-1$
							SVNRevision revision;
							int index = treeConflict.getConflictDescriptor().getSrcRightVersion().toString().lastIndexOf("@"); //$NON-NLS-1$
							if (index == -1) {
								revision = SVNRevision.HEAD;
							}
							else {
								long number = Long.parseLong(treeConflict.getConflictDescriptor().getSrcRightVersion().toString().substring(index + 1));
								revision = new SVNRevision.Number(number);
							}
							svnClient.copy(url, file, revision);
						} catch (Exception e) {
							mergeException = e;
						}					
					}
				});
				if (mergeException != null) {
					SVNUIPlugin.log(IStatus.ERROR, mergeException.getMessage(), mergeException);
					MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_2, mergeException.getMessage());
					return false;				
				}
				svnResource.getResource().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_2, e.getMessage());
				return false;
			}
			return true;
		}
		compare = mainPage.getCompare();
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

				svnClient = svnResource.getRepository().getSVNClient();
				mergeException = null;
				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
					public void run() {
						try {
							mergePath = mergeTarget.getLocation().toFile();
							svnClient.merge(url, rev1, url, revision2, mergePath, true, false, false, true);
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
					MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_mergeError, mergeException.getMessage());
					return false;				
				}
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_mergeError, e.getMessage());
				return false;
			}
			finally {
				svnResource.getRepository().returnSVNClient(svnClient);
			}
		}
		if (mainPage.getCompare()) {
			
			if (mainPage.getCompareResource2() == null) {
				ISVNLocalResource svnCompareResource = mainPage.getSvnCompareResource();
				if (svnCompareResource == null) svnCompareResource = svnResource;
				ISVNRemoteResource remoteResource = mainPage.getRemoteResource();
				try {
					
					File file = File.createTempFile("revision", ".diff"); //$NON-NLS-1$ //$NON-NLS-2$
					file.deleteOnExit();
					File path = new File(svnCompareResource.getResource().getLocation().toString());
					SVNUrl toUrl = remoteResource.getUrl();
					SVNRevision toRevision = remoteResource.getRevision();
					
					ShowDifferencesAsUnifiedDiffOperationWC operation = new ShowDifferencesAsUnifiedDiffOperationWC(targetPart, path, toUrl, toRevision, file);						
					SVNLocalCompareInput compareInput = new SVNLocalCompareInput(svnCompareResource, remoteResource);
					compareInput.setDiffOperation(operation);
					
					CompareUI.openCompareEditorOnPage(
							compareInput,
							targetPart.getSite().getPage());
					CompareCloseListener closeListener = new CompareCloseListener(Messages.ResolveTreeConflictWizard_compare + svnCompareResource.getName() + " <workspace>"); //$NON-NLS-1$
					targetPart.getSite().getPage().addPartListener(closeListener);										
				} catch (Exception e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
					MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_compareError, e.getMessage());
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
				CompareCloseListener closeListener = new CompareCloseListener(Messages.ResolveTreeConflictWizard_compare2 + mainPage.getCompareResource1().getName() + "' - '" + mainPage.getCompareResource2().getName() + "')"); //$NON-NLS-1$ //$NON-NLS-2$
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
				MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_revertError, revertException.getMessage());
				return false;					
			}
		}
		if (mainPage.getDeleteResource() != null) {
			try {
				mainPage.getDeleteResource().delete(true, new NullProgressMonitor());
			} catch (CoreException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_deleteError, e.getMessage());
				return false;
			}			
		}
		if (mainPage.getMarkResolved() || mainPage.refreshConflicts()) {
			try {
				if (mainPage.getMarkResolved()) {
					IResource[] resolvedResources = { treeConflict.getResource() };
					ResolveOperation resolveOperation = new ResolveOperation(targetPart, resolvedResources, ISVNConflictResolver.Choice.chooseMerged) {
	
						protected boolean canRunAsJob() {
							return false;
						}
						
					};
					resolveOperation.run();
				}
				
				if (mainPage.refreshConflicts()) {
					IResource[] refreshResources = { svnResource.getResource() };
					TreeConflictsView.refresh(refreshResources);
				}
			} catch (Exception e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				MessageDialog.openError(getShell(), Messages.ResolveTreeConflictWizard_markResolvedError, e.getMessage());
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
	
	public ISVNStatus getLocalCopiedTo(boolean getAll) throws SVNException, SVNClientException {
		String endsWithCheck = treeConflict.getConflictDescriptor().getSrcRightVersion().getPathInRepos();
		IProject project = svnResource.getResource().getProject();
		if (project != null) {
			int index = endsWithCheck.indexOf("/" + project.getName() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			if (index != -1) endsWithCheck = endsWithCheck.substring(index);
		}
		if (copiedTo == null && !copiedToRetrieved) {
			statuses = getStatuses(getAll);
			ISVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
			for (int i = 0; i < statuses.length; i++) {
				if (statuses[i].isCopied() && statuses[i].getTextStatus().equals(SVNStatusKind.ADDED)) {
					ISVNInfo info = svnClient.getInfoFromWorkingCopy(statuses[i].getFile());
					if (info.getCopyUrl() != null) {	
						if ((svnResource.getUrl() != null && info.getCopyUrl().toString().equals(svnResource.getUrl().toString())) || (info.getCopyUrl().toString().endsWith(endsWithCheck))) {
							copiedTo = statuses[i];
							break;
						}
					}
				}
			}
			svnResource.getRepository().returnSVNClient(svnClient);
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
	
	public boolean isCompare() {
		return compare;
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
	
	/**
	 * @return Returns the mergePath.
	 */
	public File getMergePath() {
		return mergePath;
	}

	private ISVNLogMessage[] getLogMessages() throws Exception {
		if (logMessages == null) {
			ISVNClientAdapter svnClient = null;
			try {
				svnClient = svnResource.getRepository().getSVNClient();
				IProject project = treeConflict.getResource().getProject();
				ISVNLocalResource svnProject =  SVNWorkspaceRoot.getSVNResourceFor(project);
				SVNRevision revision1 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcLeftVersion().getPegRevision());
				SVNRevision revision2 = new SVNRevision.Number(treeConflict.getConflictDescriptor().getSrcRightVersion().getPegRevision());
				logMessages = svnClient.getLogMessages(svnProject.getUrl(), revision1, revision2, true); 
			} catch (Exception e) {
				throw e;
			}
			finally {
				svnResource.getRepository().returnSVNClient(svnClient);
			}
		}
		return logMessages;
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
					if (MessageDialog.openQuestion(getShell(), Messages.ResolveTreeConflictWizard_editorClosed, Messages.ResolveTreeConflictWizard_promptToReolve + treeConflict.getResource().getName() + "?")) { //$NON-NLS-1$
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
