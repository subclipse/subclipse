/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 
*******************************************************************************/
package org.tigris.subversion.subclipse.ui.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.*;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Console helper that allows contributing actions to the console view when
 * the SVN console is visible. Added to the console via an extension point
 * from org.eclipse.ui.console.
 * 
 */
public class SVNConsolePageParticipant implements IConsolePageParticipant {

	private ConsoleRemoveAction consoleRemoveAction;
	
	public void init(IPageBookViewPage page, IConsole console) {
		this.consoleRemoveAction = new ConsoleRemoveAction();
		IActionBars bars = page.getSite().getActionBars();
		bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
	}

	public void dispose() {
		this.consoleRemoveAction = null;
	}

	public void activated() {
	}

	public void deactivated() {
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
}