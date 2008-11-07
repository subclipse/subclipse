package org.tigris.subversion.subclipse.graph.editors;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.sublicpse.graph.cache.Node;

public class NodeTooltipFigure extends Figure {

	private static final int BORDER_WIDTH = 5;
	private static final int BORDER_WIDTH2 = BORDER_WIDTH*2;
	
	private static DateFormat dateFormat;

	private boolean hasSources = false;
	private boolean hasTags = false;
	private int tagsAdded;
	private int tagCount;
	private int messageLines;
	
	private final static int NUMBER_OF_LOG_MESSAGE_LINES = 15;
	private final static int NUMBER_OF_TAG_AND_MESSAGE_LINES = 25;
	
	public NodeTooltipFigure(Node node) {
		ToolbarLayout layout = new ToolbarLayout();
		layout.setStretchMinorAxis(false);
		setLayoutManager(layout);	
		setBackgroundColor(Activator.BGCOLOR);
		setOpaque(true);
		layout.setSpacing(5);
		
		// lazy loading and reuse
		if(dateFormat == null) {
			dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		}

		Figure tooltip = new Figure();
		setToolTip(tooltip);

		add(createLabel("Action and path", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
		add(createLabel(node.getAction()+" "+node.getPath(), JFaceResources.getTextFont()));
		add(createLabel("Author", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
		add(createLabel(node.getAuthor(), JFaceResources.getTextFont()));
		add(createLabel("Date", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
		add(createLabel(dateFormat.format(node.getRevisionDate()), JFaceResources.getTextFont()));
		add(createLabel("Message", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
		add(createLabel(getFirstLines(node.getMessage(), NUMBER_OF_LOG_MESSAGE_LINES), JFaceResources.getTextFont()));
		if(node.getCopySrcPath() != null) {
			add(createLabel("From", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
			add(createLabel(format(node.getCopySrcRevision(), node.getCopySrcPath()), JFaceResources.getTextFont()));
		}
	}
	
	public void endLayout() {
		if (tagCount > tagsAdded) add(createLabel((tagCount - tagsAdded) + " more...", JFaceResources.getTextFont()));
		
		Dimension d = getPreferredSize();
		
		setPreferredSize(d.width+BORDER_WIDTH2, d.height+BORDER_WIDTH2);
		setBorder(new LineBorder(Activator.BGCOLOR, BORDER_WIDTH));
	}
	
	public void addSource(Node node) {
		if(!hasSources) {
			add(createLabel("Source of", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
			hasSources = true;
		}
		add(createLabel(format(node.getRevision(), node.getPath()), JFaceResources.getTextFont()));
	}
	
	public void addTag(Node node) {
		if(!hasTags) {
			add(createLabel("Tagged as", JFaceResources.getHeaderFont(), Activator.FONT_COLOR));
			hasTags = true;
		}
		if (messageLines + tagCount < NUMBER_OF_TAG_AND_MESSAGE_LINES) {
			add(createLabel(format(node.getRevision(), node.getPath()), JFaceResources.getTextFont()));
			tagsAdded++;
		}
		tagCount++;
	}
	
	public String format(long revision, String path) {
		return "r"+Long.toString(revision)+" "+path;
	}
	
	public static Label createLabel(String text, Font font) {
		Label label = new Label(text);
		label.setFont(font);
		label.setTextAlignment(PositionConstants.LEFT);
		return label;
	}
	
	public static Label createLabel(String text, Font font, Color c) {
		Label label = new Label(text);
		label.setFont(font);
		label.setTextAlignment(PositionConstants.LEFT);
		label.setForegroundColor(c);
		return label;
	}

	public String getFirstLines(String string, int numberLines) {
		if (string == null) return null;
		StringTokenizer tokenizer = new StringTokenizer(string, "\r\n");
		int count = tokenizer.countTokens();
		if (count <= numberLines) {
			messageLines = count;
			return string;
		}
		messageLines = numberLines;
		StringBuffer newString = new StringBuffer();
		for (int i = 0; i < numberLines; i++) {
			newString.append(tokenizer.nextToken() + "\n");
		}
		newString.append((count - numberLines) + " more message lines . . .");
		return newString.toString();
	}
	
}
