/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.operations;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;


/**
 * This class is the abstract superclass for SVN operations. It provides
 * error handling, prompting and other UI.
 */
public abstract class SVNOperation extends TeamOperation {

	private int statusCount;

	private boolean involvesMultipleResources = false;

	private List errors = new ArrayList(); // of IStatus

	protected static final IStatus OK = Status.OK_STATUS; //$NON-NLS-1$
	
	private Shell shell;
	
	// instance variable used to indicate behavior while prompting for overwrite
	private boolean confirmOverwrite = true;
	
	protected SVNOperation(IWorkbenchPart part) {
		super(part);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamOperation#getJobName()
	 */
	protected String getJobName() {
		return getTaskName();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#getOperationIcon()
	 */
	protected URL getOperationIcon() {
		try {
			URL baseURL = SVNUIPlugin.getPlugin().getBundle().getEntry("/"); //$NON-NLS-1$
			return  new URL(baseURL, ISVNUIConstants.ICON_PATH + ISVNUIConstants.IMG_CHECKOUT);
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		startOperation();
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100);
			monitor.setTaskName(getTaskName());
			execute(Policy.subMonitorFor(monitor, 100));
			endOperation();
		} catch (SVNException e) {
			// TODO: errors may not be empty (i.e. endOperation has not been executed)
			if (!e.operationInterrupted()) {
				throw new InvocationTargetException(e);
			}
		} finally {
			monitor.done();
		}
	}
	
	protected void startOperation() {
		statusCount = 0;
		resetErrors();
		confirmOverwrite = true;
	}
	
	protected void endOperation() throws SVNException {
		handleErrors((IStatus[]) errors.toArray(new IStatus[errors.size()]));
	}

	/**
	 * Subclasses must override this method to perform the operation.
	 * Clients should never call this method directly.
	 * 
	 * @param monitor
	 * @throws SVNException
	 * @throws InterruptedException
	 */
	protected abstract void execute(IProgressMonitor monitor) throws SVNException, InterruptedException;

	protected void addError(IStatus status) {
		if (status.isOK()) return;
		if (isLastError(status)) return;
		errors.add(status);
	}

	protected void collectStatus(IStatus status)  {
		if (isLastError(status)) return;
		statusCount++;
		if (!status.isOK()) addError(status);
	}
	
	protected void resetErrors() {
		errors.clear();
		statusCount = 0;
	}
	
	/**
	 * Get the last error taht occured. This can be useful when a method
	 * has a return type but wants to signal an error. The method in question
	 * can add the error using <code>addError(IStatus)</code> and return null.
	 * The caller can then query the error using this method. Also, <code>addError(IStatus)</code>
	 * will not add the error if it is already on the end of the list (using identity comparison)
	 * which allows the caller to still perform a <code>collectStatus(IStatus)</code>
	 * to get a valid operation count.
	 * @return
	 */
	protected IStatus getLastError() {
		Assert.isTrue(errors.size() > 0);
		IStatus status = (IStatus)errors.get(errors.size() - 1);
		return status;
	}
	
	private boolean isLastError(IStatus status) {
		return (errors.size() > 0 && getLastError() == status);
	}
	
	protected void handleErrors(IStatus[] errors) throws SVNException {
		if (errors.length == 0) return;
		if (errors.length == 1 && statusCount == 1)  {
			throw new SVNException(errors[0]);
		}
		MultiStatus result = new MultiStatus(SVNUIPlugin.ID, 0, getErrorMessage(errors, statusCount), null);
		for (int i = 0; i < errors.length; i++) {
			IStatus s = errors[i];
			if (s.isMultiStatus()) {
				result.add(new SVNStatus(s.getSeverity(), s.getMessage(), s.getException()));
				result.addAll(s);
			} else {
				result.add(s);
			}
		}
		throw new SVNException(result);
	}

	protected String getErrorMessage(IStatus[] failures, int totalOperations) {
		return Policy.bind("SVNOperation.0", String.valueOf(failures.length),  String.valueOf(totalOperations)); //$NON-NLS-1$
	}

	/**
	 * This method prompts the user to overwrite an existing resource. It uses the
	 * <code>involvesMultipleResources</code> to determine what buttons to show.
	 * @param project
	 * @return
	 */
	protected boolean promptToOverwrite(final String title, final String msg) {
		if (!confirmOverwrite) {
			return true;
		}
		final String buttons[];
		if (involvesMultipleResources()) {
			buttons = new String[] {
				IDialogConstants.YES_LABEL, 
				IDialogConstants.YES_TO_ALL_LABEL, 
				IDialogConstants.NO_LABEL, 
				IDialogConstants.CANCEL_LABEL};
		} else {
			buttons = new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL};
		}	
		final Shell displayShell = getShell();
		if (displayShell == null) {
			// We couldn't get a shell (perhaps due to shutdown)
			return false;
		}

		// run in syncExec because callback is from an operation,
		// which is probably not running in the UI thread.
		final int[] code = new int[] {0};
		displayShell.getDisplay().syncExec(
			new Runnable() {
				public void run() {
					MessageDialog dialog = 
						new MessageDialog(displayShell, title, null, msg, MessageDialog.QUESTION, buttons, 0);
					dialog.open();
					code[0] = dialog.getReturnCode();
				}
			});
		if (involvesMultipleResources()) {
			switch (code[0]) {
				case 0://Yes
					return true;
				case 1://Yes to all
					confirmOverwrite = false; 
					return true;
				case 2://No
					return false;
				case 3://Cancel
				default:
					throw new OperationCanceledException();
			}
		} else {
			return code[0] == 0;
		}
	}

	/**
	 * This method is used by <code>promptToOverwrite</code> to determine which 
	 * buttons to show in the prompter.
	 * 
	 * @return
	 */
	protected boolean involvesMultipleResources() {
		return involvesMultipleResources;
	}

	public void setInvolvesMultipleResources(boolean b) {
		involvesMultipleResources = b;
	}

	/**
	 * Return the string that is to be used as the task name for the operation
	 * 
	 * @param remoteFolders
	 * @return
	 */
	protected abstract String getTaskName();
	
	/**
	 * Return true if any of the accumulated status have a severity of ERROR
	 * @return
	 */
	protected boolean errorsOccurred() {
		for (Iterator iter = errors.iterator(); iter.hasNext();) {
			IStatus status = (IStatus) iter.next();
			if (status.getSeverity() == IStatus.ERROR) return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamOperation#getShell()
	 */
	protected Shell getShell() {
		// Use the shell assigned to the operation if possible
		if (shell != null && !shell.isDisposed()) {
			return shell;
		}
		return super.getShell();
	}
	
	/**
	 * Set the shell to be used by the operation. This only needs
	 * to be done if the operation does not have a workbench part.
	 * For example, if the operation is being run in a wizard.
	 * @param shell The shell to set.
	 */
	public void setShell(Shell shell) {
		this.shell = shell;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	protected boolean canRunAsJob() {
		// Put SVN jobs in the background by default.
		return true;
	}
	
	public void showCancelledMessage() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(getShell(), getJobName(), Policy.bind("SVNOperation.operationCancelled")); //$NON-NLS-1$
			}			
		});
	}
	
//	protected ISchedulingRule getSchedulingRule() {
//		// XXX IGORF consider locking affected projects only  
//		return ResourcesPlugin.getWorkspace().getRoot();
//	}
}
