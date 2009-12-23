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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.util.IPromptCondition;

/**
 * SVNAction is the common superclass for all SVN actions. It provides
 * facilities for enablement handling, standard error handling, selection
 * retrieval and prompting.
 */
abstract public class SVNAction extends ReplaceableIconAction {
	
	private List accumulatedStatus = new ArrayList();
	
	/**
	 * Common run method for all SVN actions.
	 */
	final public void run(IAction action) {
		try {
			if (!beginExecution(action)) return;
			execute(action);
			endExecution();
		} catch (InvocationTargetException e) {
			// Handle the exception and any accumulated errors
			handle(e);
		} catch (InterruptedException e) {
			// Show any problems that have occurred so far
			handle(null);
		}
	}

	/**
	 * This method gets invoked before the <code>SVNAction#execute(IAction)</code>
	 * method. It can preform any pre-checking and initialization required before 
	 * the action is executed. Subclasses may override but must invoke this
	 * inherited method to ensure proper initialization of this superclass is performed.
	 * These included preparation to accumulate IStatus and checking for dirty editors.
	 */
	protected boolean beginExecution(IAction action) {
		accumulatedStatus.clear();
		if(needsToSaveDirtyEditors()) {
			if(!saveAllEditors()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Actions must override to do their work.
	 */
	abstract protected void execute(IAction action) throws InvocationTargetException, InterruptedException;

	/**
	 * This method gets invoked after <code>SVNAction#execute(IAction)</code>
	 * if no exception occurred. Subclasses may override but should invoke this
	 * inherited method to ensure proper handling oy any accumulated IStatus.
	 */
	protected void endExecution() {
		if ( ! accumulatedStatus.isEmpty()) {
			handle(null);
		}
	}
	
	/**
	 * Add a status to the list of accumulated status. 
	 * These will be provided to method handle(Exception, IStatus[])
	 * when the action completes.
	 */
	protected void addStatus(IStatus status) {
		accumulatedStatus.add(status);
	}
	
	/**
	 * Return the list of status accumulated so far by the action. This
	 * will include any OK status that were added using addStatus(IStatus)
	 */
	protected IStatus[] getAccumulatedStatus() {
		return (IStatus[]) accumulatedStatus.toArray(new IStatus[accumulatedStatus.size()]);
	}
	
	/**
	 * Return the title to be displayed on error dialogs.
	 * Subclasses should override to present a custom message.
	 */
	protected String getErrorTitle() {
		return Policy.bind("SVNAction.errorTitle"); //$NON-NLS-1$
	}
	
	/**
	 * Return the title to be displayed on error dialogs when warnings occur.
	 * Subclasses should override to present a custom message.
	 */
	protected String getWarningTitle() {
		return Policy.bind("SVNAction.warningTitle"); //$NON-NLS-1$
	}

	/**
	 * Return the message to be used for the parent MultiStatus when 
	 * multiple errors occur during an action.
	 * Subclasses should override to present a custom message.
	 */
	protected String getMultiStatusMessage() {
		return Policy.bind("SVNAction.multipleProblemsMessage"); //$NON-NLS-1$
	}
	
	/**
	 * Return the status to be displayed in an error dialog for the given list
	 * of non-OK status.
	 * 
	 * This method can be overridden by subclasses. Returning an OK status will 
	 * prevent the error dialog from being shown.
	 */
	protected IStatus getStatusToDisplay(IStatus[] problems) {
		if (problems.length == 1) {
			return problems[0];
		}
		MultiStatus combinedStatus = new MultiStatus(SVNUIPlugin.ID, 0, getMultiStatusMessage(), null); //$NON-NLS-1$
		for (int i = 0; i < problems.length; i++) {
			combinedStatus.merge(problems[i]);
		}
		return combinedStatus;
	}
	
	/**
	 * Method that implements generic handling of an exception. 
	 * 
	 * Thsi method will also use any accumulated status when determining what
	 * information (if any) to show the user.
	 * 
	 * @param exception the exception that occured (or null if none occured)
	 * @param status any status accumulated by the action before the end of 
	 * the action or the exception occured.
	 */
	protected void handle(Exception exception) {
		if (exception instanceof SVNException) {
			if (((SVNException)exception).operationInterrupted()) {
				return;
			}
		}
		// Get the non-OK statii
		List problems = new ArrayList();
		IStatus[] status = getAccumulatedStatus();
		if (status != null) {
			for (int i = 0; i < status.length; i++) {
				IStatus iStatus = status[i];
				if ( ! iStatus.isOK() || iStatus.getCode() == SVNStatus.SERVER_ERROR) {
					problems.add(iStatus);
				}
			}
		}
		// Handle the case where there are no problem statii
		if (problems.size() == 0) {
			if (exception == null) return;
			handle(exception, getErrorTitle(), null);
			return;
		}

		// For now, display both the exception and the problem status
		// Later, we can determine how to display both together
		if (exception != null) {
			handle(exception, getErrorTitle(), null);
		}
		
		String message = null;
		IStatus statusToDisplay = getStatusToDisplay((IStatus[]) problems.toArray(new IStatus[problems.size()]));
		if (statusToDisplay.isOK()) return;
		if (statusToDisplay.isMultiStatus() && statusToDisplay.getChildren().length == 1) {
			message = statusToDisplay.getMessage();
			statusToDisplay = statusToDisplay.getChildren()[0];
		}
		String title;
		if (statusToDisplay.getSeverity() == IStatus.ERROR) {
			title = getErrorTitle();
		} else {
			title = getWarningTitle();
		}
		SVNUIPlugin.openError(getShell(), title, message, new SVNException(statusToDisplay));
	}

	/**
	 * Convenience method for running an operation with the appropriate progress.
	 * Any exceptions are propogated so they can be handled by the
	 * <code>SVNAction#run(IAction)</code> error handling code.
	 * 
	 * @param runnable  the runnable which executes the operation
	 * @param cancelable  indicate if a progress monitor should be cancelable
	 * @param progressKind  one of PROGRESS_BUSYCURSOR or PROGRESS_DIALOG
	 */
	final protected void run(final IRunnableWithProgress runnable, boolean cancelable, int progressKind) throws InvocationTargetException, InterruptedException {
		final Exception[] exceptions = new Exception[] {null};
		
		// Ensure that no repository view refresh happens until after the action
		final IRunnableWithProgress innerRunnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                SVNUIPlugin.getPlugin().getRepositoryManager().run(runnable, monitor);
			}
		};
		
		switch (progressKind) {
			case PROGRESS_BUSYCURSOR :
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					public void run() {
						try {
							innerRunnable.run(new NullProgressMonitor());
						} catch (InvocationTargetException e) {
							exceptions[0] = e;
						} catch (InterruptedException e) {
							exceptions[0] = e;
						}
					}
				});
				break;
			case PROGRESS_DIALOG :
			default :
				new ProgressMonitorDialog(getShell()).run(true, cancelable,/*cancelable, true, */innerRunnable);	
				break;
		}
		if (exceptions[0] != null) {
			if (exceptions[0] instanceof InvocationTargetException)
				throw (InvocationTargetException)exceptions[0];
			else
				throw (InterruptedException)exceptions[0];
		}
	}
	
	/**
	 * Answers if the action would like dirty editors to saved
	 * based on the SVN preference before running the action. By
	 * default, SVNActions do not save dirty editors.
	 */
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	/**
	 * Find the object associated with the selected object that is adapted to
	 * the provided class.
	 * 
	 * @param selection
	 * @param c
	 * @return Object
	 */
	public static Object getAdapter(Object selection, Class c) {
		if (c.isInstance(selection)) {
			return selection;
		}
		if (selection instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) selection;
			Object adapter = a.getAdapter(c);
			if (c.isInstance(adapter)) {
				return adapter;
			}
		}
		return null;
	}
	
	/**
	 * Get selected SVN remote folders
	 */
	protected ISVNRemoteFolder[] getSelectedRemoteFolders() {
		ArrayList resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof ISVNRemoteFolder) {
					resources.add(next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(ISVNRemoteFolder.class);
					if (adapter instanceof ISVNRemoteFolder) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			return (ISVNRemoteFolder[])resources.toArray(new ISVNRemoteFolder[resources.size()]);
		}
		return new ISVNRemoteFolder[0];
	}

	
	/**
	 * Returns the selected remote files
	 */
	protected ISVNRemoteFile[] getSelectedRemoteFiles() {
		ArrayList resources = null;
		if (selection != null && !selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof ISVNRemoteFile) {
					resources.add(next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(ISVNRemoteFile.class);
					if (adapter instanceof ISVNRemoteFile) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ISVNRemoteFile[] result = new ISVNRemoteFile[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ISVNRemoteFile[0];
	}	
	
	/**
	 * Returns the selected remote resources
	 */
	protected ISVNRemoteResource[] getSelectedRemoteResources() {
		ArrayList resources = null;
		if (selection != null && !selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof ISVNRemoteResource) {
					resources.add(next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(ISVNRemoteResource.class);
					if (adapter instanceof ISVNRemoteResource) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ISVNRemoteResource[] result = new ISVNRemoteResource[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ISVNRemoteResource[0];
	}
		
	/**
	 * Based on the SVN preference for saving dirty editors this method will either
	 * ignore dirty editors, save them automatically, or prompt the user to save them.
	 * 
	 * @return <code>true</code> if the command succeeded, and <code>false</code>
	 * if at least one editor with unsaved changes was not saved
	 */
	private boolean saveAllEditors() {
		final int option = SVNUIPlugin.getPlugin().getPreferenceStore().getInt(ISVNUIConstants.PREF_SAVE_DIRTY_EDITORS);
		final boolean[] okToContinue = new boolean[] {true};
		if (option != ISVNUIConstants.OPTION_NEVER) {		
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					boolean confirm = option == ISVNUIConstants.OPTION_PROMPT;
					okToContinue[0] = PlatformUI.getWorkbench().saveAllEditors(confirm);
				}
			});
		} 
		return okToContinue[0];
	}
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.TeamAction#handle(java.lang.Exception, java.lang.String, java.lang.String)
	 */
	protected void handle(Exception exception, String title, String message) {
		SVNUIPlugin.openError(getShell(), title, message, exception, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS);
	}

	/**
	 * A helper prompt condition for prompting for SVN dirty state.
	 * @param dirtyResources Resources that have been modified
	 * @return IPromptCondition that prompts when a resource is in the <code>dirtyResources</code> list
	 */
	public static IPromptCondition getOverwriteLocalChangesPrompt(final IResource[] dirtyResources) {
		return new IPromptCondition() {
			List resources = Arrays.asList(dirtyResources);
			public boolean needsPrompt(IResource resource) {
				return resources.contains(resource);
			}
			public String promptMessage(IResource resource) {
				return Policy.bind("ReplaceWithAction.localChanges", resource.getName());//$NON-NLS-1$
			}
		};
	}
}
