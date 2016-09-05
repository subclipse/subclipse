package org.tigris.subversion.subclipse.graph.cache;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class Node implements Serializable, IPropertySource, Comparable {
	
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
	private List tags;
	
	private transient Branch branch;
	private transient int branchIndex;
	private transient int graphIndex;

	private static DateFormat dateFormat;

	public static String P_ID_ACTION = "action";
	public static String P_ACTION = "Action";
	public static String P_ID_PATH = "path";
	public static String P_PATH = "Path";	
	public static String P_ID_DATE = "date";
	public static String P_DATE = "Date";	
	public static String P_ID_AUTHOR = "author";
	public static String P_AUTHOR = "Author";
	public static String P_ID_FROM = "from";
	public static String P_FROM = "From";	
	public static String P_ID_MSG = "msg";
	public static String P_MSG = "Message";	
	public static List descriptors;
	static
	{	
		descriptors = new ArrayList();
		descriptors.add(new PropertyDescriptor(P_ID_ACTION, P_ACTION));
		descriptors.add(new PropertyDescriptor(P_ID_PATH, P_PATH));
		descriptors.add(new PropertyDescriptor(P_ID_DATE, P_DATE));
		descriptors.add(new PropertyDescriptor(P_ID_AUTHOR, P_AUTHOR));
		descriptors.add(new PropertyDescriptor(P_ID_MSG, P_MSG));
		descriptors.add(new PropertyDescriptor(P_ID_FROM, P_FROM));
	}		

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
	
	public List getTags() {
		return tags;
	}
	
	public void addTag(Node node) {
		if(tags == null) {
			tags = new ArrayList();
		}
		tags.add(node);
	}
	
	public void setBranch(Branch branch) {
		this.branch = branch;
	}
	
	public Branch getBranch() {
		return branch;
	}
	
	public void setBranchIndex(int branchIndex) {
		this.branchIndex = branchIndex;
	}
	
	public int getBranchIndex() {
		return branchIndex;
	}
	
	public int getGraphIndex() {
		return graphIndex;
	}

	public void setGraphIndex(int graphIndex) {
		this.graphIndex = graphIndex;
	}
	
	public Object getEditableValue() {
		return "r" + revision;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		return (IPropertyDescriptor[])getDescriptors().toArray(new IPropertyDescriptor[getDescriptors().size()]);
	}
	
	private static List getDescriptors() {
		return descriptors;
	}	

	public Object getPropertyValue(Object propKey) {
		if (P_ID_MSG.equals(propKey)) {
			if (message != null) {
				return message;
			}
		}
		if (P_ID_AUTHOR.equals(propKey)) {
			if (author != null) {
				return author;
			}
		}
		if (P_ID_ACTION.equals(propKey)) {
			return action + "";
		}
		if (P_ID_PATH.equals(propKey)) {
			if (path != null) {
				return path;
			}
		}
		if (P_ID_FROM.equals(propKey)) {
			if (copySrcPath != null) {
				return "r"+Long.toString(copySrcRevision)+" "+copySrcPath;
			}
		}
		if (P_ID_DATE.equals(propKey)) {
			if (revisionDate != null) {
				return getDateFormat().format(revisionDate);
			}
		}
		return "";
	}

	public boolean isPropertySet(Object arg0) {
		return false;
	}

	public void resetPropertyValue(Object arg0) {		
	}

	public void setPropertyValue(Object arg0, Object arg1) {
	}
	
	private static DateFormat getDateFormat() {
		if(dateFormat == null) {
			dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		}
		return dateFormat;
	}

	public int compareTo(Object object) {
		if (object instanceof Node) {
			Node compareTo = (Node)object;
			if (compareTo.getRevision() < revision) return 1;
			else if (compareTo.getRevision() > revision) return -1;
			else return 0;
		}
		return 0;
	}	
	
}
