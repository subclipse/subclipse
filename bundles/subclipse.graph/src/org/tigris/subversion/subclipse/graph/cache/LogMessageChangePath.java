package org.tigris.subversion.subclipse.graph.cache;

import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

public class LogMessageChangePath implements ISVNLogMessageChangePath {
	
	private char action;
	private String copySrcPath;
	private Number copySrcRevision;
	private String path;

	public LogMessageChangePath(char action, String path, String copySrcPath,
			long copySrcRevision) {
		this.action = action;
		this.copySrcPath = copySrcPath;
		this.copySrcRevision = new Number(copySrcRevision);
		this.path = path;
	}

	public LogMessageChangePath(char action, String path) {
		this.action = action;
		this.path = path;
	}

	public char getAction() {
		return action;
	}

	public void setAction(char action) {
		this.action = action;
	}

	public String getCopySrcPath() {
		return copySrcPath;
	}

	public void setCopySrcPath(String copySrcPath) {
		this.copySrcPath = copySrcPath;
	}

	public Number getCopySrcRevision() {
		return copySrcRevision;
	}

	public void setCopySrcRevision(Number copySrcRevision) {
		this.copySrcRevision = copySrcRevision;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
