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
package org.tigris.subversion.subclipse.ui.actions;

import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 */
public class ReplaceWithBaseRevisionAction extends ReplaceWithRemoteAction {

	public ReplaceWithBaseRevisionAction() {
		super(SVNRevision.BASE);
	}
}
