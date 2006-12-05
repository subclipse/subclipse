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

package org.tigris.subversion.subclipse.mylar;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.mylar.team.AbstractTeamRepositoryProvider;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.actions.CommitAction;

/**
 * Team repository provider for Mylar.
 * 
 * @author Eugene Kuleshov
 */
public class SubclipseTeamRepositoryProvider extends AbstractTeamRepositoryProvider {

	public ActiveChangeSetManager getActiveChangeSetManager() {
		// collectors.add((CVSActiveChangeSetCollector)CVSUIPlugin.getPlugin().getChangeSetManager());
    return SVNProviderPlugin.getPlugin().getChangeSetManager();
	}

	public boolean hasOutgoingChanges(IResource[] resources) {
	  CommitAction commitAction = new CommitAction("");
    commitAction.setSelectedResources(resources);
    return commitAction.hasOutgoingChanges();
	}

	public void commit(IResource[] resources) {
    CommitAction commitAction = new CommitAction("");
    commitAction.setSelectedResources(resources);
    try {
      commitAction.execute(null);
    } catch (InvocationTargetException ex) {
      // ignore
    } catch (InterruptedException ex) {
      // ignore
    }
	}
}

