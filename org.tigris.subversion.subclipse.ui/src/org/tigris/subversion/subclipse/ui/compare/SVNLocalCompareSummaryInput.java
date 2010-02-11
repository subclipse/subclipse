package org.tigris.subversion.subclipse.ui.compare;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.internal.Utilities;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;

public class SVNLocalCompareSummaryInput extends CompareEditorInput implements ISaveableWorkbenchPart {
	private ISVNLocalResource resource;
	private ISVNRemoteFolder remoteFolder;
	private boolean readOnly;
	
	public SVNLocalCompareSummaryInput(ISVNLocalResource resource, ISVNRemoteFolder remoteFolder) throws SVNException {
		super(new CompareConfiguration());
		this.resource = resource;
		this.remoteFolder = remoteFolder;
	}
	
	/**
	 * initialize the labels : the title, the lft label and the right one
	 */
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		String resourceName = resource.getName();	
		setTitle(Policy.bind("SVNCompareRevisionsInput.compareResourceAndVersions", new Object[] {resourceName})); //$NON-NLS-1$
		cc.setLeftEditable(! readOnly);
		cc.setRightEditable(false);
		
		String leftLabel = Policy.bind("SVNCompareRevisionsInput.workspace", new Object[] {resourceName}); //$NON-NLS-1$
		cc.setLeftLabel(leftLabel);
		String rightLabel = Policy.bind("SVNCompareRevisionsInput.repository", new Object[] {resourceName}); //$NON-NLS-1$
		cc.setRightLabel(rightLabel);
	}
	
	/**
	 * Runs the compare operation and returns the compare result.
	 */
	protected Object prepareInput(IProgressMonitor monitor) throws InterruptedException {

		initLabels();
		
		try {		
			monitor.beginTask(Policy.bind("SVNCompareEditorInput.comparing"), 30); //$NON-NLS-1$
			IProgressMonitor sub = new SubProgressMonitor(monitor, 30);
			sub.beginTask(Policy.bind("SVNCompareEditorInput.comparing"), 100); //$NON-NLS-1$
			Object[] result = new Object[] { null };
			try {
				ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
				SVNDiffSummary[] diffSummary = client.diffSummarize(new File(resource.getResource().getLocation().toString()), remoteFolder.getUrl(), remoteFolder.getRevision(), true);
				diffSummary = getDiffSummaryWithSubfolders(diffSummary);
				ITypedElement left = new SVNLocalResourceSummaryNode(resource, diffSummary, resource.getResource().getLocation().toString());
				SummaryEditionNode right = new SummaryEditionNode(remoteFolder);
				right.setRootFolder((RemoteFolder)remoteFolder);
				right.setNodeType(SummaryEditionNode.RIGHT);
				right.setRoot(true);		
				right.setDiffSummary(diffSummary);	
				String localCharset = Utilities.getCharset(resource.getIResource());
				try {
					right.setCharset(localCharset);
				} catch (CoreException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}		        
		        result[0] = new SummaryDifferencer().findDifferences(false, monitor, null, null, left, right);
				if (result[0] instanceof DiffNode) {
					IDiffElement[] diffs = ((DiffNode)result[0]).getChildren();
					if (diffs == null || diffs.length == 0) {
						result[0] = null;
					}
				}    
			} finally {
				sub.done();
			}
	        return result[0];
		} catch (OperationCanceledException e) {
			throw new InterruptedException(e.getMessage());
		} catch (Exception e) {
			handle(e);
			return null;
		} finally {
			monitor.done();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		try {
			saveChanges(monitor);
		} catch (CoreException e) {
			Utils.handle(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
		// noop
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		return isSaveNeeded();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#addPropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	public void addPropertyListener(IPropertyListener listener) {
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		createContents(parent);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getSite()
	 */
	public IWorkbenchPartSite getSite() {
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleToolTip()
	 */
	public String getTitleToolTip() {
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#removePropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	public void removePropertyListener(IPropertyListener listener) {
	}
	
	public void cancelPressed() { 
		super.cancelPressed();
	}

	public boolean canRunAsJob() {
		return true;
	}

	private void handle(Exception e) {
		// create a status
		Throwable t = e;
		// unwrap the invocation target exception
		if (t instanceof InvocationTargetException) {
			t = ((InvocationTargetException)t).getTargetException();
		}
		IStatus error;
		if (t instanceof CoreException) {
			error = ((CoreException)t).getStatus();
		} else if (t instanceof TeamException) {
			error = ((TeamException)t).getStatus();
		} else {
			error = new Status(IStatus.ERROR, SVNUIPlugin.ID, 1, Policy.bind("internal"), t); //$NON-NLS-1$
		}
		setMessage(error.getMessage());
		if (!(t instanceof TeamException)) {
			SVNUIPlugin.log(error);
		}
	}
	
	private SVNDiffSummary[] getDiffSummaryWithSubfolders(SVNDiffSummary[] diffSummary) {
		ArrayList paths = new ArrayList();
		ArrayList diffs = new ArrayList();
		for (int i = 0; i < diffSummary.length; i++) {
			paths.add(diffSummary[i].getPath());
			diffs.add(diffSummary[i]);
		}
		for (int i = 0; i < diffSummary.length; i++) {
			File file = new File(diffSummary[i].getPath());
			while (file.getParentFile() != null) {
				file = file.getParentFile();
				String path = file.getPath();
				path = path.replaceAll("\\\\", "/");
				if (!paths.contains(path)) {
					paths.add(path);
					SVNDiffSummary folder = new SVNDiffSummary(path, SVNDiffKind.NORMAL, false, SVNNodeKind.DIR.toInt());
					diffs.add(folder);
				}
			}
		}
		diffSummary = new SVNDiffSummary[diffs.size()];
		diffs.toArray(diffSummary);
		return diffSummary;
	}
    
}
