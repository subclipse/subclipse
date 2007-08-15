/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;


import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class SVNChangeSetCapability extends ChangeSetCapability {  

	public ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration, IDiff[] infos) {
        ActiveChangeSet set = getActiveChangeSetManager().createSet(Policy.bind("WorkspaceChangeSetCapability_1"), new IDiff[0]); 
		CommitSetDialog dialog = new CommitSetDialog(configuration.getSite().getShell(), set, getResources(infos),
				Policy.bind("WorkspaceChangeSetCapability_2"), Policy.bind("WorkspaceChangeSetCapability_3")); // 
		dialog.open();
		if (dialog.getReturnCode() != Window.OK) return null;
		set.add(infos);
		return set;
	}

    private IResource[] getResources(IDiff[] diffs) {
    	Set result = new HashSet();
    	for (int i = 0; i < diffs.length; i++) {
			IDiff diff = diffs[i];
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null)
				result.add(resource);
		}
        return (IResource[]) result.toArray(new IResource[result.size()]);
    }
    
	public void editChangeSet(ISynchronizePageConfiguration configuration, ActiveChangeSet set) {
        CommitSetDialog dialog = new CommitSetDialog(configuration.getSite().getShell(), set, set.getResources(),
        		Policy.bind("WorkspaceChangeSetCapability_7"), Policy.bind("WorkspaceChangeSetCapability_8"), true); // 
		dialog.open();
//		if (dialog.getReturnCode() != Window.OK) return;
		// Nothing to do here as the set was updated by the dialog 
	}

	
    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ChangeSetCapability#supportsCheckedInChangeSets()
     */
    public boolean supportsCheckedInChangeSets() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ChangeSetCapability#supportsActiveChangeSets()
     */
    public boolean supportsActiveChangeSets() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ChangeSetCapability#createCheckedInChangeSetCollector(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
     */
    public SyncInfoSetChangeSetCollector createSyncInfoSetChangeSetCollector(ISynchronizePageConfiguration configuration) {
        return new SVNChangeSetCollector(configuration);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ChangeSetCapability#getActionGroup()
     */
    public SynchronizePageActionGroup getActionGroup() {
//        return new CVSChangeSetActionGroup();
    	return null;
    }
    

	/* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ChangeSetCapability#enableChangeSetsByDefault()
     */
    public boolean enableChangeSetsByDefault() {
        return SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_COMMIT_SET_DEFAULT_ENABLEMENT);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ChangeSetCapability#getActiveChangeSetManager()
     */
    public ActiveChangeSetManager getActiveChangeSetManager() {
    	return SVNProviderPlugin.getPlugin().getChangeSetManager();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ChangeSetCapability#enableActiveChangeSetsFor(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
     */
    public boolean enableActiveChangeSetsFor(ISynchronizePageConfiguration configuration) {
    	return this.supportsActiveChangeSets() && configuration.getMode() != ISynchronizePageConfiguration.INCOMING_MODE;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ChangeSetCapability#enableCheckedInChangeSetsFor(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
     */
    public boolean enableCheckedInChangeSetsFor(ISynchronizePageConfiguration configuration) {
    	return this.supportsCheckedInChangeSets() && configuration.getMode() != ISynchronizePageConfiguration.OUTGOING_MODE;
    }
}
