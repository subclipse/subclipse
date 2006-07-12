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

import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class SVNChangeSetCollector extends SyncInfoSetChangeSetCollector {
    /*
     * Constant used to add the collector to the configuration of a page so
     * it can be accessed by the SVN custom actions
     */
    public static final String SVN_CHECKED_IN_COLLECTOR = SVNUIPlugin.ID + ".SVNCheckedInCollector"; //$NON-NLS-1$

	public SVNChangeSetCollector(ISynchronizePageConfiguration configuration) {
		super(configuration);
        configuration.setProperty(SVNChangeSetCollector.SVN_CHECKED_IN_COLLECTOR, this);
	}

	protected void add(SyncInfo[] infos) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.ChangeSetManager#initializeSets()
	 */
	protected void initializeSets() {
		// Do nothing
	}
}
