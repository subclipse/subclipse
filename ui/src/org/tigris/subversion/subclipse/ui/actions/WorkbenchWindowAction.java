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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.internal.provisional.action.IToolBarContributionItem;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Helper class for actions that are displayed in a toolbar
 */
public abstract class WorkbenchWindowAction extends WorkspaceAction implements IWorkbenchWindowActionDelegate {

	protected abstract String getMenuId();
	
	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow workbenchWindow) {
		setIcon();

		ApplicationWindow window = (ApplicationWindow)workbenchWindow;
		
		CoolBarManager  coolBarmgr  = (CoolBarManager)window.getCoolBarManager();
		CoolItem[] items = coolBarmgr.getControl().getItems();
		
		for (int i = 0; i < items.length; ++i)
		{
			IContributionItem contribItem = (IContributionItem) items[i].getData();
			if (!"org.tigris.subversion.subclipse.actionSet".equals(contribItem.getId()))  //$NON-NLS-1$
				continue;

			IToolBarContributionItem toolBarContribItem = (IToolBarContributionItem)contribItem;

			IToolBarManager toolBarMgr = toolBarContribItem.getToolBarManager();
			IContributionItem updateItem = toolBarMgr.find(getMenuId());

			if (updateItem == null)
			{
				SVNUIPlugin.log(IStatus.ERROR, "updateItem == null", null);
				break;
			}

			if (getAction().getImageDescriptor() == null)
				updateItem.setVisible(false);
			else
				updateItem.setVisible(true);
			toolBarMgr.update(true);
			coolBarmgr.update(true);
			
			break;
		}
	}

}
