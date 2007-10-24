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

import org.eclipse.jface.action.Action;
import org.eclipse.team.ui.TeamImages;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * Action that removed the SVN console from the console view. The console
 * can be re-added via the console view "Open Console" drop-down.
 * 
 */
public class ConsoleRemoveAction extends Action {

	ConsoleRemoveAction() {
		this.setText(Policy.bind("ConsoleRemoveAction.label")); //$NON-NLS-1$
		setToolTipText(Policy.bind("ConsoleRemoveAction.tooltip")); //$NON-NLS-1$
		setImageDescriptor(TeamImages.getImageDescriptor("elcl16/participant_rem.gif")); //$NON-NLS-1$
		setDisabledImageDescriptor(TeamImages.getImageDescriptor("dlcl16/participant_rem.gif")); //$NON-NLS-1$
	}
	
	public void run() {
		SVNOutputConsoleFactory.closeConsole();
	}
}