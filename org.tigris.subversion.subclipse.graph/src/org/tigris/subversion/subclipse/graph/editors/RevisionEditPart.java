package org.tigris.subversion.subclipse.graph.editors;

import java.util.Iterator;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.tigris.subversion.subclipse.graph.cache.Node;

public class RevisionEditPart extends AbstractGraphicalEditPart {
	private final static int NODE_WIDTH = 50;
	private final static int NODE_HEIGHT = 30;
	private final static int NODE_OFFSET_Y = NODE_HEIGHT + 10;
	private final static int NODE_OFFSET_X = (BranchEditPart.BRANCH_WIDTH - NODE_WIDTH) / 2;

	public RevisionEditPart() {
		super();
	}

	protected IFigure createFigure() {
		Figure f = new Figure();
		f.setBackgroundColor(ColorConstants.white);
		f.setOpaque(true);
		BorderLayout layout = new BorderLayout();
		f.setLayoutManager(layout);
		
		Node node = (Node)getModel();
		GraphEditPart2 graphEditPart = (GraphEditPart2)getParent().getParent();
		NodeFigure nodeFigure = graphEditPart.getNodeFigure(node);
		
		node.setView(nodeFigure);
		
		if (node.getTags() != null) {
			Iterator iter = node.getTags().iterator();
			while (iter.hasNext()) {
				Node tag = (Node)iter.next();
				nodeFigure.addTag(tag);
			}
		}
		
		nodeFigure.endLayout();
		
		f.add(nodeFigure, BorderLayout.CENTER);
		
		Rectangle rect = new Rectangle(NODE_OFFSET_X, 10+BranchEditPart.BRANCH_HEIGHT+node.getIndex()*NODE_OFFSET_Y, NODE_WIDTH, NODE_HEIGHT);
		((AbstractGraphicalEditPart)getParent()).getFigure().getLayoutManager().setConstraint(f, rect);

		
		return f;
	}

	public boolean isSelectable() {
//		System.out.println("RevisionEditPart.isSelectable");
		return true;
	}

	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionEditPolicy() {
			protected void hideSelection() {
//				System.out.println("hide revision");
			}

			protected void showSelection() {
//				System.out.println("show revision");
			}			
		});
	}

}
