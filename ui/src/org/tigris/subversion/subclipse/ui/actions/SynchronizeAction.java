/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.tigris.subversion.subclipse.ui.subscriber.SVNSynchronizeParticipant;

/**
 * Action to synchronize the selected resources. This results
 * in a file-system participant being added to the synchronize view.
 */
public class SynchronizeAction extends WorkspaceAction {
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void execute(IAction action) {
		IResource[] resources = getSelectedResources();
		// First check if there is an existing matching participant
		SVNSynchronizeParticipant participant = (SVNSynchronizeParticipant)SubscriberParticipant.getMatchingParticipant(SVNSynchronizeParticipant.ID, resources);
		// If there isn't, create one and add to the manager
		if (participant == null) {
			participant = new SVNSynchronizeParticipant(new ResourceScope(resources));
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
		}
		participant.refresh(resources, "Synchronizing", "Synchronizing " + participant.getName(), getTargetPart().getSite());

	}

}
