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

package org.tigris.subversion.subclipse.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.ui.console.ConsoleView;
import org.tigris.subversion.subclipse.ui.repository.RepositoryManager;
import org.tigris.subversion.subclipse.ui.repository.model.SVNAdapterFactory;
/**
 * UI Plugin for Subversion provider-specific workbench functionality.
 */
public class SVNUIPlugin extends AbstractUIPlugin {
	/**
	 * The id of the SVN plug-in
	 */
	public static final String ID = "org.tigris.subversion.subclipse.ui"; //$NON-NLS-1$
	public static final String DECORATOR_ID = "org.tigris.subversion.subclipse.ui.decorator"; //$NON-NLS-1$
	

	/**
	 * The singleton plug-in instance
	 */
	private static SVNUIPlugin plugin;
	
	/**
	 * The repository manager
	 */
	private RepositoryManager repositoryManager;
    
    private ImageDescriptors imageDescriptors = new ImageDescriptors();
    
    private Preferences preferences;
	
//	// Property change listener
//	IPropertyChangeListener teamUIListener = new IPropertyChangeListener() {
//		public void propertyChange(PropertyChangeEvent event) {
//			if (event.getProperty().equals(TeamUI.GLOBAL_IGNORES_CHANGED)) {
//				SVNLightweightDecorator.refresh();
//			}
//		}
//	};
		
