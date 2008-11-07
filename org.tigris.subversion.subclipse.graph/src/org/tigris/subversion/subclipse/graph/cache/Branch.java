package org.tigris.subversion.subclipse.graph.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Branch implements Serializable {
	
	private static final long serialVersionUID = -1236475833029223413L;

	private static final Comparator COMPARATOR = new Comparator() {
		public int compare(Object a, Object b) {
			long ra;
			long rb;
			if(a instanceof Long) {
				ra = ((Long) a).longValue();
			} else if(a instanceof Node) {
				ra = ((Node) a).getRevision();
			} else {
				throw new RuntimeException();
			}
			if(b instanceof Long) {
				rb = ((Long) b).longValue();
			} else if(b instanceof Node) {
				rb = ((Node) b).getRevision();
			} else {
				throw new RuntimeException();
			}
			if(ra < rb) {
				return -1;
			} else if(ra > rb) {
				return 1;
			}
			return 0;
		}
	};
	
	private String path;
	private List nodes = new ArrayList();
	private Node lastNode;
	
	private transient Object view;
	
	public Object getView() {
		return view;
	}

	public void setView(Object view) {
		this.view = view;
	}

	public Branch(String path) {
		this.path = path;
	}
	
	public void addNode(Node n) {
		nodes.add(n);
		lastNode = n;
	}
	
	public Node getLastNode() {
		return lastNode;
	}

	public String getPath() {
		return path;
	}

	public List getNodes() {
		return nodes;
	}
	
	public void end() {
		lastNode = null;
	}
	
	public Node getSource(long revision) {
		int index = Collections.binarySearch(nodes, new Long(revision), COMPARATOR);
		if(index < 0) {
			index = -index-2;
			if(index < 0) {
				return null;
			}
		}
		Node previous = (Node) nodes.get(index);
		if(previous.getAction() == 'D' && previous.getRevision() < revision)
			return null;
		return previous;
	}

	public boolean isEnded() {
		return lastNode == null && !nodes.isEmpty();
		// nodes is empty for the root node, but that doesn't mean that
		// the branch is ended
	}

}
