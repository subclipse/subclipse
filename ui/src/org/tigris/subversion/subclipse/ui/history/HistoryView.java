/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

 
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.OpenRemoteFileAction;
import org.tigris.subversion.subclipse.ui.console.TextViewerAction;

import com.qintsoft.jsvn.jni.Revision;


/**
 * The history view allows browsing of an array of resource revisions
 */
public class HistoryView extends ViewPart {
	private IFile file;

	// cached for efficiency
	private LogEntry[] entries;

	private HistoryTableProvider historyTableProvider;
	
	private TableViewer tableViewer;
	private TextViewer textViewer;
	
    private OpenRemoteFileAction openAction;
	private TextViewerAction copyAction;
	private TextViewerAction selectAllAction;
	private Action getContentsAction;
	private Action getRevisionAction;
	private Action refreshAction;

	private SashForm sashForm;
	private SashForm innerSashForm;

	private LogEntry currentSelection; 
	
	public static final String VIEW_ID = "org.tigris.subversion.subclipse.ui.history.HistoryView"; //$NON-NLS-1$


    /**
     * All Actions use this class 
     * This action :
     * - updates currentSelection
     * - action.run 
     */
    private Action getContextMenuAction(String title, final IWorkspaceRunnable action) {
            return new Action(title) {
            public void run() {
                try {
                    if (file == null) return;
                    ISelection selection = tableViewer.getSelection();
                    if (!(selection instanceof IStructuredSelection)) return;
                    IStructuredSelection ss = (IStructuredSelection)selection;
                    currentSelection = (LogEntry)ss.getFirstElement();
                    new ProgressMonitorDialog(getViewSite().getShell()).run(false, true, new WorkspaceModifyOperation() {
                        protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            try {               
                                action.run(monitor);
                            } catch (CoreException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                } catch (InvocationTargetException e) {
                    SVNUIPlugin.openError(getViewSite().getShell(), null, null, e, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
            
            // we don't allow multiple selection
            public boolean isEnabled() {
                ISelection selection = tableViewer.getSelection();
                if (!(selection instanceof IStructuredSelection)) return false;
                IStructuredSelection ss = (IStructuredSelection)selection;
                if(ss.size() != 1) return false;
                return true;
            }
        };
    }
	
	/**
	 * Adds the action contributions for this view.
	 */
	protected void contributeActions() {
		// Refresh (toolbar)
		SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
		refreshAction = new Action(Policy.bind("HistoryView.refreshLabel"), plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
			public void run() {
				refresh();
			}
		};
		refreshAction.setToolTipText(Policy.bind("HistoryView.refresh")); //$NON-NLS-1$
		refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
		refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
		
		// Double click open action
        openAction = new OpenRemoteFileAction();
		tableViewer.getTable().addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				openAction.selectionChanged(null, tableViewer.getSelection());
				openAction.run(null);
			}
		});

        // get contents        
		getContentsAction = getContextMenuAction(Policy.bind("HistoryView.getContentsAction"), new IWorkspaceRunnable() { //$NON-NLS-1$
			public void run(IProgressMonitor monitor) throws CoreException {
				ISVNRemoteFile remoteFile = (ISVNRemoteFile)currentSelection.getRemoteResource();
				monitor.beginTask(null, 100);
				try {
					if(confirmOverwrite()) {
						InputStream in = remoteFile.getContents(new SubProgressMonitor(monitor, 50));
						file.setContents(in, false, true, new SubProgressMonitor(monitor, 50));				
					}
				} catch (TeamException e) {
					throw new CoreException(e.getStatus());
				} finally {
					monitor.done();
				}
			}
		});
		WorkbenchHelp.setHelp(getContentsAction, IHelpContextIds.GET_FILE_CONTENTS_ACTION);	

        // update to the selected revision
		getRevisionAction = getContextMenuAction(Policy.bind("HistoryView.getRevisionAction"), new IWorkspaceRunnable() { //$NON-NLS-1$
			public void run(IProgressMonitor monitor) throws CoreException {
				ISVNRemoteFile remoteFile = (ISVNRemoteFile)currentSelection.getRemoteResource();
				try {
					if(confirmOverwrite()) {
						SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(file.getProject());
                        provider.update(new IResource[] {file}, remoteFile.getLastChangedRevision(), monitor);					 
						historyTableProvider.setFile(remoteFile);
						tableViewer.refresh();
					}
				} catch (TeamException e) {
					throw new CoreException(e.getStatus());
				}
			}
		});
		WorkbenchHelp.setHelp(getRevisionAction, IHelpContextIds.GET_FILE_REVISION_ACTION);	

		// Contribute actions to popup menu for the table
		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr) {
				fillTableMenu(menuMgr);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		tableViewer.getTable().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);

		// Create the local tool bar
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(refreshAction);
		tbm.update(false);
        
        IActionBars actionBars = getViewSite().getActionBars();
        
		// Create actions for the text editor (copy and select all)
		copyAction = new TextViewerAction(textViewer, ITextOperationTarget.COPY);
		copyAction.setText(Policy.bind("HistoryView.copy")); //$NON-NLS-1$
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY, copyAction);
		
		selectAllAction = new TextViewerAction(textViewer, ITextOperationTarget.SELECT_ALL);
		selectAllAction.setText(Policy.bind("HistoryView.selectAll")); //$NON-NLS-1$
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.SELECT_ALL, selectAllAction);

		actionBars.updateActionBars();

        // Contribute actions to popup menu for the comments area
		menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr) {
				fillTextMenu(menuMgr);
			}
		});
		StyledText text = textViewer.getTextWidget();
		menu = menuMgr.createContextMenu(text);
		text.setMenu(menu);
	}
    
	/*
	 * Method declared on IWorkbenchPart
	 */
	public void createPartControl(Composite parent) {
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer = createTable(sashForm);
		innerSashForm = new SashForm(sashForm, SWT.HORIZONTAL);
		textViewer = createText(innerSashForm);
		sashForm.setWeights(new int[] { 70, 30 });

		contributeActions();
		// set F1 help
		WorkbenchHelp.setHelp(sashForm, IHelpContextIds.RESOURCE_HISTORY_VIEW);
		initDragAndDrop();
	}

	/**
	 * Creates the group that displays lists of the available repositories
	 * and team streams.
	 *
	 * @param the parent composite to contain the group
	 * @return the group control
	 */
	protected TableViewer createTable(Composite parent) {
		
		historyTableProvider = new HistoryTableProvider();
		TableViewer viewer = historyTableProvider.createTable(parent);
		
        // set the content provider for the table
		viewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				// Short-circuit to optimize
				if (entries != null) return entries;
				
				if (!(inputElement instanceof ISVNRemoteFile)) return null;
				final ISVNRemoteFile remoteFile = (ISVNRemoteFile)inputElement;
				final Object[][] result = new Object[1][];
				try {
					new ProgressMonitorDialog(getViewer().getTable().getShell()).run(true, true, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								entries = remoteFile.getLogEntries(monitor);
								result[0] = entries;
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
				} catch (InterruptedException e) { // ignore cancellation
					result[0] = new Object[0];
				} catch (InvocationTargetException e) {
					SVNUIPlugin.openError(getViewSite().getShell(), null, null, e);
					result[0] = new Object[0];
				}
				return result[0];
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				entries = null;
			}
		});
		
        // set the selectionchanged listener for the table
        // updates the comments when selection changes
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
					return;
				}
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (ss.size() != 1) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
					return;
				}
				LogEntry entry = (LogEntry)ss.getFirstElement();
				textViewer.setDocument(new Document(entry.getComment()));
			}
		});
		
		return viewer;
	}

    /**
     * Create the TextViewer for the logEntry comments 
     */
	protected TextViewer createText(Composite parent) {
		TextViewer result = new TextViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		result.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				copyAction.update();
			}
		});
		return result;
	}

	/**
	 * Returns the table viewer contained in this view.
	 */
	protected TableViewer getViewer() {
		return tableViewer;
	}

	/**
	 * Adds drag and drop support to the history view.
	 */
	void initDragAndDrop() {
		int ops = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
		Transfer[] transfers = new Transfer[] {ResourceTransfer.getInstance()};
		tableViewer.addDropSupport(ops, transfers, new HistoryDropAdapter(tableViewer, this));
	}

    /**
     * fill the popup menu for the table
     */
	private void fillTableMenu(IMenuManager manager) {
		// file actions go first (view file)
		manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
		if (file != null) {
			// Add the "Add to Workspace" action if 1 revision is selected.
			ISelection sel = tableViewer.getSelection();
			if (!sel.isEmpty()) {
				if (sel instanceof IStructuredSelection) {
					if (((IStructuredSelection)sel).size() == 1) {
						manager.add(getContentsAction);
						manager.add(getRevisionAction);
						manager.add(new Separator());
					}
				}
			}
		}
		manager.add(new Separator("additions")); //$NON-NLS-1$
		manager.add(refreshAction);
		manager.add(new Separator("additions-end")); //$NON-NLS-1$
	}
    
    /**
     * fill the popup menu for the comments area 
     */
	private void fillTextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(selectAllAction);
	}
    
