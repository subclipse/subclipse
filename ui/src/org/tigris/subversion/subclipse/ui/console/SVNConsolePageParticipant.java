/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
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