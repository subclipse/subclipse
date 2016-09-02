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

public class Alias implements Comparable {
	private int revision;
	private String name;
	private String relativePath;
	private boolean branch;
	private String url;

	public Alias() {
		super();
	}
	
	public Alias(int revision, String name, String relativePath, String url) {
		this();
		this.revision = revision;
		this.name = name;
		this.relativePath = relativePath;
		this.url = url;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public boolean equals(Object object) {
		if (!(object instanceof Alias)) return false;
		return ((Alias)object).getName().equals(name);
	}

	public String toString() {
		return revision + "," + name + "," + relativePath + " URL: " + url;
	}
	
	public int compareTo(Object object) {
		Alias compare = (Alias)object;
		if (revision > compare.getRevision()) return 1;
		if (compare.getRevision() > revision) return -1;
		return 0;
	}

	public boolean isBranch() {
		return branch;
	}

	public void setBranch(boolean branch) {
		this.branch = branch;
	}

}
