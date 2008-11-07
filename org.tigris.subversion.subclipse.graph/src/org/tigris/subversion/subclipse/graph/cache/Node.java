package org.tigris.subversion.subclipse.graph.cache;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Node implements Serializable {
	
	private static final long serialVersionUID = 2835522933811459843L;
	
	// fields read from log messages
	private long revision;
	private String author;
	private Date revisionDate;
	private String message;
	private String path;
	private char action;
	private long copySrcRevision;
	private String copySrcPath;
	
	// other fields
	private Node parent;
	private Node source;
	private int childCount;
	private transient Object view;
	
	private List mergedRevisions;

	public Node() {
	}
	
	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
		if(parent != null)
			parent.childCount++;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getRevisionDate() {
		return revisionDate;
	}

	public void setRevisionDate(Date revisionDate) {
		this.revisionDate = revisionDate;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public char getAction() {
		return action;
	}

	public void setAction(char action) {
		this.action = action;
	}

	public long getCopySrcRevision() {
		return copySrcRevision;
	}

	public void setCopySrcRevision(long copySrcRevision) {
		this.copySrcRevision = copySrcRevision;
	}

	public String getCopySrcPath() {
		return copySrcPath;
	}

	public void setCopySrcPath(String copySrcPath) {
		this.copySrcPath = copySrcPath;
	}
	
	public String toString() {
		String pattern = "{0} by {1} {3} on {2} -- {4} --";
		return MessageFormat.format(pattern, 
				new Object[]{ new Long(revision), 
				author, path, action+"", message});
	}

	public Object getView() {
		return view;
	}

	public void setView(Object view) {
		this.view = view;
	}
	
	public int getChildCount() {
		return childCount;
	}

	public Node getSource() {
		return source;
	}

	public void setSource(Node source) {
		this.source = source;
		if(source != null)
			source.childCount++;
	}
	
	public List getMergedRevisions() {
		return mergedRevisions;
	}
	
	public void addMergedRevision(Node node) {
		if(mergedRevisions == null) {
			mergedRevisions = new ArrayList();
		}
		mergedRevisions.add(node);
	}
	
}
