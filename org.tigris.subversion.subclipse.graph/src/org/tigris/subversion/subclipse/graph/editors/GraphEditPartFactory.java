package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.tigris.subversion.subclipse.graph.cache.Graph;

public class GraphEditPartFactory implements EditPartFactory {
	
	private ScrollingGraphicalViewer viewer;
	
	public GraphEditPartFactory(ScrollingGraphicalViewer viewer) {
		this.viewer = viewer;
	}

	public EditPart createEditPart(EditPart editPart, Object node) {
		if (node instanceof String) {
			final String s = (String) node;
			return new AbstractGraphicalEditPart() {
				protected IFigure createFigure() {
					return new Label(s);
				}

				protected void createEditPolicies() {
				}
			};
		} else if (node instanceof Graph) {
			return new GraphEditPart((Graph) node, viewer);
		}
		throw new RuntimeException("cannot create EditPart for "+node.getClass().getName()+" class");
	}

}
