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

import org.eclipse.jface.action.Action;
import org.eclipse.team.ui.TeamImages;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * Action that removed the CVS console from the console view. The console
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
