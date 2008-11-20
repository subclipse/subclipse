package org.tigris.subversion.subclipse.graph.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.XYAnchor;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.cache.Branch;
import org.tigris.subversion.subclipse.graph.cache.Cache;
import org.tigris.subversion.subclipse.graph.cache.Graph;
import org.tigris.subversion.subclipse.graph.cache.Node;

public class GraphEditPart2 extends AbstractGraphicalEditPart implements MouseListener {
	private Graph graph;
	private NodeFigure selected;
	private ScrollingGraphicalViewer viewer;
	private Map branchMap = new HashMap();
	private Map nodeMap = new HashMap();
	private List connections = new ArrayList();
	private IPreferenceStore store = Activator.getDefault().getPreferenceStore();

	public GraphEditPart2(Graph graph, ScrollingGraphicalViewer viewer) {
		super();
		this.graph = graph;
		this.viewer = viewer;
	}

	protected IFigure createFigure() {
		Figure f = new Figure();
		f.setBackgroundColor(ColorConstants.white);
		f.setOpaque(true);

		XYLayout layout = new XYLayout();
		f.setLayoutManager(layout);

		return f;
	}

	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, null);
	}

	protected List getModelChildren() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int showDeleted = store.getInt(RevisionGraphEditor.SHOW_DELETED_PREFERENCE);
		
		Graph graph = (Graph)getModel();
		List paths = graph.getPaths();
		List branches = new ArrayList();
		Iterator iter = paths.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Branch branch = graph.getBranch((String)iter.next());
			
			if(branch.getNodes().size() == 1) {
				Node firstNode = (Node) branch.getNodes().iterator().next();
				if(firstNode.getSource() != null && firstNode.getChildCount() == 0) {
					// is not the root node and is not the target of any arrow
					// therefore is a tag
					branch.setView(null);
					continue;
				}
			}
			
			if (branch.getNodes().size() > 0) {
				Node lastNode = (Node)branch.getNodes().get(branch.getNodes().size() - 1);
				if (lastNode.getAction() == 'D' && Cache.isEqualsOrParent(lastNode.getPath(), branch.getPath())) {
					if (showDeleted == RevisionGraphEditor.SHOW_DELETED_NO || (showDeleted == RevisionGraphEditor.SHOW_DELETED_MODIFIED && !isModified(branch))) {
						// branch has been deleted and item was not modified in that location.
						// do not show this branch, unless it is the location where the item came
						// into existence.
						if (!graph.getRootPath().equals(branch.getPath())) {
							branch.setView(null);
							continue;
						}
					}
				}
			}			
			
			branch.setIndex(i++);
			branches.add(branch);

			int mod = branch.getIndex() % Activator.BG_COLORS.length;
			Color bgcolor = Activator.BG_COLORS[mod];
			Color fgcolor = Activator.FG_COLORS[mod];
			BranchFigure branchFigure = new BranchFigure(branch.getPath(), bgcolor, fgcolor);
			branchMap.put(branch.getPath(), branchFigure);
		}
		
		iter = branches.iterator();
		while (iter.hasNext()) {
			Branch branch = (Branch)iter.next();
			Iterator nodeIter = branch.getNodes().iterator();
			while (nodeIter.hasNext()) {
				Node node = (Node)nodeIter.next();
				int mod = branch.getIndex() % Activator.BG_COLORS.length;
				Color bgcolor = Activator.BG_COLORS[mod];
				Color fgcolor = Activator.FG_COLORS[mod];
				NodeFigure nodeFigure = new NodeFigure(node, bgcolor, fgcolor);
				nodeFigure.addMouseListener(this);
				nodeMap.put(node, nodeFigure);
			}
		}
		
		iter = branches.iterator();
		while (iter.hasNext()) {
			Branch branch = (Branch)iter.next();
			Iterator nodeIter = branch.getNodes().iterator();
			while (nodeIter.hasNext()) {
				Node node = (Node)nodeIter.next();
				NodeFigure nodeFigure = (NodeFigure)nodeMap.get(node);
				if(node.getParent() != null) {
					NodeFigure target = (NodeFigure)nodeMap.get(node.getParent());
					if(target != null) {
						makeConnection(getFigure(), target, nodeFigure);
					}
				} else if(node.getSource() != null) {
					NodeFigure target = (NodeFigure)nodeMap.get(node.getSource());
					if(target != null) {
						makeConnection(getFigure(), target, nodeFigure);
					}
				} else {
					BranchFigure branchFigure = (BranchFigure)branchMap.get(branch.getPath());
					makeConnection(getFigure(), branchFigure, nodeFigure);
				}				
			}
		}
		
		// Merged Connections
		iter = branches.iterator();
		while (iter.hasNext()) {
			Branch branch = (Branch)iter.next();
			for (Iterator it = branch.getNodes().iterator(); it.hasNext();) {
				Node node = (Node) it.next();
				List mergedRevisions = node.getMergedRevisions();
				if(mergedRevisions == null)
					continue;				
				NodeFigure nodeFigure = (NodeFigure) nodeMap.get(node);
				for (Iterator iterator = node.getMergedRevisions().iterator(); iterator
						.hasNext();) {
					Node merged = (Node) iterator.next();
					NodeFigure mergedView = (NodeFigure)nodeMap.get(merged);
					if(mergedView != null)
						makeConnection(getFigure(), mergedView, nodeFigure, ColorConstants.red);
				}				
			}
		}

		Branch selectedBranch = graph.getBranch(graph.getSelectedPath());
		if (selectedBranch != null) {
			Node n = selectedBranch.getSource(graph.getSelectedRevision());
			NodeFigure nodeFigure = (NodeFigure)nodeMap.get(n);
			if(nodeFigure != null) {
				selectNode(nodeFigure);
				// FIXME: it doesn't work
//				scrollTo((Rectangle) contentsLayout.getConstraint(nodeFigure)); 
			}
		}
		
		setConnectionVisibility(selected);
		
		return branches;
	}
	
	public BranchFigure getBranchFigure(String path) {
		return (BranchFigure)branchMap.get(path);
	}
	
	public NodeFigure getNodeFigure(Node node) {
		return (NodeFigure)nodeMap.get(node);
	}
	
	public List getNodes(Branch branch) {
		return (List)nodeMap.get(branch);
	}
	
	private boolean isModified(Branch branch) {
		List nodes = branch.getNodes();
		Iterator iter = nodes.iterator();
		while (iter.hasNext()) {
			Node node = (Node)iter.next();
			if (node.getPath().equals(branch.getPath()) && node.getAction() == 'M') return true;
		}
		return false;
	}

	protected void refreshVisuals() {
		super.refreshVisuals();
	}
	
	public void setConnectionVisibility(NodeFigure figure) {
		Iterator iter = connections.iterator();
		while (iter.hasNext()) {
			PolylineConnection con = (PolylineConnection)iter.next();
			boolean show = !store.getBoolean(RevisionGraphEditor.FILTER_CONNECTIONS) || (con.getSourceAnchor().getOwner() == figure || con.getTargetAnchor().getOwner() == figure);
			con.setVisible(show);
		}		
	}
	
	private void selectNode(NodeFigure figure) {
		setConnectionVisibility(figure);
		
		if(selected != null)
			selected.setSelected(false);
		figure.setSelected(true);
		selected = figure;
		if (figure == null) graph.setSelectedNode(null);
		else graph.setSelectedNode(figure.getNode());
	}	

	public NodeFigure getSelectedNode() {
		return selected;
	}	
	
	private PolylineConnection makeConnection(IFigure contents, IFigure source, NodeFigure target) {
		return makeConnection(contents, source, target, Activator.CONNECTION_COLOR);
	}

	private PolylineConnection makeConnection(IFigure contents, IFigure source, NodeFigure target, Color color) {
		PolylineConnection c = new PolylineConnection();
		ConnectionAnchor targetAnchor = new ChopboxAnchor(target);
		c.setTargetAnchor(targetAnchor);
		c.setSourceAnchor(new ChopboxAnchor(source));
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
		c.setTargetDecoration(decoration);
		c.setForegroundColor(color);
		ConnectionMouseListener listener = new ConnectionMouseListener(c);
		c.addMouseMotionListener(listener);
		c.addMouseListener(listener);
		c.setCursor(Cursors.HAND);
		contents.add(c);
		connections.add(c);
		return c;
	}
	
	private void scrollTo(Rectangle fbounds) {
		scrollTo(fbounds.x+fbounds.width/2, fbounds.y+fbounds.height/2);
	}
	
	private void scrollTo(int ax, int ay) {
		Viewport viewport = ((FigureCanvas)viewer.getControl()).getViewport();
		Rectangle vbounds = viewport.getBounds();
		Point p = new Point(ax, ay);
//		target.translateToAbsolute(p); // TODO
		int x = p.x-vbounds.width/2;
		int y = p.y-vbounds.height/2;
		viewport.setHorizontalLocation(x);
		viewport.setVerticalLocation(y);
	}

	private void scrollTo(IFigure target) {
		scrollTo(target.getBounds());
	}
	
	public void mouseDoubleClicked(MouseEvent event) {
	}

	public void mousePressed(MouseEvent event) {
	}

	public void mouseReleased(MouseEvent event) {
		Object source = event.getSource();
		if (source instanceof NodeFigure) {
			selectNode((NodeFigure) source);
		}
	}
	
	class ConnectionMouseListener implements MouseMotionListener, MouseListener {

		private PolylineConnection connection;

		public ConnectionMouseListener(PolylineConnection connection) {
			this.connection = connection;
		}

		public void mouseDragged(MouseEvent event) {
		}

		public void mouseEntered(MouseEvent event) {
		}

		public void mouseExited(MouseEvent event) {
			connection.setLineWidth(1);
		}

		public void mouseHover(MouseEvent event) {
			connection.setLineWidth(2);
		}

		public void mouseMoved(MouseEvent event) {
		}

		public void mouseDoubleClicked(MouseEvent event) {
		}

		public void mousePressed(MouseEvent event) {
		}

		public void mouseReleased(MouseEvent event) {
			NodeFigure nodeFigure = (NodeFigure) connection.getTargetAnchor().getOwner();
			scrollTo(nodeFigure);
			selectNode(nodeFigure);
		}

	} class MyXYAnchor extends XYAnchor {

		private IFigure f;

		public MyXYAnchor(Point point, IFigure f) {
			super(point);
			this.f = f;
		}

		public Point getLocation(Point reference) {
			Point p = super.getLocation(reference).getCopy();
			f.translateToAbsolute(p);
			return p;
		}

		public IFigure getOwner() {
			return f;
		}

	}
	
}
