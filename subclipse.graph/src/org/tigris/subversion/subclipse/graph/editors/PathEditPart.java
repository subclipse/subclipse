package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.tigris.subversion.subclipse.graph.cache.Path;

public class PathEditPart extends AbstractGraphicalEditPart {
	private BranchFigure branchFigure;

	public PathEditPart() {
		super();
	}

	protected IFigure createFigure() {
		Figure f = new Figure();
		f.setBackgroundColor(ColorConstants.white);
		f.setOpaque(true);
		BorderLayout layout = new BorderLayout();
		f.setLayoutManager(layout);
		
		Path path = (Path)getModel();
		GraphEditPart graphEditPart = (GraphEditPart)getParent().getParent();
		branchFigure = graphEditPart.getBranchFigure(path.getPath());
		
		f.add(branchFigure, BorderLayout.CENTER);
		
		return f;
	}



	protected void refreshVisuals() {
		getFigure().setSize(220, 30);
		
		super.refreshVisuals();
	}

	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionEditPolicy() {
			protected void hideSelection() {
//				System.out.println("hide path");
			}

			protected void showSelection() {
//				System.out.println("show path");
			}			
		});
	}

}
