package test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

public class LogMessage implements ISVNLogMessage {
	
	private String author;
	private Date date;
	private String message;
	private Number revision;
	private List changePaths = new ArrayList();

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
		throw new RuntimeException("Method not implemented");
	}
	
	public LogMessage addChangePath(ISVNLogMessageChangePath changePath) {
		changePaths.add(changePath);
		return this;
	}

	public ISVNLogMessageChangePath[] getChangedPaths() {
		return (ISVNLogMessageChangePath[]) changePaths.toArray(new ISVNLogMessageChangePath[0]);
	}

	public ISVNLogMessage[] getChildMessages() {
		// TODO Auto-generated method stub
		return null;
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
