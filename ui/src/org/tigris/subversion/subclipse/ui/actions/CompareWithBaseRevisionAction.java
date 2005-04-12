/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;
 
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CompareWithBaseRevisionAction extends CompareWithRemoteAction {

	/**
	 * Creates a new compare action that will compare against the BASE revision
	 */
	public CompareWithBaseRevisionAction() {
		super(SVNRevision.BASE);
	}
}
