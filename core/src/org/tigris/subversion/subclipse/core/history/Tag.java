package org.tigris.subversion.subclipse.core.history;

public class Tag implements Comparable {
	private int revision;
	private String name;
	private String relativePath;
	private boolean branch;
	private String tagUrl;

	public Tag() {
		super();
	}
	
	public Tag(int revision, String name, String relativePath, String tagUrl) {
		this();
		this.revision = revision;
		this.name = name;
		this.relativePath = relativePath;
		this.tagUrl = tagUrl;
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

	public String getTagUrl() {
		return tagUrl;
	}

	public void setTagUrl(String tagUrl) {
		this.tagUrl = tagUrl;
	}
	
	public boolean equals(Object object) {
		if (!(object instanceof Tag)) return false;
		return ((Tag)object).getName().equals(name);
	}

	public String toString() {
		return revision + "," + name + "," + relativePath + " URL: " + tagUrl;
	}
	
	public int compareTo(Object object) {
		Tag compare = (Tag)object;
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
