package org.tigris.subversion.subclipse.graph.cache;

public class Path {
	private String path;
	private transient int index;
	
	public Path(String path) {
		super();
		this.path = path;
	}

	public String getPath() {
		return path;
	}
	
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
