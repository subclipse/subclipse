/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.sync;

 
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.sync.ILocalSyncElement;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.core.sync.RemoteSyncElement;
import org.eclipse.team.internal.ui.sync.CatchupReleaseViewer;
import org.eclipse.team.internal.ui.sync.ITeamNode;
import org.eclipse.ui.help.WorkbenchHelp;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.decorator.SVNLightweightDecorator;

public class SVNCatchupReleaseViewer extends CatchupReleaseViewer {
//	// Actions
//	private UpdateSyncAction updateAction;
//	private ForceUpdateSyncAction forceUpdateAction;
//	
//	private CommitSyncAction commitAction;
//	private ForceCommitSyncAction forceCommitAction;
//	
//	private UpdateMergeAction updateMergeAction;
//	private UpdateWithForcedJoinAction updateWithJoinAction;
//	private OverrideUpdateMergeAction forceUpdateMergeAction;
//
//	private IgnoreAction ignoreAction;
//	private HistoryAction showInHistory;
//
//	private Action confirmMerge;
//	private AddSyncAction addAction;
//	
//	private Action selectAdditions;
	private Image conflictImage;
	
	private static class DiffOverlayIcon extends OverlayIcon {
		private static final int HEIGHT = 16;
		private static final int WIDTH = 22;
		public DiffOverlayIcon(Image baseImage, ImageDescriptor[] overlays, int[] locations) {
			super(baseImage, overlays, locations, new Point(WIDTH, HEIGHT));
		}
		protected void drawOverlays(ImageDescriptor[] overlays, int[] locations) {
			Point size = getSize();
			for (int i = 0; i < overlays.length; i++) {
				ImageDescriptor overlay = overlays[i];
				ImageData overlayData = overlay.getImageData();
				switch (locations[i]) {
					case TOP_LEFT:
						drawImage(overlayData, 0, 0);			
						break;
					case TOP_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, 0);			
						break;
					case BOTTOM_LEFT:
						drawImage(overlayData, 0, size.y - overlayData.height);			
						break;
					case BOTTOM_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, size.y - overlayData.height);			
						break;
				}
			}
		}
	}
	
//	private static class HistoryAction extends Action implements ISelectionChangedListener {
//		IStructuredSelection selection;
//		public HistoryAction(String label) {
//			super(label);
//		}
//		public void run() {
//			if (selection.isEmpty()) {
//				return;
//			}
//			HistoryView view = HistoryView.openInActivePerspective();
//			if (view == null) {
//				return;
//			}
//			ITeamNode node = (ITeamNode)selection.getFirstElement();
//			IRemoteSyncElement remoteSyncElement = ((TeamFile)node).getMergeResource().getSyncElement();
//			ISVNRemoteFile remoteFile = (ISVNRemoteFile)remoteSyncElement.getRemote();
//			IResource local = remoteSyncElement.getLocal();
//			ISVNRemoteFile baseFile = (ISVNRemoteFile)remoteSyncElement.getBase();
//			
//			// can only show history if remote exists or local has a base.
//			String currentRevision = null;
//			try {
//				currentRevision = baseFile != null ? baseFile.getRevision(): null;
//			} catch(TeamException e) {
//				SVNUIPlugin.log(e.getStatus());
//			}
//			if (remoteFile != null) {
//				view.showHistory(remoteFile, currentRevision);
//			} else if (baseFile != null) {
//				view.showHistory(baseFile, currentRevision);
//			}
//		}
//		public void selectionChanged(SelectionChangedEvent event) {
//			ISelection selection = event.getSelection();
//			if (!(selection instanceof IStructuredSelection)) {
//				setEnabled(false);
//				return;
//			}
//			IStructuredSelection ss = (IStructuredSelection)selection;
//			if (ss.size() != 1) {
//				setEnabled(false);
//				return;
//			}
//			ITeamNode first = (ITeamNode)ss.getFirstElement();
//			if (first instanceof TeamFile) {
//				// can only show history on elements that have a remote file
//				this.selection = ss;
//				IRemoteSyncElement remoteSyncElement = ((TeamFile)first).getMergeResource().getSyncElement();
//				if(remoteSyncElement.getRemote() != null || remoteSyncElement.getBase() != null) {
//					setEnabled(true);
//				} else {
//					setEnabled(false);
//				}
//			} else {
//				this.selection = null;
//				setEnabled(false);
//			}
//		}
//	}
//	
	public SVNCatchupReleaseViewer(Composite parent, SVNSyncCompareInput model) {
		super(parent, model);
		initializeActions(model);
		initializeLabelProvider();
		// set F1 help
		WorkbenchHelp.setHelp(this.getControl(), IHelpContextIds.CATCHUP_RELEASE_VIEWER);
	}
	
	private static class Decoration implements IDecoration {
		public String prefix, suffix;
		public ImageDescriptor overlay;

		/**
		 * @see org.eclipse.jface.viewers.IDecoration#addPrefix(java.lang.String)
		 */
		public void addPrefix(String prefix) {
			this.prefix = prefix;
		}
		/**
		 * @see org.eclipse.jface.viewers.IDecoration#addSuffix(java.lang.String)
		 */
		public void addSuffix(String suffix) {
			this.suffix = suffix;
		}
		/**
		 * @see org.eclipse.jface.viewers.IDecoration#addOverlay(org.eclipse.jface.resource.ImageDescriptor)
		 */
		public void addOverlay(ImageDescriptor overlay) {
			this.overlay = overlay;
		}
	}
	
