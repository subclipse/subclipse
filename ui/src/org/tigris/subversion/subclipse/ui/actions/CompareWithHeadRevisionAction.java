/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;
 
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CompareWithHeadRevisionAction extends CompareWithRemoteAction {

	/**
	 * Creates a new compare action that will compare against the HEAD revision
	 */
	public CompareWithHeadRevisionAction() {
		super(SVNRevision.HEAD);
	}
}
