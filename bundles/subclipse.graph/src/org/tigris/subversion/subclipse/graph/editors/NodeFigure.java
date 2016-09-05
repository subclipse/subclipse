package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.cache.Node;

public class NodeFigure extends RoundedRectangle {

	private Node node;
	private PolylineConnection source;
	private int sourceIndex;
	private boolean hasTags;
	private Color bgcolor;
	private boolean selected;
	
	public NodeFigure(Node node, Color bgcolor, Color fgcolor) {
		this.node = node;
		this.bgcolor = bgcolor;
		setLayoutManager(new BorderLayout());
		setBackgroundColor(bgcolor);
		setForegroundColor(fgcolor);
		setOpaque(true);

		setToolTip(new NodeTooltipFigure(node));
		setCursor(Cursors.HAND);
	}
	
	public PolylineConnection getSource() {
		return source;
	}

	public void setSource(PolylineConnection source, int sourceIndex) {
		this.source = source;
		this.sourceIndex = sourceIndex;
	}
	
	public int getSourceIndex() {
		return sourceIndex;
	}

	public Node getNode() {
		return node;
	}
	
	public void addTag(Node source) {
		NodeTooltipFigure tt = (NodeTooltipFigure) getToolTip();
		tt.addTag(source);
		this.hasTags = true;
		setForegroundColor(ColorConstants.black);
	}
	
	public void endLayout() {
		if(hasTags)
			add(createLabel(Long.toString(node.getRevision()) + "*", JFaceResources.getDefaultFont()), BorderLayout.CENTER);
		else
			add(createLabel(Long.toString(node.getRevision()), JFaceResources.getDefaultFont()), BorderLayout.CENTER);
		NodeTooltipFigure tt = (NodeTooltipFigure) getToolTip();
		tt.endLayout();
	}
	
	public static Label createLabel(String text, Font font) {
		Label label = new Label(text);
		label.setFont(font);
		label.setForegroundColor(Activator.FONT_COLOR);
		return label;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
		if(selected)
			setBackgroundColor(ColorConstants.white);
		else
			setBackgroundColor(bgcolor);
	}
	
	public boolean isSelected() {
		return selected;
	}
	
}