//	private Image getConflictImage() {
//		if(conflictImage != null)
//			return conflictImage;
//		final ImageDescriptor conflictDescriptor = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MERGEABLE_CONFLICT);
//		conflictImage = conflictDescriptor.createImage();
//		return conflictImage;
//	}
//		
//
	private void initializeLabelProvider() {
		final LabelProvider oldProvider = (LabelProvider)getLabelProvider();
		
		
		setLabelProvider(new LabelProvider() {
			private OverlayIconCache iconCache = new OverlayIconCache();
			
			public void dispose() {
				iconCache.disposeAll();
				oldProvider.dispose();
				if(conflictImage != null)	
					conflictImage.dispose();
			}
			
			public Image getImage(Object element) {
				Image image = oldProvider.getImage(element);

				if (! (element instanceof ITeamNode))
					return image;
				
				ITeamNode node = (ITeamNode)element;
				IResource resource = node.getResource();

				if (! resource.exists())
					return image;
					
				SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
				List overlays = new ArrayList();
				List locations = new ArrayList();
				
				// use the default svn image decorations
				ImageDescriptor resourceOverlay = SVNLightweightDecorator.getOverlay(node.getResource(),false, provider);
				
				int kind = node.getKind();
				boolean conflict = (kind & IRemoteSyncElement.AUTOMERGE_CONFLICT) != 0;

				if(resourceOverlay != null) {
					overlays.add(resourceOverlay);
					locations.add(new Integer(OverlayIcon.BOTTOM_RIGHT));
				}
				
				if(conflict) {
					overlays.add(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MERGEABLE_CONFLICT));
					locations.add(new Integer(OverlayIcon.TOP_LEFT));
				}

				if (overlays.isEmpty()) {
					return image;
				}

				//combine the descriptors and return the resulting image
				Integer[] integers = (Integer[])locations.toArray(new Integer[locations.size()]);
				int[] locs = new int[integers.length];
				for (int i = 0; i < integers.length; i++) {
					locs[i] = integers[i].intValue();
				}
				
				return iconCache.getImageFor(new DiffOverlayIcon(image,
					(ImageDescriptor[]) overlays.toArray(new ImageDescriptor[overlays.size()]),
					locs));
			}

			public String getText(Object element) {
				String label = oldProvider.getText(element);
				if (! (element instanceof ITeamNode))
					return label;
					
				ITeamNode node = (ITeamNode)element;					
				IResource resource = node.getResource();

				if (resource.exists()) {
					// use the default text decoration preferences
					Decoration decoration = new Decoration();
					SVNLightweightDecorator.decorateTextLabel(resource, decoration, false /*don't show dirty*/); //, false /*don't show revisions*/);
					label = decoration.prefix + label + decoration.suffix;
				}
				
				if (SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_SYNCINFO_AS_TEXT)) {
					int syncKind = node.getKind();
					if (syncKind != ILocalSyncElement.IN_SYNC) {
						String syncKindString = RemoteSyncElement.kindToString(syncKind);
						label = Policy.bind("SVNCatchupReleaseViewer.labelWithSyncKind", label, syncKindString); //$NON-NLS-1$
					}
				}
				return label;
			}								
		});
	}
