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
package org.tigris.subversion.subclipse.ui.repository.model;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

/**
 * A simple job scheduling rule for serializing jobs for an ICVSRepositoryLocation
 */
public class RepositoryLocationSchedulingRule implements ISchedulingRule {
	ISVNRepositoryLocation location;
	public RepositoryLocationSchedulingRule(ISVNRepositoryLocation location) {
		this.location = location;
	}
	public boolean isConflicting(ISchedulingRule rule) {
		if(rule instanceof RepositoryLocationSchedulingRule) {
			return ((RepositoryLocationSchedulingRule)rule).location.equals(location);
		}
		return false;
	}
	public boolean contains(ISchedulingRule rule) {
		return isConflicting(rule);
	}
}
