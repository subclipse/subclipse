package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.swt.graphics.Image;

public class MultipleSelectionNode implements IStructureComparator, ITypedElement {
	private Object[] children;

	public MultipleSelectionNode(Object[] children) {
		super();
		this.children = children;
	}

	public Object[] getChildren() {
		return children;
	}

	public Image getImage() {
		return null;
	}

	public String getName() {
		return "";
	}

	public String getType() {
		return UNKNOWN_TYPE;
	}

}