//	
//	protected void fillContextMenu(IMenuManager manager) {
//		super.fillContextMenu(manager);
//		if (showInHistory != null) {
//			manager.add(showInHistory);
//		}
//		manager.add(new Separator());
//		switch (getSyncMode()) {
//			case SyncView.SYNC_INCOMING:
//				updateAction.update(SyncView.SYNC_INCOMING);
//				manager.add(updateAction);
//				forceUpdateAction.update(SyncView.SYNC_INCOMING);
//				manager.add(forceUpdateAction);
//				manager.add(new Separator());
//				confirmMerge.setEnabled(confirmMerge.isEnabled());				
//				manager.add(confirmMerge);
//				break;
//			case SyncView.SYNC_OUTGOING:
//				addAction.update(SyncView.SYNC_OUTGOING);
//				manager.add(addAction);
//				commitAction.update(SyncView.SYNC_OUTGOING);
//				manager.add(commitAction);
//				forceCommitAction.update(SyncView.SYNC_OUTGOING);
//				manager.add(forceCommitAction);
//				ignoreAction.update();
//				manager.add(ignoreAction);
//				manager.add(new Separator());
//				confirmMerge.setEnabled(confirmMerge.isEnabled());				
//				manager.add(confirmMerge);
//				selectAdditions.setEnabled(selectAdditions.isEnabled());				
//				manager.add(selectAdditions);
//				break;
//			case SyncView.SYNC_BOTH:
//				addAction.update(SyncView.SYNC_BOTH);
//				manager.add(addAction);
//				commitAction.update(SyncView.SYNC_BOTH);
//				manager.add(commitAction);
//				updateAction.update(SyncView.SYNC_BOTH);
//				manager.add(updateAction);
//				ignoreAction.update();
//				manager.add(ignoreAction);
//				manager.add(new Separator());
//				forceCommitAction.update(SyncView.SYNC_BOTH);
//				manager.add(forceCommitAction);
//				forceUpdateAction.update(SyncView.SYNC_BOTH);
//				manager.add(forceUpdateAction);				
//				manager.add(new Separator());
//				confirmMerge.setEnabled( confirmMerge.isEnabled());				
//				manager.add(confirmMerge);
//				break;
//			case SyncView.SYNC_MERGE:
//				updateMergeAction.update(SyncView.SYNC_INCOMING);
//				forceUpdateMergeAction.update(SyncView.SYNC_INCOMING);
//				updateWithJoinAction.update(SyncView.SYNC_INCOMING);
//				manager.add(updateMergeAction);
//				manager.add(forceUpdateMergeAction);
//				manager.add(updateWithJoinAction);
//				break;
//		}
//	}
//	
	/**
	 * Creates the actions for this viewer.
	 */
	private void initializeActions(final SVNSyncCompareInput diffModel) {
//		Shell shell = getControl().getShell();
//		commitAction = new CommitSyncAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.commit"), shell); //$NON-NLS-1$
//		forceCommitAction = new ForceCommitSyncAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.forceCommit"), shell); //$NON-NLS-1$
//		updateAction = new UpdateSyncAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.update"), shell); //$NON-NLS-1$
//		forceUpdateAction = new ForceUpdateSyncAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.forceUpdate"), shell); //$NON-NLS-1$
//		updateMergeAction = new UpdateMergeAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.update"), shell); //$NON-NLS-1$
//		ignoreAction = new IgnoreAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.ignore"), shell); //$NON-NLS-1$
//		updateWithJoinAction = new UpdateWithForcedJoinAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.mergeUpdate"), shell); //$NON-NLS-1$
//		forceUpdateMergeAction = new OverrideUpdateMergeAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.forceUpdate"), shell); //$NON-NLS-1$
//		addAction = new AddSyncAction(diffModel, this, Policy.bind("SVNCatchupReleaseViewer.addAction"), shell); //$NON-NLS-1$
//		
//		// Show in history view
//		showInHistory = new HistoryAction(Policy.bind("SVNCatchupReleaseViewer.showInHistory")); //$NON-NLS-1$
//		WorkbenchHelp.setHelp(showInHistory, IHelpContextIds.SHOW_IN_RESOURCE_HISTORY);
//		addSelectionChangedListener(showInHistory);
//		
//		selectAdditions = new Action(Policy.bind("SVNCatchupReleaseViewer.Select_&Outgoing_Additions_1"), null) { //$NON-NLS-1$
//			public boolean isEnabled() {
//				DiffNode node = diffModel.getDiffRoot();
//				IDiffElement[] elements = node.getChildren();
//				for (int i = 0; i < elements.length; i++) {
//					IDiffElement element = elements[i];
//					if (element instanceof ITeamNode) {
//						SVNSyncSet set = new SVNSyncSet(new StructuredSelection(element));
//						try {
//							if (set.hasNonAddedChanges()) return true;
//						} catch (SVNException e) {
//							// Log the error and enable the menu item
//							SVNUIPlugin.log(e.getStatus());
//							return true;
//						}
//					} else {
//						// unanticipated situation, just enable the action
//						return true;
//					}
//				}
//				return false;
//			}
//			public void run() {
//				List additions = new ArrayList();
//				DiffNode root = diffModel.getDiffRoot();
//				visit(root, additions);
//				setSelection(new StructuredSelection(additions));
//			}
//			private void visit(IDiffElement node, List additions) {
//				try {
//					if (node instanceof TeamFile) {
//						TeamFile file = (TeamFile)node;
//						if (file.getChangeDirection() == IRemoteSyncElement.OUTGOING) {
//							if (file.getChangeType() == IRemoteSyncElement.ADDITION) {
//								ISVNResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(file.getResource());
//								if (svnResource.isManaged()) return;
//								additions.add(node);
//							}
//						}
//						return;
//					}
//					if (node instanceof ChangedTeamContainer) {
//						ChangedTeamContainer container = (ChangedTeamContainer)node;
//						if (container.getChangeDirection() == IRemoteSyncElement.OUTGOING) {
//							if (container.getChangeType() == IRemoteSyncElement.ADDITION) {
//								ISVNResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(container.getResource());
//								if (!((ISVNFolder)svnResource).isSVNFolder()) {
//									additions.add(node);
//								}
//							}
//						}
//						
//					}
//					if (node instanceof DiffContainer) {
//						IDiffElement[] children = ((DiffContainer)node).getChildren();
//						for (int i = 0; i < children.length; i++) {
//							visit(children[i], additions);
//						}
//					}
//				} catch (TeamException e) {
//					SVNUIPlugin.log(e.getStatus());
//				}
//			}
//		};
//		WorkbenchHelp.setHelp(selectAdditions, IHelpContextIds.SELECT_NEW_RESOURCES_ACTION);
//		
//		// confirm merge
//		confirmMerge = new Action(Policy.bind("SVNCatchupReleaseViewer.confirmMerge"), null) { //$NON-NLS-1$
//			public void run() {
//				ISelection s = getSelection();
//				if (!(s instanceof IStructuredSelection) || s.isEmpty()) {
//					return;
//				}
//				List needsMerge = new ArrayList();
//				for (Iterator it = ((IStructuredSelection)s).iterator(); it.hasNext();) {
//					final Object element = it.next();
//					if(element instanceof DiffElement) {
//						mergeRecursive((IDiffElement)element, needsMerge);
//					}
//				}
//				TeamFile[] files = (TeamFile[]) needsMerge.toArray(new TeamFile[needsMerge.size()]);
//				if(files.length != 0) {
//					try {
//						for (int i = 0; i < files.length; i++) {		
//							TeamFile teamFile = (TeamFile)files[i];
//							SVNUIPlugin.getPlugin().getRepositoryManager().merged(new IRemoteSyncElement[] {teamFile.getMergeResource().getSyncElement()});
//							teamFile.merged();
//						}
//					} catch(TeamException e) {
//						SVNUIPlugin.openError(getControl().getShell(), null, null, e);
//					}
//				}
//				refresh();
//				diffModel.updateStatusLine();
//			}
//			 
//			public boolean isEnabled() {
//				ISelection s = getSelection();
//				if (!(s instanceof IStructuredSelection) || s.isEmpty()) {
//					return false;
//				}
//				for (Iterator it = ((IStructuredSelection)s).iterator(); it.hasNext();) {
//					Object element = (Object) it.next();
//					if(element instanceof TeamFile) {
//						TeamFile file = (TeamFile)element;						
//						int direction = file.getChangeDirection();
//						int type = file.getChangeType();
//						if(direction == IRemoteSyncElement.INCOMING ||
//						   direction == IRemoteSyncElement.CONFLICTING) {
//							continue;
//						}
//					}
//					return false;
//				}
//				return true;
//			}
//		};
//		WorkbenchHelp.setHelp(confirmMerge, IHelpContextIds.CONFIRM_MERGE_ACTION);
	}
	
