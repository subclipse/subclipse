/*******************************************************************************
 * copied from: org.eclipse.team.internal.ui.actions.TeamAction
 * 
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.internal;


import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionDelegate;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * The abstract superclass of all Team actions. This class contains some convenience
 * methods for getting selected objects and mapping selected objects to their
 * providers.
 * 
 * Team providers may subclass this class when creating their actions.
 * Team providers may also instantiate or subclass any of the  
 * subclasses of TeamAction provided in this package.
 */
public abstract class TeamAction extends ActionDelegate implements IObjectActionDelegate, IViewActionDelegate {
	// The current selection
	protected IStructuredSelection selection;
	
	// The shell, required for the progress dialog
	protected Shell shell;

	// Constants for determining the type of progress. Subclasses may
	// pass one of these values to the run method.
	public final static int PROGRESS_DIALOG = 1;
	public final static int PROGRESS_BUSYCURSOR = 2;

	private IWorkbenchPart targetPart;

	/**
	 * Creates an array of the given class type containing all the
	 * objects in the selection that adapt to the given class.
	 * 
	 * @param selection
	 * @param c
	 * @return
	 */
	public static Object[] getSelectedAdaptables(ISelection selection, Class c) {
		ArrayList result = null;
		if (!selection.isEmpty()) {
			result = new ArrayList();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object adapter = getAdapter(elements.next(), c);
				if (c.isInstance(adapter)) {
					result.add(adapter);
				}
			}
		}
		if (result != null && !result.isEmpty()) {
			return result.toArray((Object[])Array.newInstance(c, result.size()));
		}
		return (Object[])Array.newInstance(c, 0);
	}
	
	/**
	 * Find the object associated with the given object when it is adapted to
	 * the provided class. Null is returned if the given object does not adapt
	 * to the given class
	 * 
	 * @param selection
	 * @param c
	 * @return Object
	 */
	public static Object getAdapter(Object adaptable, Class c) {
		if (c.isInstance(adaptable)) {
			return adaptable;
		}
		if (adaptable instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) adaptable;
			Object adapter = a.getAdapter(c);
			if (c.isInstance(adapter)) {
				return adapter;
			}
		}
		return null;
	}
	
	/**
	 * Returns the selected projects.
	 * 
	 * @return the selected projects
	 */
	protected IProject[] getSelectedProjects() {
		IResource[] selectedResources = getSelectedResources();
		if (selectedResources.length == 0) return new IProject[0];
		ArrayList projects = new ArrayList();
		for (int i = 0; i < selectedResources.length; i++) {
			IResource resource = selectedResources[i];
			if (resource.getType() == IResource.PROJECT) {
				projects.add(resource);
			}
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}
	
	/**
	 * Returns an array of the given class type c that contains all
	 * instances of c that are either contained in the selection or
	 * are adapted from objects contained in the selection.
	 * 
	 * @param c
	 * @return
	 */
	protected Object[] getSelectedResources(Class c) {
		return getSelectedAdaptables(selection, c);
	}
	
	/**
	 * Returns the selected resources.
	 * 
	 * @return the selected resources
	 */
	protected IResource[] getSelectedResources() {
		return (IResource[])getSelectedResources(IResource.class);
	}
	
	/**
	 * Convenience method for getting the current shell.
	 * 
	 * @return the shell
	 */
	public Shell getShell() {
		if (shell != null) {
			return shell;
		} else {
			IWorkbench workbench = SVNUIPlugin.getPlugin().getWorkbench();
			if (workbench == null) return null;
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window == null) return null;
			return window.getShell();
		}
	}
	/**
	 * Convenience method for running an operation with progress and
	 * error feedback.
	 * 
	 * @param runnable  the runnable which executes the operation
	 * @param problemMessage  the message to display in the case of errors
	 * @param progressKind  one of PROGRESS_BUSYCURSOR or PROGRESS_DIALOG
	 */
	final protected void run(final IRunnableWithProgress runnable, final String problemMessage, int progressKind) {
		final Exception[] exceptions = new Exception[] {null};
		switch (progressKind) {
			case PROGRESS_BUSYCURSOR :
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					public void run() {
						try {
							runnable.run(new NullProgressMonitor());
						} catch (InvocationTargetException e) {
							exceptions[0] = e;
						} catch (InterruptedException e) {
							exceptions[0] = null;
						}
					}
				});
				break;
			default :
			case PROGRESS_DIALOG :
				try {
					new ProgressMonitorDialog(getShell()).run(true, true, runnable);
				} catch (InvocationTargetException e) {
					exceptions[0] = e;
				} catch (InterruptedException e) {
					exceptions[0] = null;
				}
				break;
		}
		if (exceptions[0] != null) {
			handle(exceptions[0], null, problemMessage);
		}
	}
	
	/*
	 * Method declared on IActionDelegate.
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) sel;
			if (action != null) {
				setActionEnablement(action);
			}
		}
	}
	
	/**
	 * Method invoked from <code>selectionChanged(IAction, ISelection)</code> 
	 * to set the enablement status of the action. The instance variable 
	 * <code>selection</code> will contain the latest selection so the methods
	 * <code>getSelectedResources()</code> and <code>getSelectedProjects()</code>
	 * will provide the proper objects.
	 * 
	 * This method can be overridden by subclasses but should not be invoked by them.
	 */
	protected void setActionEnablement(IAction action) {
		try {
			action.setEnabled(isEnabled());
		} catch (TeamException e) {
			if (e.getStatus().getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
				// Enable the action to allow the user to discover the problem
				action.setEnabled(true);
			} else {
				action.setEnabled(false);
				// We should not open a dialog when determining menu enablements so log it instead
				SVNUIPlugin.log(e);
			}
		}
	}
	
	/*
	 * Method declared on IObjectActionDelegate.
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		if(targetPart != null) {
			this.shell = targetPart.getSite().getShell();
			this.targetPart = targetPart;
		}
	}
	/**
	 * Shows the given errors to the user.
	 * 
	 * @param status  the status containing the error
	 * @param title  the title of the error dialog
	 * @param message  the message for the error dialog
	 * @param shell  the shell to open the error dialog in
	 */
	protected void handle(Exception exception, String title, String message) {
		Utils.handleError(getShell(), exception, title, message);
	}
	
	/**
	 * Concrete action enablement code.
	 * Subclasses must implement.
	 * 
	 * @return whether the action is enabled
	 * @throws TeamException if an error occurs during enablement detection
	 */
	abstract protected boolean isEnabled() throws TeamException;
	
	/**
	 * Convenience method that maps the selected resources to their providers.
	 * The returned Hashtable has keys which are ITeamProviders, and values
	 * which are Lists of IResources that are shared with that provider.
	 * 
	 * @return a hashtable mapping providers to their selected resources
	 */
	protected Hashtable getProviderMapping() {
		return getProviderMapping(getSelectedResources());
	}
	/**
	 * Convenience method that maps the given resources to their providers.
	 * The returned Hashtable has keys which are ITeamProviders, and values
	 * which are Lists of IResources that are shared with that provider.
	 * 
	 * @return a hashtable mapping providers to their resources
	 */
	protected Hashtable getProviderMapping(IResource[] resources) {
		Hashtable result = new Hashtable();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}
	
	/**
	 * @return IWorkbenchPart
	 */
	protected IWorkbenchPart getTargetPart() {
		return targetPart;
	}

	/**
	 * Return the path that was active when the menu item was selected.
	 * @return IWorkbenchPage
	 */
	protected IWorkbenchPage getTargetPage() {
		if (getTargetPart() == null) return SVNUIPlugin.getActivePage();
		return getTargetPart().getSite().getPage();
	}
	
	/**
	 * Show the view with the given ID in the perspective from which the action
	 * was executed. Returns null if the view is not registered.
	 * 
	 * @param viewId
	 * @return IViewPart
	 */
	protected IViewPart showView(String viewId) {
		try {
			return getTargetPage().showView(viewId);
		} catch (PartInitException pe) {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view) {
		targetPart = view;
	}
}
