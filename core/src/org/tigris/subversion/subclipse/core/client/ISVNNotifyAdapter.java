/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.tigris.subversion.svnclientadapter.SVNNodeKind;

public class ISVNNotifyAdapter implements IConsoleListener {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#setCommand(int)
	 */
	public void setCommand(int command) {
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logCommandLine(java.lang.String)
	 */
	public void logCommandLine(String commandLine) {
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logMessage(java.lang.String)
	 */
	public void logMessage(String message) {
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logError(java.lang.String)
	 */
	public void logError(String message) {
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logRevision(long, java.lang.String)
	 */
	public void logRevision(long revision, String path) {
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logCompleted(java.lang.String)
	 */
	public void logCompleted(String message) {
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#onNotify(java.io.File, org.tigris.subversion.svnclientadapter.SVNNodeKind)
	 */
	public void onNotify(File path, SVNNodeKind kind) {
	}

}