//	protected void mergeRecursive(IDiffElement element, List needsMerge) {
//		if (element instanceof DiffContainer) {
//			DiffContainer container = (DiffContainer)element;
//			IDiffElement[] children = container.getChildren();
//			for (int i = 0; i < children.length; i++) {
//				mergeRecursive(children[i], needsMerge);
//			}
//		} else if (element instanceof TeamFile) {
//			TeamFile file = (TeamFile)element;
//			needsMerge.add(file);			
//		}
//	}
//	
//	/**
//	 * Provide SVN-specific labels for the editors.
//	 */
//	protected void updateLabels(MergeResource resource) {
//		CompareConfiguration config = getCompareConfiguration();
//		String name = resource.getName();
//		config.setLeftLabel(Policy.bind("SVNCatchupReleaseViewer.workspaceFile", name)); //$NON-NLS-1$
//	
//		IRemoteSyncElement syncTree = resource.getSyncElement();
//		IRemoteResource remote = syncTree.getRemote();
//		if (remote != null) {
//			try {
//				final ISVNRemoteFile remoteFile = (ISVNRemoteFile)remote;
//				String revision = remoteFile.getRevision();
//				final String[] author = new String[] { "" }; //$NON-NLS-1$
//				try {
//					SVNUIPlugin.runWithProgress(getTree().getShell(), true /*cancelable*/,
//						new IRunnableWithProgress() {
//						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//							try {
//								ILogEntry logEntry = remoteFile.getLogEntry(monitor);
//								if (logEntry != null)
//									author[0] = logEntry.getAuthor();
//							} catch (TeamException e) {
//								throw new InvocationTargetException(e);
//							}
//						}
//					});
//				} catch (InterruptedException e) { // ignore cancellation
//				} catch (InvocationTargetException e) {
//					Throwable t = e.getTargetException();
//					if (t instanceof TeamException) {
//						throw (TeamException) t;
//					}
//					// should not get here
//				}
//				config.setRightLabel(Policy.bind("SVNCatchupReleaseViewer.repositoryFileRevision", new Object[] {name, revision, author[0]})); //$NON-NLS-1$
//			} catch (TeamException e) {
//				SVNUIPlugin.openError(getControl().getShell(), null, null, e);
//				config.setRightLabel(Policy.bind("SVNCatchupReleaseViewer.repositoryFile", name)); //$NON-NLS-1$
//			}
//		} else {
//			config.setRightLabel(Policy.bind("SVNCatchupReleaseViewer.noRepositoryFile")); //$NON-NLS-1$
//		}
//	
//		IRemoteResource base = syncTree.getBase();
//		if (base != null) {
//			try {
//				String revision = ((ISVNRemoteFile)base).getRevision();
//				config.setAncestorLabel(Policy.bind("SVNCatchupReleaseViewer.commonFileRevision", new Object[] {name, revision} )); //$NON-NLS-1$
//			} catch (TeamException e) {
//				SVNUIPlugin.openError(getControl().getShell(), null, null, e);
//				config.setRightLabel(Policy.bind("SVNCatchupReleaseViewer.commonFile", name)); //$NON-NLS-1$
//			}
//		} else {
//			config.setAncestorLabel(Policy.bind("SVNCatchupReleaseViewer.noCommonFile")); //$NON-NLS-1$
//		}
//		
//		IResource local = syncTree.getLocal();
//		if (local != null) {
//			if (!local.exists()) {
//				config.setLeftLabel(Policy.bind("SVNCatchupReleaseViewer.No_workspace_file_1")); //$NON-NLS-1$
//			} else {
//				ISVNFile svnFile = SVNWorkspaceRoot.getSVNFileFor((IFile)local);
//				ResourceSyncInfo info = null;
//				try {
//					info = svnFile.getSyncInfo();
//					name = local.getName();
//					String revision = null;
//					if (info != null) {
//						revision = info.getRevision();
//						if (info.isAdded() || info.isDeleted()) {
//							revision = null;
//						}
//					}
//					if (revision != null) {
//						config.setLeftLabel(Policy.bind("SVNCatchupReleaseViewer.commonFileRevision", name, revision)); //$NON-NLS-1$
//					} else {
//						config.setLeftLabel(Policy.bind("SVNCatchupReleaseViewer.commonFile", name)); //$NON-NLS-1$
//					}
//				} catch (SVNException e) {
//					SVNUIPlugin.openError(getControl().getShell(), null, null, e);
//					config.setLeftLabel(Policy.bind("SVNCatchupReleaseViewer.commonFile", name)); //$NON-NLS-1$				
//				}
//			}
//		}
//	}
}
