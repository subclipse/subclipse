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

public class Tags {
	private Alias[] tags;
	
	public Tags() {
		super();
	}
	
	public Tags(Alias[] tags) {
		this();
		this.tags = tags;
	}

	public Alias[] getTags() {
		return tags;
	}

	public void setTags(Alias[] tags) {
		this.tags = tags;
	}

}