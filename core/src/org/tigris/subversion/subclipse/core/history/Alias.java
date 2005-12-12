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
