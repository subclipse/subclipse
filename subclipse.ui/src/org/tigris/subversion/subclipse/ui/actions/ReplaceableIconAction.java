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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.internal.TeamAction;

/**
 * Helper class for actions that are displayed in a toolbar
 */
public abstract class ReplaceableIconAction extends TeamAction {

	private IAction action        = null;
	private boolean isInitialized = false;
	
	/**
	 * Returns the id of the image for this menu entry
	 * @return the id of the image for this menu entry
	 */
	protected String getImageId() {
		return null;
	}
	
	/*
	 * @see org.eclipse.ui.actions.ActionDelegate#init(org.eclipse.jface.action.IAction)
	 */
	public void init(IAction action) {
		super.init(action);

		this.action = action;

		setIcon();
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.internal.TeamAction#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		super.setActivePart(action, targetPart);
		
		if (!isInitialized)
		{
			setIcon();
			isInitialized = true;
		}
	}

	protected void setIcon() {
		String iconName = getImageId();
		
		if (iconName != null && action != null) {
			ImageDescriptor descriptor = SVNUIPlugin.getPlugin().getImageDescriptor(iconName); 
			action.setImageDescriptor(descriptor);
		}
	}
	
	/*
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose()
	{
		super.dispose();
		action = null;
	}

	protected IAction getAction() {
		return action;
	}
}
