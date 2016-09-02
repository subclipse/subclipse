/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.compare;

 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffContainer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.internal.Utilities;
import org.tigris.subversion.subclipse.ui.history.HistoryTableProvider;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * A compare input for comparing local resource with remote ones 
 * Used by CompareWithRevisionAction
 */
public class SVNCompareRevisionsInput extends CompareEditorInput implements ISaveableWorkbenchPart {
	private IFile resource;
	private ILogEntry[] logEntries;
	private TableViewer viewer;
	private Action getContentsAction;
	private Action getRevisionAction;
	private Shell shell;
	
	private HistoryTableProvider historyTableProvider;
	
    /**
     * the ITypedElement for the left element (ie the file) 
     */
	class TypedBufferedContent extends ResourceNode {
		public TypedBufferedContent(IFile resource) {
			super(resource);
		}
		protected InputStream createStream() throws CoreException {
			return ((IFile)getResource()).getContents();
		}
        
        // used by getContentsAction
		public void setContent(byte[] contents) {
			if (contents == null) contents = new byte[0];
			final InputStream is = new ByteArrayInputStream(contents);
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException  {
					try {
						IFile file = resource;
						if (is != null) {
							if (!file.exists()) {
								file.create(is, false, monitor);
							} else {
								file.setContents(is, false, true, monitor);
							}
						} else {
							file.delete(false, true, monitor);
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				new ProgressMonitorDialog(shell).run(false, false, runnable);
			} catch (InvocationTargetException e) {
				SVNUIPlugin.openError(SVNUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell(), Policy.bind("TeamFile.saveChanges", resource.getName()), null, e); //$NON-NLS-1$
			} catch (InterruptedException e) {
				// Ignore
			}
			fireContentChanged();
		}	
		public ITypedElement replace(ITypedElement child, ITypedElement other) {
			return null;
		}
	}
	
	/**
	 * This class is an edition node which knows the log entry it came from.
	 */
	class ResourceRevisionNode extends ResourceEditionNode {	
		ILogEntry entry;
		public ResourceRevisionNode(ILogEntry entry) {
			super(entry.getRemoteResource());
			this.entry = entry;
			if (entry.getRemoteResource() instanceof RemoteResource) {
				((RemoteResource)entry.getRemoteResource()).setPegRevision(SVNRevision.HEAD);
			}
		}
		public ILogEntry getLogEntry() {
			return entry;
		}
		public String getName() {
			
			IResource resource = SVNCompareRevisionsInput.this.resource;
			try {
				ISVNRemoteFile currentEdition = (ISVNRemoteFile) SVNWorkspaceRoot.getBaseResourceFor(resource);
				if (currentEdition != null && currentEdition.getLastChangedRevision().equals(entry.getRevision())) {
					Policy.bind("currentRevision", entry.getRevision().toString()); //$NON-NLS-1$
				} else {
					return entry.getRevision().toString();
				}
			} catch (TeamException e) {
				handle(e);
			}
			return super.getName();
		}
	}
    
	/**
	 * A compare node that gets its label from the right element
	 */
	class VersionCompareDiffNode extends DiffNode implements IAdaptable {
		public VersionCompareDiffNode(ITypedElement left, ITypedElement right) {
			super(left, right);
		}
		public String getName() {
			return getRight().getName();
		}
		public Object getAdapter(Class adapter) {
			if (adapter == ILogEntry.class) {
				return ((ResourceRevisionNode)getRight()).getLogEntry();
			}
			return null;
		}

	}
	/**
	 * A content provider which knows how to get the children of the diff container
	 */
	class VersionCompareContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof DiffContainer) {
				return ((DiffContainer)inputElement).getChildren();
			}
			return null;
		}
	}
	
    /**
     * creates a SVNCompareRevisionsInput  
     */
	public SVNCompareRevisionsInput(IFile resource, ILogEntry[] logEntries) {
		super(new CompareConfiguration());
		this.resource = resource;
		this.logEntries = logEntries;
		updateCurrentEdition();
		initializeActions();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public Control createContents(Composite parent) {
		Control c = super.createContents(parent);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// This is a hack to get around a problem with initial sorting in OSx.
		historyTableProvider.setSortColumn(viewer, 0);
		historyTableProvider.setSortColumn(viewer, 0);
		
		return c;
	}

    /**
     * create the diff viewer :
     * - the table for the revisions of the file
     * - 
     */
	public Viewer createDiffViewer(Composite parent) {
		this.shell = parent.getShell();
		viewer = getHistoryTableProvider().createTable(parent);
		Table table = viewer.getTable();
		table.setData(CompareUI.COMPARE_VIEWER_TITLE, Policy.bind("SVNCompareRevisionsInput.structureCompare")); //$NON-NLS-1$
	
		viewer.setContentProvider(new VersionCompareContentProvider());

		MenuManager mm = new MenuManager();
		mm.setRemoveAllWhenShown(true);
		mm.addMenuListener(
			new IMenuListener() {
				public void menuAboutToShow(IMenuManager mm) {
					mm.add(getContentsAction);
					mm.add(getRevisionAction);
				}
			}
		);
		table.setMenu(mm.createContextMenu(table));
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (!(selection instanceof IStructuredSelection)) {
					getContentsAction.setEnabled(false);
					getRevisionAction.setEnabled(false);
					return;
				}
				IStructuredSelection ss = (IStructuredSelection)selection;
				getContentsAction.setEnabled(ss.size() == 1);
				getRevisionAction.setEnabled(ss.size() == 1);
			}	
		});
		
		// Add F1 help.
		PlatformUI.getWorkbench().getHelpSystem().setHelp(table, IHelpContextIds.COMPARE_REVISIONS_VIEW);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getContentsAction, IHelpContextIds.GET_FILE_CONTENTS_ACTION);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getRevisionAction, IHelpContextIds.GET_FILE_REVISION_ACTION);
		
		viewer.resetFilters();
		
		return viewer;
	}

    /**
     * initialize the labels : the title, the lft label and the right one
     */
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		String resourceName = resource.getName();	
		setTitle(Policy.bind("SVNCompareRevisionsInput.compareResourceAndVersions", new Object[] {resourceName})); //$NON-NLS-1$
		cc.setLeftEditable(true);
		cc.setRightEditable(false);
		
		String leftLabel = Policy.bind("SVNCompareRevisionsInput.workspace", new Object[] {resourceName}); //$NON-NLS-1$
		cc.setLeftLabel(leftLabel);
		String rightLabel = Policy.bind("SVNCompareRevisionsInput.repository", new Object[] {resourceName}); //$NON-NLS-1$
		cc.setRightLabel(rightLabel);
	}
    
    /**
     * initialize the actions :
     * - getContentsAction : get the contents for the selected revision
     * - getRevisionAction : updates to the given revision
     */
	private void initializeActions() {
		getContentsAction = new Action(Policy.bind("HistoryView.getContentsAction"), null) { //$NON-NLS-1$
			public void run() {
				try {
					new ProgressMonitorDialog(shell).run(false, true, new WorkspaceModifyOperation() {
						protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
							IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
							if (selection.size() != 1) return;
							VersionCompareDiffNode node = (VersionCompareDiffNode)selection.getFirstElement();
							ResourceEditionNode right = (ResourceEditionNode)node.getRight();
							ISVNRemoteResource edition = right.getRemoteResource();
							// Do the load. This just consists of setting the local contents. We don't
							// actually want to change the base.
							try {
								monitor.beginTask(null, 100);
								InputStream in = ((IResourceVariant)edition).getStorage(new SubProgressMonitor(monitor, 50)).getContents();
								resource.setContents(in, false, true, new SubProgressMonitor(monitor, 50));
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							} finally {
								monitor.done();
							}
						}
					});
				} catch (InterruptedException e) {
					// Do nothing
					return;
				} catch (InvocationTargetException e) {
					handle(e);
				}
				// recompute the labels on the viewer
				updateCurrentEdition();
				viewer.refresh();
			}
		};
		
		getRevisionAction = new Action(Policy.bind("HistoryView.getRevisionAction"), null) { //$NON-NLS-1$
			public void run() {
				try {
				    new ProgressMonitorDialog(shell).run(false, true, new WorkspaceModifyOperation(null) {
						protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						    IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
							if (selection.size() != 1) return;
							VersionCompareDiffNode node = (VersionCompareDiffNode)selection.getFirstElement();
							ResourceEditionNode right = (ResourceEditionNode)node.getRight();
							final ISVNRemoteFile edition = (ISVNRemoteFile)right.getRemoteResource();
							new UpdateOperation(null, resource, edition.getLastChangedRevision()).run(monitor);							    
							// recompute the labels on the viewer
							getHistoryTableProvider().setRemoteResource(edition);
							viewer.refresh();
						}
				    });
				} catch (InterruptedException e) {
					// Do nothing
					return;
				} catch (InvocationTargetException e) {
					handle(e);
				}
			}
		};
	}
    
    /**
     * Runs the compare operation and returns the compare result.
     */
	protected Object prepareInput(IProgressMonitor monitor){
		initLabels();
		DiffNode diffRoot = new DiffNode(Differencer.NO_CHANGE);
		String localCharset = Utilities.getCharset(resource);
		for (int i = 0; i < logEntries.length; i++) {		
			ITypedElement left = new TypedBufferedContent(resource);
			ResourceRevisionNode right = new ResourceRevisionNode(logEntries[i]);
			try {
				right.setCharset(localCharset);
			} catch (CoreException e) {
			}
			diffRoot.add(new VersionCompareDiffNode(left, right));
		}
		return diffRoot;		
	}
	
    /**
     * sets the file for the historyTableProvider
     * Used when file is changed (updated to a revision) 
     */
    private void updateCurrentEdition() {
		try {
			getHistoryTableProvider().setRemoteResource((ISVNRemoteFile) SVNWorkspaceRoot.getBaseResourceFor(resource));
		} catch (TeamException e) {
			handle(e);
		}
	}
    
	private void handle(Exception e) {
		setMessage(SVNUIPlugin.openError(shell, null, null, e, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS).getMessage());
	}
	/**
	 * Returns the historyTableProvider.
	 * @return HistoryTableProvider
	 */
	public HistoryTableProvider getHistoryTableProvider() {
		if (historyTableProvider == null) {
			historyTableProvider = new HistoryTableProvider();
		}
		return historyTableProvider;
	}

	/**
	 * Updates the contents of the local file with the contents of the currently selected LogEntry
	 * TODO shouldn't it replace with the selected revision (taking the revision number with it)
	 * @throws CoreException
	 */
	public void replaceLocalWithCurrentlySelectedRevision() throws CoreException {
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		if (selection.size() != 1) return;
		VersionCompareDiffNode node = (VersionCompareDiffNode)selection.getFirstElement();
		ResourceRevisionNode right = (ResourceRevisionNode)node.getRight();
		TypedBufferedContent left = (TypedBufferedContent)node.getLeft();
		left.setContent(Utils.readBytes(right.getContents()));
	}

	/**
	 * Gets the table viewer that will be diaplsying the log entries.
	 * TODO this should not be public
	 */
	public TableViewer getViewer() {
		return viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		try {
			saveChanges(monitor);
		} catch (CoreException e) {
			Utils.handle(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
		// noop
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		return isSaveNeeded();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#addPropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	public void addPropertyListener(IPropertyListener listener) {
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		createContents(parent);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getSite()
	 */
	public IWorkbenchPartSite getSite() {
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleToolTip()
	 */
	public String getTitleToolTip() {
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#removePropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	public void removePropertyListener(IPropertyListener listener) {
	}

	public boolean canRunAsJob() {
		return true;
	}
}