//	/**
//	 * Makes the history view visible in the active perspective. If there
//	 * isn't a history view registered <code>null</code> is returned.
//	 * Otherwise the opened view part is returned.
//	 */
//	public static HistoryView openInActivePerspective() {
//		try {
//			return (HistoryView)SVNUIPlugin.getActivePage().showView(VIEW_ID);
//		} catch (PartInitException pe) {
//			return null;
//		}
//	}

	/** (Non-javadoc)
	 * Method declared on IWorkbenchPart
	 */
	public void setFocus() {
		if (tableViewer != null) {
			Table control = tableViewer.getTable();
			if (control != null && !control.isDisposed()) {
				control.setFocus();
			}
		}
	}
	
	/**
	 * Shows the history for the given IResource in the view.
	 * 
	 * Only files are supported for now.
	 */
	public void showHistory(IResource resource) {
		if (resource instanceof IFile) {
			IFile file = (IFile)resource;
			this.file = file;
			RepositoryProvider teamProvider = RepositoryProvider.getProvider(file.getProject(), SVNProviderPlugin.getTypeId());
			if (teamProvider != null) {
				try {
					ISVNRemoteFile remoteFile = (ISVNRemoteFile)SVNWorkspaceRoot.getRemoteResourceFor(file);
					historyTableProvider.setFile(remoteFile);
					tableViewer.setInput(remoteFile);
					setTitle(Policy.bind("HistoryView.titleWithArgument", remoteFile.getName())); //$NON-NLS-1$
				} catch (TeamException e) {
					SVNUIPlugin.openError(getViewSite().getShell(), null, null, e);
				}				
			}
			return;
		}
		this.file = null;
		tableViewer.setInput(null);
		setTitle(Policy.bind("HistoryView.title")); //$NON-NLS-1$
	}
	
	/**
	 * Shows the history for the given ISVNRemoteFile in the view.
	 */
	public void showHistory(ISVNRemoteFile remoteFile, String currentRevision) {
		try {
			if (remoteFile == null) {
				tableViewer.setInput(null);
				setTitle(Policy.bind("HistoryView.title")); //$NON-NLS-1$
				return;
			}
			this.file = null;
			historyTableProvider.setFile(remoteFile);
			tableViewer.setInput(remoteFile);
			setTitle(Policy.bind("HistoryView.titleWithArgument", remoteFile.getName())); //$NON-NLS-1$
		} catch (SVNException e) {
			SVNUIPlugin.openError(getViewSite().getShell(), null, null, e);
		}
	}
	
    
	/**
     * Ask the user to confirm the overwrite of the file if the file has been modified
     * since last commit
	 */
	private boolean confirmOverwrite() {
		if (file!=null && file.exists()) {
			ISVNLocalFile svnFile = SVNWorkspaceRoot.getSVNFileFor(file);
			try {
				if(svnFile.isModified()) {
					String title = Policy.bind("HistoryView.overwriteTitle"); //$NON-NLS-1$
					String msg = Policy.bind("HistoryView.overwriteMsg"); //$NON-NLS-1$
					final MessageDialog dialog = new MessageDialog(getViewSite().getShell(), title, null, msg, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
					final int[] result = new int[1];
					getViewSite().getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						result[0] = dialog.open();
					}});
					if (result[0] != 0) {
						// cancel
						return false;
					}
				}
			} catch(SVNException e) {
				SVNUIPlugin.log(e.getStatus());
			}
		}
		return true;
	}
	
	/*
	 * Refresh the view by refetching the log entries for the remote file
	 */
	private void refresh() {
		entries = null;
        // show a Busy Cursor during refresh
		BusyIndicator.showWhile(tableViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				tableViewer.refresh();
			}
		});
	}
}
