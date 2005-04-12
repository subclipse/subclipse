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
package org.tigris.subversion.subclipse.ui.annotations;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * Model for a SVN Annotate block.
 */
public class AnnotateBlock {

	long revision = -1;
	String user = ""; //$NON-NLS-1$
	int startLine = 0;
	int endLine = 0;

	public AnnotateBlock(long revision, String user, int startLine, int endLine) {
		this.revision = revision;
		this.user = user;
		this.startLine = startLine;
		this.endLine = endLine;
	}
	
	
	/**
	 * @return int the last source line of the receiver
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * @param line
	 */
	public void setEndLine(int line) {
		endLine = line;
	}

	/**
	 * @return the revision the receiver occured in.
	 */
	public long getRevision() {
		return revision;
	}

	/**
	 * @return the first source line number of the receiver
	 */
	public int getStartLine() {
		return startLine;
	}

	/**
	 * Used by the default LabelProvider to display objects in a List View
	 */
	public String toString() {
		int delta = endLine - startLine + 1;
		String line = Policy.bind("SVNAnnotateBlock.lines"); //$NON-NLS-1$
		if (delta == 1) {
			line = Policy.bind("SVNAnnotateBlock.line"); //$NON-NLS-1$
		}
		return Policy.bind("SVNAnnotateBlock.userRevision", new Object[] { //$NON-NLS-1$
			user,
			new Long(revision),
			String.valueOf(delta),
			line
		});
	}

	/**
	 * Answer true if the receiver contains the given line number, false otherwse.
	 * @param i a line number
	 * @return true if receiver contains a line number.
	 */
	public boolean contains(int i) {
		return (i >= startLine && i <= endLine);
	}
}
