package org.tigris.subversion.subclipse.core.history;

public class Branches {
	private Tag[] branches;
	
	public Branches() {
		super();
	}
	
	public Branches(Tag[] branches) {
		this();
		this.branches = branches;
	}

	public Tag[] getBranches() {
		return branches;
	}

	public void setBranches(Tag[] branches) {
		this.branches = branches;
	}

}
