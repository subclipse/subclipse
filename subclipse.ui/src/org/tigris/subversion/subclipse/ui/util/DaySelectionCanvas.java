/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Special canvas used to display the days of a month on. Every column represents
 * the day of a week from monday till sunday.
 */
public class DaySelectionCanvas extends Canvas {

	private final static Color BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private final static Color GRAY = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
	private final static Color SELECTION_FOREGROUND = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	private final static Color WHITE = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	
	private Color selectionBackgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

	private final int WIDTH = 210;
	private final int HEIGHT = 140;
	
	private int[] days;
	private String[] header;
	private int selection;
	
	/**
	 * Constructs a DaySelectionCanvas for displaying days of a month.
	 * @param parent parent widget
	 * @param style style of the canvas
	 */
	public DaySelectionCanvas(Composite parent, int style) {
		super(parent, style);
		selection = -1;
		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		addPaintListener(new DaySelectPaintListener());
		addMouseListener(new DaySelectMouseListener());
		addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				e.doit = true;
			}
		});
		// Enable focusing on the canvas
		addKeyListener(new KeyAdapter() {});
		addFocusListener(new FocusListener() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
// We do not need to (or want to!) dispose system colors that we did not create.
//				selectionBackgroundColor.dispose();
				selectionBackgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
				redraw();
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusLost(FocusEvent e) {
// We do not need to (or want to!) dispose system colors that we did not create.				
//				selectionBackgroundColor.dispose();
				selectionBackgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
				redraw();
			}
		});
	}
	
	/**
	 * Set the days to be displayed. The first days are expected to be from the
	 * previous month and grayed out. The days of this month are black and the tailing
	 * days of the next month are grayed out. The canvas is automatically redrawn.
	 * @param days array with days represented as ints
	 * @return if the array is valid
	 */
	public boolean setDays(int[] days) {
		if ((days == null) || (days.length < 1)) {
			days = null;
			return false;
		}
		int i = 0;
		while ((i < days.length) && (days[i] > 1)) {
			i++;
		}
		if ((i >= days.length) || (days[i] > 1)) {
			days = null;
			return false;
		}
		this.days = days;
		this.selection = -1;
		redraw();
		return true;
	}
	
	/**
	 * Sets the header text containing the days of the week.
	 * @param header array of strings to be displayed as header
	 */
	public void setHeader(String[] header) {
		this.header = header;
		redraw();
	}
	
	/**
	 * @return the selected day of the month
	 */
	public int getSelectedDay() {
		if (isValidSelection(selection)) {
			return days[selection];
		}
		return -1;
	}
	
	/**
	 * Set the selection to the specified day.
	 * @param day day to be selected
	 */
	public void setSelectedDay(int day) {
		if (days == null) {
			selection = -1;
			return;
		}
		
		for (int i=0; i<days.length; i++) {
			if (days[i] == 1) {
				selection = i + day - 1;
				break;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
	 */
	public Point computeSize(int w, int h, boolean b) {
		int borderWidth = getBorderWidth();
		return new Point(WIDTH + 2*borderWidth, HEIGHT + 2*borderWidth);
	}

// We do not need to (or want to!) dispose system colors that we did not create.	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
//	public void dispose() {
//		BLACK.dispose();
//		GRAY.dispose();
//		selectionBackgroundColor.dispose();
//		SELECTION_FOREGROUND.dispose();
//		WHITE.dispose();
//		super.dispose();
//	}
	
	/**
	 * Set the selection that results from the user clicking a day.
	 * @param x x-coordinate of the clicked point
	 * @param y y-coordinate of the clicked point
	 * @param width width of clicked component
	 * @param height height of the clicked component
	 */
	private void setSelection(Point point, Point size) {
		int row = point.y / getRowHeight(size.y) - 1;
		int column = point.x / getColumnWidth(size.x);
		selection = row * 7 + column;
		redraw();
	}
	
	/**
	 * Checks if the selected item is selectable.
	 * @param day day to check for
	 * @return <code>true</code> iff the selected day is enabled
	 * 		   for selection, false otherwise
	 */
	private boolean isValidSelection(int day) {
		if ((days == null) || (day > days.length)) {
			return false;
		}
   		int i=0;
   		while ((i < days.length) && (days[i] > 1)) {
   			if (i == day)
   				return false;
   			i++;
   		}
   		if (i == day) {
   			return true;
   		}
		i++;
   		for (; (i < days.length) && (days[i] > 1); i++) {
   			if (i == day)
   				return true;
   		}
		return false;
	}
	
	/**
	 * Computes the width of a column.
	 * @param width width available for columns
	 * @return width of one column
	 */
	private int getColumnWidth(int width) {
		return width/7;
	}
	
	/**
	 * Computes the height of a row.
	 * @param height height available for rows
	 * @return height of one row
	 */
	private int getRowHeight(int height) {
		return height/7;
	}
	
	/**
	 * Listener that is used for painting the header and days on the canvas.  
	 */
	private class DaySelectPaintListener implements PaintListener {
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
		 */
		public void paintControl(PaintEvent e) {
		    Point controlSize = ((Control) e.getSource()).getSize();
		    int height = controlSize.y;
		   	int rowHeight = getRowHeight(height);
		    int width = controlSize.x;
	   		int columnWidth = getColumnWidth(width);
		    
		    GC gc = e.gc; // gets the SWT graphics context from the event
		    
		    gc.setBackground(WHITE);
		    gc.fillRectangle(0, 0, width, height);
		    
	    	// Draw header
	    	gc.setForeground(BLACK);
	    	gc.setLineWidth(2);
	    	gc.drawLine(0, getRowHeight(height), width, getRowHeight(height));
		    if (header != null) {
		    	for (int i=0; i<header.length; i++) {
		    		Point position = getHeaderPosition(gc, i, header, columnWidth, rowHeight);
		    		gc.drawText(header[i], position.x, position.y);
		    	}
		    }
		    
		    // Draw days area
	   		gc.setForeground(GRAY);
	   		gc.setLineStyle(SWT.LINE_DOT);
	   		gc.setLineWidth(1);
		   	for (int i=1; i<7; i++) {
		   		gc.drawLine(i*columnWidth, getRowHeight(height) + 1, i*columnWidth, height);
		   	}
		   	for (int i=2; i<7; i++) {
		   		gc.drawLine(0, i*rowHeight, width, i*rowHeight);
		   	}
		   	
		   	// Draw days
		   	if ((days != null) && days.length > 0) {
		   		gc.setForeground(GRAY);
		   		int i=0;
		   		while ((i < days.length) && (days[i] > 1)) {
		   			drawDay(gc, i, days, columnWidth, rowHeight);
		   			i++;
		   		}
		   		gc.setForeground(BLACK);
	   			drawDay(gc, i, days, columnWidth, rowHeight);
	   			i++;
		   		for (; (i < days.length) && (days[i] > 1); i++) {
		   			drawDay(gc, i, days, columnWidth, rowHeight);
		   		}
		   		gc.setForeground(GRAY);
		   		for (; i < days.length; i++) {
		   			drawDay(gc, i, days, columnWidth, rowHeight);
		   		}
		   	}
		   	
		   	gc.dispose();
		}
		
		/**
		 * Draws a day on the canvas.
		 * @param gc GC used to draw
		 * @param days item to be drawn
		 * @param days array with days
		 * @param columnWidth width of a column
		 * @param rowHeight height of a row
		 */
		private void drawDay(GC gc, int item, int[] days, int columnWidth, int rowHeight) {
			Color tempBackground = null;
			Color tempForeground = null;
			boolean validSelection = isValidSelection(item);
			if (validSelection && (item == selection)) {
				tempBackground = gc.getBackground();
				tempForeground = gc.getForeground();
				gc.setBackground(selectionBackgroundColor);
				gc.setForeground(SELECTION_FOREGROUND);
				int height = rowHeight;
				int width = columnWidth;
				int x = columnWidth * (item % 7);
				if (x > 0) {
					x++;
					width--;
				}
				int y = rowHeight * (item / 7 + 1) + 1;
				height--;
				if (y == rowHeight) {
					y++;
					height--;
				}
				gc.fillRectangle(x, y, width, height);
				gc.setBackground(tempBackground);
			}
   			String dayString = String.valueOf(days[item]);
   			Point position = getDayPosition(gc, item, days, columnWidth, rowHeight);
   			gc.drawText(dayString, position.x, position.y, SWT.DRAW_TRANSPARENT);
   			if (validSelection && (item == selection)) {
				gc.setForeground(tempForeground);
   			}
		}
		
		/**
		 * Compute the point where to draw a header string.
		 * @param gc GC used to draw
		 * @param item header item to be drawn
		 * @param header array with header strings
		 * @param columnWidth width of a column
		 * @param rowHeight height of a row
		 * @return the point where the header string has to be drawn
		 */
		private Point getHeaderPosition(GC gc, int item, String[] header, int columnWidth, int rowHeight) {
			int column = item % 7;
   			Point size = gc.textExtent(String.valueOf(header[item]), SWT.DRAW_TRANSPARENT);
   			
   			int x = column * columnWidth + columnWidth/2 - size.x/2; 
   			int y = rowHeight/2 - size.y/2; 
   			return new Point(x, y);   			
		}
		
		/**
		 * Compute the point where to draw a day.
		 * @param gc GC used to draw
		 * @param item days item to be drawn
		 * @param days array with days
		 * @param columnWidth width of a column
		 * @param rowHeight height of a row
		 * @return the point where the day string has to be drawn
		 */
		private Point getDayPosition(GC gc, int item, int[] days, int columnWidth, int rowHeight) {
			int row = item / 7 + 1;
			int column = item % 7;
   			Point size = gc.textExtent(String.valueOf(days[item]), SWT.DRAW_TRANSPARENT);
   			
   			int x = column * columnWidth + columnWidth/2 - size.x/2; 
   			int y = row * rowHeight + rowHeight/2 - size.y/2; 
   			return new Point(x, y);   			
		}
		
	}
	
	/**
	 * MouseListener to set a day selection.
	 */
	private class DaySelectMouseListener extends MouseAdapter {
		
		public void mouseDown(MouseEvent e) {
			Point size = ((Control)e.getSource()).getSize();
			setSelection(new Point(e.x, e.y), size);
		}
		
	}
	
}
