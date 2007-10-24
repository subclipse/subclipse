package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.ui.Policy;

public class SummaryDifferencer extends Differencer {
//	private SVNDiffSummary[] diffSummary;

//	public SummaryDifferencer(SVNDiffSummary[] diffSummary) {
	public SummaryDifferencer() {
		super();
//		this.diffSummary = diffSummary;	
	}
	
	protected boolean contentsEqual(Object input1, Object input2) {
		return false;
//		SummaryEditionNode node = (SummaryEditionNode)input1;
//		for (int i = 0; i < diffSummary.length; i++) {
//			if (node.getRemoteResource().getRepositoryRelativePath().endsWith("/" + diffSummary[i].getPath())) //$NON-NLS-1$
//				return false;
//		}
//		return true;
	}
	
    protected void updateProgress(IProgressMonitor progressMonitor, Object node) {
        if (node instanceof ITypedElement) {
            ITypedElement element = (ITypedElement)node;
            progressMonitor.subTask(Policy.bind("CompareEditorInput.fileProgress", new String[] {element.getName()})); //$NON-NLS-1$
            progressMonitor.worked(1);
        }
    }

}
