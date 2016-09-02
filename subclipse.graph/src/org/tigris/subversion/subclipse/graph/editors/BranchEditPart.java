package org.tigris.subversion.subclipse.graph.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.tigris.subversion.subclipse.graph.cache.Branch;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.cache.Path;

public class BranchEditPart extends AbstractGraphicalEditPart {
	public final static int BRANCH_WIDTH = 220;
	public final static int BRANCH_HEIGHT = 30;
	public final static int BRANCH_OFFSET = BRANCH_WIDTH+20;

	public BranchEditPart() {
		super();
	}

	protected IFigure createFigure() {
		Figure f = new Figure();
		f.setBackgroundColor(ColorConstants.white);
		f.setOpaque(true);

		XYLayout layout = new XYLayout();
		f.setLayoutManager(layout);
		
		Branch branch = (Branch)getModel();		
		Rectangle rect = new Rectangle(10+branch.getIndex()*BRANCH_OFFSET, 10, BRANCH_WIDTH, -1);
		((AbstractGraphicalEditPart)getParent()).getFigure().getLayoutManager().setConstraint(f, rect);
		
		return f;
	}
	
	protected List getModelChildren() {
		Branch branch = (Branch)getModel();
		List children = new ArrayList();
		Path path = new Path(branch.getPath());
		path.setIndex(branch.getIndex());
		children.add(path);
		
		List nodes = branch.getNodes();
		Iterator iter = nodes.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Node node = (Node)iter.next();
			node.setBranch(branch);
			
			node.setBranchIndex(i++);
			
			children.add(node);
		}
		
		return children;
	}

	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionEditPolicy() {
			protected void hideSelection() {
//				System.out.println("hide branch");
			}

			protected void showSelection() {
//				System.out.println("show branch");
			}			
		});
	}
	
//	public boolean isSelectable() {
//		System.out.println("BranchEditPart.isSelectable");
//		return true;
//	}

}
