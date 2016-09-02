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
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.internal.InfiniteSubProgressMonitor;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * Unmanage action removes the svn feature from a project and optionally
 * deletes the SVN meta information that is stored on disk.
 */
public class UnmanageAction extends WorkspaceAction {
	
    // the dialog that will ask if we want to delete .SVN directory
    // from the project. It also allows to cancel the operation
	static class DeleteProjectDialog extends MessageDialog {

		
		private boolean deleteContent = false;
		private Button radio1;
		private Button radio2;
		
		DeleteProjectDialog(Shell parentShell, IProject[] projects) {
			super(
				parentShell, 
				getTitle(projects), 
				null,	// accept the default window icon
				getMessage(projects),
				MessageDialog.QUESTION, 
				new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
				0); 	// yes is the default
			
		}
		
		static String getTitle(IProject[] projects) {
			if (projects.length == 1)
				return Policy.bind("Unmanage.title");  //$NON-NLS-1$
			else
				return Policy.bind("Unmanage.titleN");  //$NON-NLS-1$
		}
		
		static String getMessage(IProject[] projects) {
			if (projects.length == 1) {
				IProject project = projects[0];
				return Policy.bind("Unmanage.message", project.getName());  //$NON-NLS-1$
			}
			else {
				return Policy.bind("Unmanage.messageN", new Integer(projects.length).toString());  //$NON-NLS-1$
			}
		}
		
		protected Control createCustomArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			radio1 = new Button(composite, SWT.RADIO);
			radio1.addSelectionListener(selectionListener);
			
			radio1.setText(Policy.bind("Unmanage.option2")); //$NON-NLS-1$

			radio2 = new Button(composite, SWT.RADIO);
			radio2.addSelectionListener(selectionListener);

			radio2.setText(Policy.bind("Unmanage.option1")); //$NON-NLS-1$
			
			// set initial state
			radio1.setSelection(deleteContent);
			radio2.setSelection(!deleteContent);
			
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.DISCONNECT_ACTION);
			
			return composite;
		}
		
		private SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				if (button.getSelection()) {
					deleteContent = (button == radio1);
				}
			}
		};
		
		public boolean getDeleteContent() {
			return deleteContent;
		}
	}
	
	private boolean deleteContent = false; // true if we want to delete .svn directories, false otherwise
	
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		if(confirmDeleteProjects()) {
			run(getOperation(), true /* cancelable */, PROGRESS_DIALOG);	
		}
	}

    /**
     * get the WorkspaceModifyOperation. The operation will :
     * - delete svn directories if this option has been chosen
     * - unmap the project
     * @return
     */
	private IRunnableWithProgress getOperation() {
		return new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					// maps the selected resources (projects) to their providers
                    Hashtable table = getProviderMapping(getSelectedResources());
					Set keySet = table.keySet();
					monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
					monitor.setTaskName(Policy.bind("Unmanage.unmanaging")); //$NON-NLS-1$
					Iterator iterator = keySet.iterator();
					while (iterator.hasNext()) {
						IProgressMonitor subMonitor = new InfiniteSubProgressMonitor(monitor, 1000);
						subMonitor.beginTask(null, 100);
						SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
						
                        // get the resources (projects) to unmanage for the given provider
                        List list = (List)table.get(provider);
						IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
						for (int i = 0; i < providerResources.length; i++) {
							// get the folder for the project
                            IResource resource = providerResources[i];
							ISVNLocalFolder folder = SVNWorkspaceRoot.getSVNFolderFor((IContainer) resource);
							try {
								if(deleteContent) {
									folder.unmanage(Policy.subMonitorFor(subMonitor, 10));
								}
							} finally {
								// We want to remove the nature even if the unmanage operation fails
								RepositoryProvider.unmap((IProject)resource);							
							}
						}											
					}										
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
	}

    /**
     * open the deltion confirmation dialog
     * returns true if deletion is confirmed 
     */
	boolean confirmDeleteProjects() {
		final int[] result = new int[] { MessageDialog.OK };
		IProject[] projects = getSelectedProjects();
		final DeleteProjectDialog dialog = new DeleteProjectDialog(shell, projects);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] = dialog.open();
			}
		});		
		deleteContent = dialog.getDeleteContent();
// No longer need to show warning message if meta data is being deleted
// since it is now possible to reconnect.
//		if (deleteContent && result[0] == 0) {
//		    String title;
//			if (projects.length == 1)
//				title = Policy.bind("Unmanage.title");  //$NON-NLS-1$
//			else
//				title = Policy.bind("Unmanage.titleN");  //$NON-NLS-1$		    
//		    return MessageDialog.openQuestion(shell, title, Policy.bind("Unmanage.deleteMeta"));//$NON-NLS-1$
//		}
		return result[0] == 0;  // YES
	}
	
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("Unmanage.unmanagingError");//$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForSVNResource(org.tigris.subversion.subclipse.core.ISVNResource)
	 */
	protected boolean isEnabledForSVNResource(ISVNLocalResource svnResource) {
		IResource resource = svnResource.getIResource();
		return resource != null && resource.getType() == IResource.PROJECT;
	}

}
