package org.tigris.subversion.subclipse.graph.cache;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class Graph implements Serializable, IPropertySource {

	private static final String[] EMPTY_STRING = {};
	private static final long serialVersionUID = -5285462558875510455L;
	
	private String rootPath;
	private Map branches = new HashMap();
	private List paths = new ArrayList();
	
	private transient String[] pathsArray = null;
	private transient String selectedPath;
	private transient long selectedRevision;
	private transient Node selectedNode;
	
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
	
	public Node getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(Node selectedNode) {
		this.selectedNode = selectedNode;
	}

	public Graph(String rootPath) {
		this.rootPath = rootPath;
	}
	
	public String getSelectedPath() {
		return selectedPath;
	}

	public void setSelectedPath(String selectedPath) {
		this.selectedPath = selectedPath;
	}

	public long getSelectedRevision() {
		return selectedRevision;
	}

	public void setSelectedRevision(long selectedRevision) {
		this.selectedRevision = selectedRevision;
	}

	public Branch addBranch(String path) {
		Branch b = new Branch(path);
		branches.put(path, b);
		paths.add(path);
		pathsArray = null;
		return b;
	}
	
	public List getPaths() {
		return paths;
	}
	
	public Branch getBranch(String path) {
		return (Branch) branches.get(path);
	}
	
	public Node[] getNodes() {
		List nodes = new ArrayList();
		Set keySet = branches.keySet();
		Iterator iter = keySet.iterator();
		while (iter.hasNext()) {
			Branch branch = (Branch)branches.get(iter.next());
			List branchNodes = branch.getNodes();
			Iterator nodeIter = branchNodes.iterator();
			while (nodeIter.hasNext()) {
				nodes.add(nodeIter.next());
			}
		}
		Node[] nodeArray = new Node[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}
	
	public String getRootPath() {
		return rootPath;
	}

	public String[] getPathsAsArray() {
		if(pathsArray == null)
			pathsArray = (String[]) paths.toArray(EMPTY_STRING);
		return pathsArray;
	}

	public Object getEditableValue() {
		return "";
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		return (IPropertyDescriptor[])getDescriptors().toArray(new IPropertyDescriptor[getDescriptors().size()]);
	}
	
	private static List getDescriptors() {
		return descriptors;
	}	

	public Object getPropertyValue(Object propKey) {
		if (getSelectedNode() == null) return "";
		if (P_ID_MSG.equals(propKey)) {
			if (getSelectedNode().getMessage() != null) {
				return getSelectedNode().getMessage();
			}
		}
		if (P_ID_AUTHOR.equals(propKey)) {
			if (getSelectedNode().getAuthor() != null) {
				return getSelectedNode().getAuthor();
			}
		}
		if (P_ID_ACTION.equals(propKey)) {
			return getSelectedNode().getAction() + "";
		}
		if (P_ID_PATH.equals(propKey)) {
			if (getSelectedNode().getPath() != null) {
				return getSelectedNode().getPath();
			}
		}
		if (P_ID_FROM.equals(propKey)) {
			if (getSelectedNode().getCopySrcPath() != null) {
				return "r"+Long.toString(getSelectedNode().getCopySrcRevision())+" "+getSelectedNode().getCopySrcPath();
			}
		}
		if (P_ID_DATE.equals(propKey)) {
			if (getSelectedNode().getRevisionDate() != null) {
				return getDateFormat().format(getSelectedNode().getRevisionDate());
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
	
}
