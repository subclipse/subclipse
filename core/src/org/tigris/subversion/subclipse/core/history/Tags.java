package org.tigris.subversion.subclipse.core.history;

public class Tags {
	private Tag[] tags;
	
	public Tags() {
		super();
	}
	
	public Tags(Tag[] tags) {
		this();
		this.tags = tags;
	}

	public Tag[] getTags() {
		return tags;
	}

	public void setTags(Tag[] tags) {
		this.tags = tags;
	}

}