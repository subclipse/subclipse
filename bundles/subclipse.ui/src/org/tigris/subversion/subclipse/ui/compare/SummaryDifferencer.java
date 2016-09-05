package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.ui.Policy;

public class SummaryDifferencer extends Differencer {

	public SummaryDifferencer() {
		super();
	}
	
	protected boolean contentsEqual(Object input1, Object input2) {
		return false;
	}
	
    protected void updateProgress(IProgressMonitor progressMonitor, Object node) {
        if (node instanceof ITypedElement) {
            ITypedElement element = (ITypedElement)node;
            progressMonitor.subTask(Policy.bind("CompareEditorInput.fileProgress", new String[] {element.getName()})); //$NON-NLS-1$
            progressMonitor.worked(1);
        }
    }

}
