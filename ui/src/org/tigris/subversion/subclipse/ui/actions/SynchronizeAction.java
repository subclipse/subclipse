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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.subscriber.SVNSynchronizeParticipant;

/**
 * Action to synchronize the selected resources. This results
 * in a file-system participant being added to the synchronize view.
 */
public class SynchronizeAction extends WorkbenchWindowAction {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        if (action != null && !action.isEnabled()) { 
        	action.setEnabled(true);
        } 
        else {
    		IResource[] resources = getSelectedResources();
    		// First check if there is an existing matching participant
    		SVNSynchronizeParticipant participant = (SVNSynchronizeParticipant)SubscriberParticipant.getMatchingParticipant(SVNSynchronizeParticipant.ID, resources);
    		// If there isn't, create one and add to the manager
    		if (participant == null) {
    			participant = new SVNSynchronizeParticipant(new ResourceScope(resources));
    			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
    		}
    		// If called by the accelerator key, for some reason targetPart is null, thus the check
    		if(getTargetPart() == null) {
    			//System.out.println("site:null"+ SVNUIPlugin.getActivePage().getActivePart().getSite());
    			participant.refresh(resources, "Synchronizing", "Synchronizing " + participant.getName(), SVNUIPlugin.getActivePage().getActivePart().getSite());
    		} else {
    			participant.refresh(resources, "Synchronizing", "Synchronizing " + participant.getName(), getTargetPart().getSite());
    		}
        } 
	}

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_SYNC;
	}

}
