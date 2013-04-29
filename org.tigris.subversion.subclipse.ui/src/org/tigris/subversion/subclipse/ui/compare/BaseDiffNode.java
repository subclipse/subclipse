package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;

public class BaseDiffNode extends DiffNode {

	public BaseDiffNode(int kind) {
		super(kind);
	}

	public BaseDiffNode(ITypedElement left, ITypedElement right) {
		super(left, right);
	}

	public BaseDiffNode(IDiffContainer parent, int kind) {
		super(parent, kind);
	}

	public BaseDiffNode(int kind, ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		super(kind, ancestor, left, right);
	}

	public BaseDiffNode(IDiffContainer parent, int kind, ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		super(parent, kind, ancestor, left, right);
	}

	// Need to make this public
	@Override
	public void fireChange() {
		super.fireChange();
	}

}
