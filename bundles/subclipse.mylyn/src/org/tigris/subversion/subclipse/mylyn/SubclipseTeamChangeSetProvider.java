/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eugene Kuleshov - initial API and implementation
 ******************************************************************************/

package org.tigris.subversion.subclipse.mylyn;

import org.eclipse.mylyn.team.ui.AbstractActiveChangeSetProvider;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

/**
 * Team repository provider for Mylyn.
 * 
 * @author Eugene Kuleshov
 */
public class SubclipseTeamChangeSetProvider extends
		AbstractActiveChangeSetProvider {

	public ActiveChangeSetManager getActiveChangeSetManager() {
		return SVNProviderPlugin.getPlugin().getChangeSetManager();
	}
}
