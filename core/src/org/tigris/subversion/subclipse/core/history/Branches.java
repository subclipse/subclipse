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
package org.tigris.subversion.subclipse.core.history;

public class Branches {
	private Alias[] branches;
	
	public Branches() {
		super();
	}
	
	public Branches(Alias[] branches) {
		this();
		this.branches = branches;
	}

	public Alias[] getBranches() {
		return branches;
	}

	public void setBranches(Alias[] branches) {
		this.branches = branches;
	}

}
