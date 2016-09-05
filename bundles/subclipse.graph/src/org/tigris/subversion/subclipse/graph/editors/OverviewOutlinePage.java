package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class OverviewOutlinePage extends Page implements IContentOutlinePage {
	
	private Canvas overview;
	private ScalableRootEditPart rootEditPart;
	private Thumbnail thumbnail;

	public OverviewOutlinePage(ScalableRootEditPart rootEditPart) {
		super();
		this.rootEditPart = rootEditPart;
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		
	}

	public void createControl(Composite parent) {
		// create canvas and lws
		overview = new Canvas(parent, SWT.NONE);
		LightweightSystem lws = new LightweightSystem(overview);
		// create thumbnail
		thumbnail =
			new ScrollableThumbnail((Viewport) rootEditPart.getFigure());
		thumbnail.setBorder(new MarginBorder(3));
		thumbnail.setSource(
				rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS));
		lws.setContents(thumbnail);
	}

	public void dispose() {
		if (null != thumbnail)
			thumbnail.deactivate();
		super.dispose();
	}

	public Control getControl() {
		return overview;
	}

	public ISelection getSelection() {
		return StructuredSelection.EMPTY;
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		
	}

	public void setFocus() {
		if (getControl() != null)
			getControl().setFocus();
	}

	public void setSelection(ISelection selection) {
		
	}
}