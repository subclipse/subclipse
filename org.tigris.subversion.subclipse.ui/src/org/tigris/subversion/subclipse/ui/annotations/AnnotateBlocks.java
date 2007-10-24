/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.annotations;

import java.util.LinkedList;
import java.util.List;

import org.tigris.subversion.svnclientadapter.ISVNAnnotations;

public class AnnotateBlocks {
	private List blocks = new LinkedList();
	
	public AnnotateBlocks(ISVNAnnotations svnAnnotations) {
		for (int i = 0; svnAnnotations.getLine(i) != null;i++) {
			AnnotateBlock block = new AnnotateBlock(
					svnAnnotations.getRevision(i),
					svnAnnotations.getAuthor(i),
					svnAnnotations.getChanged(i),
					i,i);
			add(block);
		}
	}

	/**
	 * Add an annotate block merging this block with the
	 * previous block if it is part of the same change.
	 * @param aBlock
	 */
	private void add(AnnotateBlock aBlock) {
		
		int size = blocks.size();
		if (size == 0) {
			blocks.add(aBlock);
		} else {
			AnnotateBlock lastBlock = (AnnotateBlock) blocks.get(size - 1);
			if (lastBlock.getRevision() == aBlock.getRevision()) {
				lastBlock.setEndLine(aBlock.getStartLine());
			} else {
				blocks.add(aBlock);
			}
		}
	}	

	public List getAnnotateBlocks() {
		return blocks;
	}	
	
}
