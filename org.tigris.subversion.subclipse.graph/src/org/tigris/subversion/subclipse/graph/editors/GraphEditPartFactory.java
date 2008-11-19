package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.tigris.subversion.subclipse.graph.cache.Branch;
import org.tigris.subversion.subclipse.graph.cache.Graph;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.cache.Path;

public class GraphEditPartFactory implements EditPartFactory {
	
	private ScrollingGraphicalViewer viewer;
	
	public GraphEditPartFactory(ScrollingGraphicalViewer viewer) {
		this.viewer = viewer;
	}

	public EditPart createEditPart(EditPart context, Object model) {
		EditPart editPart = null;
		if (model instanceof String) {
			final String s = (String) model;
			return new AbstractGraphicalEditPart() {
				protected IFigure createFigure() {
					return new Label(s);
				}

				protected void createEditPolicies() {
				}
			};
		} else if (model instanceof Graph) {
			editPart = new GraphEditPart2((Graph) model, viewer);
		} else if (model instanceof Branch) {
			editPart = new BranchEditPart();
		} else if (model instanceof Path) {
			editPart = new PathEditPart();
		} else if (model instanceof Node) {
			editPart = new RevisionEditPart();
		}
		if (editPart == null)
			throw new RuntimeException("cannot create EditPart for "+model.getClass().getName()+" class");
		else
			editPart.setModel(model);
		return editPart;
	}

}
