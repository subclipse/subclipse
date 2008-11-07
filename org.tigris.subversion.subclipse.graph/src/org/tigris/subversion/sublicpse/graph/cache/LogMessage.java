package org.tigris.subversion.sublicpse.graph.cache;

import java.util.Date;

import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

public class LogMessage implements ISVNLogMessage {
	
	private String author;
	private Date date;
	private String message;
	private Number revision;
	private LogMessageChangePath[] changedPaths;
	private LogMessage[] childMessages;

	public LogMessage(long revision, String author, Date date, String message) {
		this.author = author;
		this.date = date;
		this.message = message;
		this.revision = new Number(revision);
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Number getRevision() {
		return revision;
	}

	public void setRevision(Number revision) {
		this.revision = revision;
	}
	
	public void addChild(ISVNLogMessage msg) {
		throw new RuntimeException("Method not implemented");
	}
	
	public boolean hasChildren() {
		return childMessages != null;
	}

	public void setChangedPaths(LogMessageChangePath[] changedPaths) {
		this.changedPaths = changedPaths;
	}

	public ISVNLogMessageChangePath[] getChangedPaths() {
		return changedPaths;
	}

	public ISVNLogMessage[] getChildMessages() {
		return childMessages;
	}

	public void setChildMessages(LogMessage[] childMessages) {
		this.childMessages = childMessages;
	}

	public long getNumberOfChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getTimeMicros() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getTimeMillis() {
		// TODO Auto-generated method stub
		return 0;
	}
}
