/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.GapTextStore;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

/**
 * the console model 
 */
public class ConsoleDocument extends AbstractDocument {
	public static final int COMMAND = 0; // command text
	public static final int MESSAGE = 1; // message received
	public static final int ERROR = 2;   // error received
	public static final int STATUS = 3;  // status text
	public static final int DELIMITER = 4; // delimiter text between runs

	private int[] lineTypes = null;
	private int   currentLine = 0;

	/**
	 * Creates an empty console document.
	 */
	public ConsoleDocument() {
		setTextStore(new GapTextStore(512, 1024));
		setLineTracker(new DefaultLineTracker());
		completeInitialization();
	}
	
	/**
	 * Clears the console document.
	 */
	public void clear() {
		lineTypes = null;
		currentLine = 0;
		set(""); //$NON-NLS-1$
	}
	
	/**
	 * Gets the line type for the line containing the specified offset.
	 */
	public int getLineType(int offset) {
		try {
			int line = getLineOfOffset(offset);
			if (line < currentLine) return lineTypes[line];
		} catch (BadLocationException e) {
			SVNProviderPlugin.log(SVNException.wrapException(e));
		}
		return 0;
	}
	
	/**
	 * Appends a line of the specified type to the end of the console.
	 */
	public void appendConsoleLine(int type, String line) {
		if (lineTypes == null) {
			lineTypes = new int[16];
		} else if (currentLine >= lineTypes.length) {
			int[] oldLineTypes = lineTypes;
			lineTypes = new int[oldLineTypes.length * 2];
			System.arraycopy(oldLineTypes, 0, lineTypes, 0, oldLineTypes.length);
		}
		lineTypes[currentLine++] = type;
		try { 
			replace(getLength(), 0, line + "\n"); //$NON-NLS-1$
		} catch (BadLocationException e) {
			SVNProviderPlugin.log(SVNException.wrapException(e));
		}
	}
	
	/**
	 * Return the indicies of the lines that contain command strings
	 */
	private int[] getCommandLines() {
		List commandLineList = new ArrayList();
		for (int i = 0; i < currentLine; i++) {
			if (lineTypes[i] == COMMAND) {
				commandLineList.add(new Integer(i));
			}			
		}
		int[] commandLines = new int[commandLineList.size()];
		int i = 0;
		for (Iterator iter = commandLineList.iterator(); iter.hasNext(); ) {
			commandLines[i++] = ((Integer) iter.next()).intValue();
		}
		return commandLines;
	}	
}