	/**
	 * SVNUIPlugin constructor
	 * 
	 * @param descriptor  the plugin descriptor
	 */
	public SVNUIPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
	}


	/**
	 * Returns the active workbench page. Note that the active page may not be
	 * the one that the usr perceives as active in some situations so this
	 * method of obtaining the activae page should only be used if no other
	 * method is available.
	 * 
	 * @return the active workbench page
	 */
	public static IWorkbenchPage getActivePage() {
		return TeamUIPlugin.getActivePage();
	}
	
	/**
	 * Creates a busy cursor and runs the specified runnable.
	 * May be called from a non-UI thread.
	 * 
	 * @param parent the parent Shell for the dialog
	 * @param cancelable if true, the dialog will support cancelation
	 * @param runnable the runnable
	 * 
	 * @exception InvocationTargetException when an exception is thrown from the runnable
	 * @exception InterruptedException when the progress monitor is cancelled
	 */

	public static void runWithProgress(Shell parent, boolean cancelable,
		final IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		TeamUIPlugin.runWithProgress(parent, cancelable, runnable);
	}
	
	
	/**
	 * Returns the singleton plug-in instance.
	 * 
	 * @return the plugin instance
	 */
	public static SVNUIPlugin getPlugin() {
		// If the instance has not been initialized, we will wait.
		// This can occur if multiple threads try to load the plugin at the same
		// time (see bug 33825: http://bugs.eclipse.org/bugs/show_bug.cgi?id=33825)
		while (plugin == null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore and keep trying
			}
		}
		return plugin;
	}

	/**
	 * Returns the repository manager
	 * 
	 * @return the repository manager
	 */
	public synchronized RepositoryManager getRepositoryManager() {
		if (repositoryManager == null) {
			repositoryManager = new RepositoryManager();
			try {
				repositoryManager.startup();
			} catch (TeamException e) {
				SVNUIPlugin.log(e.getStatus());
			}
		}
		return repositoryManager;
	}
	
	/**
	 * Convenience method for logging statuses to the plugin log
	 * 
	 * @param status  the status to log
	 */
	public static void log(IStatus status) {
		getPlugin().getLog().log(status);
	}

	public static void log(TeamException e) {
		getPlugin().getLog().log(new Status(e.getStatus().getSeverity(), SVNUIPlugin.ID, 0, Policy.bind("simpleInternal"), e));; //$NON-NLS-1$
	}

	// flags to tailor error reporting
	public static final int PERFORM_SYNC_EXEC = 1;
	public static final int LOG_TEAM_EXCEPTIONS = 2;
	public static final int LOG_CORE_EXCEPTIONS = 4;
	public static final int LOG_OTHER_EXCEPTIONS = 8;
	public static final int LOG_NONTEAM_EXCEPTIONS = LOG_CORE_EXCEPTIONS | LOG_OTHER_EXCEPTIONS;
	
	/**
	 * Convenience method for showing an error dialog 
	 * @param shell a valid shell or null
	 * @param exception the exception to be report
	 * @param title the title to be displayed
	 * @return IStatus the status that was displayed to the user
	 */
	public static IStatus openError(Shell shell, String title, String message, Throwable exception) {
		return openError(shell, title, message, exception, LOG_OTHER_EXCEPTIONS);
	}
	
	/**
	 * Convenience method for showing an error dialog 
	 * @param shell a valid shell or null
	 * @param exception the exception to be report
	 * @param title the title to be displayed
	 * @param flags customizing attributes for the error handling
	 * @return IStatus the status that was displayed to the user
	 */
	public static IStatus openError(Shell providedShell, String title, String message, Throwable exception, int flags) {
		// Unwrap InvocationTargetExceptions
		if (exception instanceof InvocationTargetException) {
			Throwable target = ((InvocationTargetException)exception).getTargetException();
			// re-throw any runtime exceptions or errors so they can be handled by the workbench
			if (target instanceof RuntimeException) {
				throw (RuntimeException)target;
			}
			if (target instanceof Error) {
				throw (Error)target;
			} 
			return openError(providedShell, title, message, target, flags);
		}
		
		// Determine the status to be displayed (and possibly logged)
		IStatus status = null;
		boolean log = false;
		if (exception instanceof CoreException) {
			status = ((CoreException)exception).getStatus();
			log = ((flags & LOG_CORE_EXCEPTIONS) > 0);
		} else if (exception instanceof TeamException) {
			status = ((TeamException)exception).getStatus();
			log = ((flags & LOG_TEAM_EXCEPTIONS) > 0);
		} else if (exception instanceof InterruptedException) {
			return new SVNStatus(IStatus.OK, Policy.bind("ok")); //$NON-NLS-1$
		} else if (exception != null) {
			status = new SVNStatus(IStatus.ERROR, Policy.bind("internal"), exception); //$NON-NLS-1$
			log = ((flags & LOG_OTHER_EXCEPTIONS) > 0);
			if (title == null) title = Policy.bind("SimpleInternal"); //$NON-NLS-1$
		}
		
		// Check for a build error and report it differently
		if (status.getCode() == IResourceStatus.BUILD_FAILED) {
			message = Policy.bind("buildError"); //$NON-NLS-1$
			log = true;
		}
		
		// Check for multi-status with only one child
		if (status.isMultiStatus() && status.getChildren().length == 1) {
			status = status.getChildren()[0];
		}
		if (status.isOK()) return status;
		
		// Log if the user requested it
		if (log) SVNUIPlugin.log(status);
		
		// Create a runnable that will display the error status
		final String displayTitle = title;
		final String displayMessage = message;
		final IStatus displayStatus = status;
		final IOpenableInShell openable = new IOpenableInShell() {
			public void open(Shell shell) {
				if (displayStatus.getSeverity() == IStatus.INFO && !displayStatus.isMultiStatus()) {
					MessageDialog.openInformation(shell, Policy.bind("information"), displayStatus.getMessage()); //$NON-NLS-1$
				} else {
					ErrorDialog.openError(shell, displayTitle, displayMessage, displayStatus);
				}
			}
		};
		openDialog(providedShell, openable, flags);
		
		// return the status we display
		return status;
	}
	
	/**
	 * Interface that allows a shell to be passed to an open method. The
	 * provided shell can be used without sync-execing, etc.
	 */
	public interface IOpenableInShell {
		public void open(Shell shell);
	}
	
	/**
	 * Open the dialog code provided in the IOpenableInShell, ensuring that 
	 * the provided whll is valid. This method will provide a shell to the
	 * IOpenableInShell if one is not provided to the method.
	 * 
	 * @param providedShell
	 * @param openable
	 * @param flags
	 */
	public static void openDialog(Shell providedShell, final IOpenableInShell openable, int flags) {
		// If no shell was provided, try to get one from the active window
		if (providedShell == null) {
			IWorkbenchWindow window = SVNUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				providedShell = window.getShell();
				// sync-exec when we do this just in case
				flags = flags | PERFORM_SYNC_EXEC;
			}
		}

		// Create a runnable that will display the error status
		final Shell shell = providedShell;
		Runnable outerRunnable = new Runnable() {
			public void run() {
				Shell displayShell;
				if (shell == null) {
					Display display = Display.getCurrent();
					displayShell = new Shell(display);
				} else {
					displayShell = shell;
				}
				openable.open(displayShell);
				if (shell == null) {
					displayShell.dispose();
				}
			}
		};

		// Execute the above runnable as determined by the parameters
		if (shell == null || (flags & PERFORM_SYNC_EXEC) > 0) {
			Display display;
			if (shell == null) {
				display = Display.getCurrent();
				if (display == null) {
					display = Display.getDefault();
				}
			} else {
				display = shell.getDisplay();
			}
			display.syncExec(outerRunnable);
		} else {
			outerRunnable.run();
		}
	}


	/**
	 * @see Plugin#startup()
	 */
	public void startup() throws CoreException {
		super.startup();
		Policy.localize("org.tigris.subversion.subclipse.ui.messages"); //$NON-NLS-1$
        
		SVNAdapterFactory factory = new SVNAdapterFactory();
		
		Platform.getAdapterManager().registerAdapters(factory, ISVNRemoteFile.class); 
		Platform.getAdapterManager().registerAdapters(factory, ISVNRemoteFolder.class); 
		Platform.getAdapterManager().registerAdapters(factory, ISVNRepositoryLocation.class);
//		Platform.getAdapterManager().registerAdapters(factory, RepositoryRoot.class);
		
        // we initialize the image descriptors
		imageDescriptors.initializeImages(getDescriptor().getInstallURL());
		
        preferences = new Preferences(getPreferenceStore());
		preferences.initializePreferences();
		
//		// if the global ignores list is changed then update decorators.
//		TeamUI.addPropertyChangeListener(teamUIListener);
		
		ConsoleView.startup();
	}
	
	/**
	 * @see Plugin#shutdown()
	 */
	public void shutdown() throws CoreException {
		super.shutdown();
//		TeamUI.removePropertyChangeListener(listener);
		try {
			if (repositoryManager != null)
				repositoryManager.shutdown();
		} catch (TeamException e) {
			throw new CoreException(e.getStatus());
		}

		ConsoleView.shutdown();
	}
	

    /**
     * Returns the image descriptor for the given image ID.
     * Returns null if there is no such image.
     */
    public ImageDescriptor getImageDescriptor(String id) {
        return imageDescriptors.getImageDescriptor(id);
    }    
    
}
