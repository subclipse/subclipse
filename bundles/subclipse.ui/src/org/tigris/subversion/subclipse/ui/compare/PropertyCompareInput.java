package org.tigris.subversion.subclipse.ui.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.ui.Policy;

public class PropertyCompareInput extends CompareEditorInput {
	private IPropertyProvider left;
	private IPropertyProvider right;
	private boolean recursive;

	public PropertyCompareInput(IPropertyProvider left, IPropertyProvider right, boolean recursive) {
		super(new CompareConfiguration());
		this.left = left;
		this.right = right;
		this.recursive = recursive;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		initLabels();
		if (monitor != null) {
			monitor.subTask(Policy.bind("PropertyCompareInput.0") + left.getLabel()); //$NON-NLS-1$
		}
		left.getProperties(recursive);
		if (monitor != null && monitor.isCanceled()) {
			return null;
		}
		if (monitor != null) {
			monitor.subTask(Policy.bind("PropertyCompareInput.0") + right.getLabel()); //$NON-NLS-1$
		}
		right.getProperties(recursive);
		if (monitor != null && monitor.isCanceled()) {
			return null;
		}
		return new Differencer().findDifferences(false, monitor,null,null,left,right);
	}
	
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		setTitle(Policy.bind("PropertyCompareInput.2")); //$NON-NLS-1$
		cc.setLeftEditable(left.isEditable());
		cc.setRightEditable(right.isEditable());
		cc.setLeftLabel(left.getLabel());
		cc.setRightLabel(right.getLabel());
	}

}
