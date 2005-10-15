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

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * @author Brock Janiczak
 */
public class SVNOutputConsoleFactory implements IConsoleFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
	 */
	public void openConsole() {
		showConsole();
	}
	
	public static void showConsole() {
		SVNOutputConsole console = SVNUIPlugin.getPlugin().getConsole();
		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] existing = manager.getConsoles();
			boolean exists = false;
			for (int i = 0; i < existing.length; i++) {
				if(console == existing[i])
					exists = true;
			}
			if(! exists)
				manager.addConsoles(new IConsole[] {console});
			manager.showConsoleView(console);
		}
	}
	
	public static void closeConsole() {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		SVNOutputConsole console = SVNUIPlugin.getPlugin().getConsole();
		if (console != null) {
			manager.removeConsoles(new IConsole[] {console});
			ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.new MyLifecycle());
		}
	}
}
