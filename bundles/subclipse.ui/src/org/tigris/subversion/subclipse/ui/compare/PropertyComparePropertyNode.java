package org.tigris.subversion.subclipse.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class PropertyComparePropertyNode implements IStructureComparator, ITypedElement, IStreamContentAccessor {
	private ISVNProperty property;
	
	public PropertyComparePropertyNode(ISVNProperty property) {
		this.property = property;
	}

	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(property.getValue().getBytes());
	}

	public String getName() {
		return property.getName();
	}

	public Image getImage() {
		return SVNUIPlugin.getImage(ISVNUIConstants.IMG_PROPERTIES);
	}

	public String getType() {
		return "txt"; //$NON-NLS-1$
	}

	public Object[] getChildren() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ITypedElement) {
			ITypedElement compareTo = (ITypedElement)obj;
			return getName().equals(compareTo.getName());
		}
		return super.equals(obj);
	}
	
	public int hashCode() {
		return getName().hashCode();
	}

}
