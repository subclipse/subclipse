package org.tigris.subversion.subclipse.graph.editors;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.swt.graphics.Color;
import org.tigris.subversion.subclipse.graph.Activator;

public class BranchFigure extends RoundedRectangle {

	private String path;
	
	public BranchFigure(String path, Color bgcolor, Color fgcolor) {
		this.path = path;
		
		setLayoutManager(new BorderLayout());
		setBackgroundColor(bgcolor);
		setForegroundColor(fgcolor);
		setOpaque(true);

		Label label = new Label(path);
		label.setForegroundColor(Activator.FONT_COLOR);
		add(label, BorderLayout.CENTER);
		
		Label tooltip = new Label(path);
		setToolTip(tooltip);
	}
	
	public String getPath() {
		return path;
	}
	
}
