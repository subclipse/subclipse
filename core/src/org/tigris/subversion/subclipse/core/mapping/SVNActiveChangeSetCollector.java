/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.mapping;

import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.SubscriberChangeSetManager;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;

/**
 *
 */
public class SVNActiveChangeSetCollector extends SubscriberChangeSetManager {

	public SVNActiveChangeSetCollector(Subscriber subscriber) {
		super(subscriber);
	}

	protected ActiveChangeSet doCreateSet(String name) {
		return new SVNActiveChangeSet(this, name);
	}
}
