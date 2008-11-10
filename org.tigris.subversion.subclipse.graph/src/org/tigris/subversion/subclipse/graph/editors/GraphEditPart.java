package org.tigris.subversion.subclipse.graph.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.cache.Branch;
import org.tigris.subversion.subclipse.graph.cache.Cache;
import org.tigris.subversion.subclipse.graph.cache.Graph;
import org.tigris.subversion.subclipse.graph.cache.Node;

public class GraphEditPart extends AbstractGraphicalEditPart implements MouseListener {

	private Graph graph;
	private NodeFigure selected;
	private List connections = new ArrayList();
	private IPreferenceStore store = Activator.getDefault().getPreferenceStore();

	private final static int NODE_WIDTH = 50;
	private final static int NODE_HEIGHT = 30;
	private final static int BRANCH_WIDTH = 220;
	private final static int BRANCH_HEIGHT = 30;
	private final static int BRANCH_OFFSET = BRANCH_WIDTH+20;
	private final static int NODE_OFFSET_Y = 10;
	private final static int NODE_OFFSET_X = (BRANCH_WIDTH - NODE_WIDTH) / 2;

	private ScrollingGraphicalViewer viewer;

	public GraphEditPart(Graph graph, ScrollingGraphicalViewer viewer) {
		this.graph = graph;
		this.viewer = viewer;
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

	protected IFigure createFigure() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int showDeleted = store.getInt(RevisionGraphEditor.SHOW_DELETED_PREFERENCE);
		Figure contents = new Figure();
		contents.setBackgroundColor(ColorConstants.white);
		contents.setOpaque(true);
		XYLayout contentsLayout = new XYLayout();
		contents.setLayoutManager(contentsLayout);

		List paths = graph.getPaths();

		int i = 0;
		// create nodes
		for (Iterator iter = paths.iterator(); iter.hasNext(); i++) {
			String path = (String) iter.next();

			Branch branch = graph.getBranch(path);
			if(branch.getNodes().size() == 1) {
				Node firstNode = (Node) branch.getNodes().iterator().next();
				if(firstNode.getSource() != null && firstNode.getChildCount() == 0) {
					// is not the root node and is not the target of any arrow
					// therefore is a tag
					i--;
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
							i--;
							continue;
						}
					}
				}
			}

			// both are supposed to have the same length
			int mod = i % Activator.BG_COLORS.length;
			Color bgcolor = Activator.BG_COLORS[mod];
			Color fgcolor = Activator.FG_COLORS[mod];

			BranchFigure branchFigure = new BranchFigure(path, bgcolor, fgcolor);
			branch.setView(branchFigure);
			contents.add(branchFigure);

			Rectangle rect = new Rectangle(10+i*BRANCH_OFFSET, 10, BRANCH_WIDTH, BRANCH_HEIGHT);
			contentsLayout.setConstraint(branchFigure, rect);

			int x = rect.x + NODE_OFFSET_X;
			int figureIndex = 0;
			for (Iterator it = branch.getNodes().iterator(); it.hasNext(); figureIndex++) {
				Node node = (Node) it.next();

				int y = NODE_OFFSET_Y + rect.y + rect.height;
				int height = NODE_HEIGHT; // + ARROW_PADDING * node.getChildCount();

				NodeFigure nodeFigure = new NodeFigure(node, bgcolor, fgcolor);
				contents.add(nodeFigure);
				node.setView(nodeFigure);
				nodeFigure.addMouseListener(this);

				rect = new Rectangle(x, y, NODE_WIDTH, height);
				contentsLayout.setConstraint(nodeFigure, rect);
			}
		}
		
		// create connections
		for (Iterator iter = paths.iterator(); iter.hasNext(); i++) {
			String path = (String) iter.next();
			Branch branch = graph.getBranch(path);
			if(branch.getView() == null) {
				for (Iterator it = branch.getNodes().iterator(); it.hasNext();) {
					Node node = (Node) it.next();
					if(node.getSource() != null && node.getSource().getView() != null) {
						Node lastNode = (Node)branch.getNodes().get(branch.getNodes().size() - 1);
						if (lastNode.getAction() != 'D' || !Cache.isEqualsOrParent(lastNode.getPath(), branch.getPath())) {
							NodeFigure nodeFigure = (NodeFigure) node.getSource().getView();
							nodeFigure.addTag(node);
						}
					}
				}
			} else {
				for (Iterator it = branch.getNodes().iterator(); it.hasNext();) {
					Node node = (Node) it.next();
					NodeFigure nodeFigure = (NodeFigure) node.getView();

					if(node.getParent() != null) {
						NodeFigure target = (NodeFigure) node.getParent().getView();
						if(target != null) {
							makeConnection(contents, target, nodeFigure);
						}
					} else if(node.getSource() != null) {
						NodeFigure target = (NodeFigure) node.getSource().getView();
						if(target != null) {
							makeConnection(contents, target, nodeFigure);
//							PolylineConnection c = 
//							target.addConnection(c, node);
						}
					} else {
						makeConnection(contents, (BranchFigure) branch.getView(), nodeFigure);
					}
				}
			}
		}
		
		// merged connections
		for (Iterator iter = paths.iterator(); iter.hasNext(); i++) {
			String path = (String) iter.next();
			Branch branch = graph.getBranch(path);
			if(branch.getView() != null) {
				for (Iterator it = branch.getNodes().iterator(); it.hasNext();) {
					Node node = (Node) it.next();
					List mergedRevisions = node.getMergedRevisions();
					if(mergedRevisions == null)
						continue;
					
					NodeFigure nodeFigure = (NodeFigure) node.getView();
					for (Iterator iterator = node.getMergedRevisions().iterator(); iterator
							.hasNext();) {
						Node merged = (Node) iterator.next();
						NodeFigure mergedView = (NodeFigure) merged.getView();
						if(mergedView != null)
							makeConnection(contents, mergedView, nodeFigure, ColorConstants.red);
					}
				}
			}
		}

		// end layouts
		for (Iterator iter = paths.iterator(); iter.hasNext(); i++) {
			String path = (String) iter.next();
			Branch branch = graph.getBranch(path);
			if(branch.getView() == null) {
				continue;
			}
			for (Iterator it = branch.getNodes().iterator(); it.hasNext();) {
				Node node = (Node) it.next();
				NodeFigure nodeFigure = (NodeFigure) node.getView();
				nodeFigure.endLayout();
			}

		}
		
		Branch selected = graph.getBranch(graph.getSelectedPath());
		if (selected != null) {
			Node n = selected.getSource(graph.getSelectedRevision());
			if(n.getView() != null) {
				NodeFigure nodeFigure = (NodeFigure) n.getView();
				selectNode(nodeFigure);
				// FIXME: it doesn't work
//				scrollTo((Rectangle) contentsLayout.getConstraint(nodeFigure)); 
			}
		}

		return contents;
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
	
	public void setConnectionVisibility(NodeFigure figure) {
		if (figure == null) return;
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
		getViewer().setSelection(new IStructuredSelection() {

			public Object getFirstElement() {
				return GraphEditPart.this;
			}

			public Iterator iterator() {
				return toList().iterator();
			}

			public int size() {
				return toArray().length;
			}

			public Object[] toArray() {
				Object[] selectedObjects = { GraphEditPart.this };
				return selectedObjects;
			}

			public List toList() {
				List list = new ArrayList();
				list.add(GraphEditPart.this);
				return list;
			}

			public boolean isEmpty() {
				return false;
			}
			
		});
	}
	
	public NodeFigure getSelectedNode() {
		return selected;
	}
	
	public Object getModel() {
		return graph;
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

	protected void createEditPolicies() {
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