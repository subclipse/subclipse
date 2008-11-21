package org.tigris.subversion.subclipse.graph.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph implements Serializable {

	private static final String[] EMPTY_STRING = {};
	private static final long serialVersionUID = -5285462558875510455L;
	
	private String rootPath;
	private Map branches = new HashMap();
	private List paths = new ArrayList();
	
	private transient String[] pathsArray = null;
	private transient String selectedPath;
	private transient long selectedRevision;
	private transient Node selectedNode;
	
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
	
}
