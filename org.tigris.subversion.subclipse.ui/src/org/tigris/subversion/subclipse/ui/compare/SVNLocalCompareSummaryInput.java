package org.tigris.subversion.subclipse.ui.compare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.StatusAndInfoCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.internal.Utilities;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;

public class SVNLocalCompareSummaryInput extends SVNAbstractCompareEditorInput implements ISaveableWorkbenchPart {
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
				SVNDiffSummary[] diffSummary = null;
				if (remoteFolder.getRevision().equals(SVNRevision.HEAD) && remoteFolder.getUrl().equals(resource.getUrl())) {
			        StatusAndInfoCommand cmd = new StatusAndInfoCommand(SVNWorkspaceRoot.getSVNResourceFor( resource.getResource() ), true, false, true );
			        cmd.run(monitor);
			        RemoteResourceStatus[] statuses = cmd.getRemoteResourceStatuses();
			        diffSummary = getDiffSymmary(statuses);
				} else {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
					diffSummary = client.diffSummarize(new File(resource.getResource().getLocation().toString()), remoteFolder.getUrl(), remoteFolder.getRevision(), true);
				}
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
			return e.getMessage();
		} finally {
			monitor.done();
		}
	}
	
	private SVNDiffSummary[] getDiffSymmary(RemoteResourceStatus[] statuses) {
		List diffSummaryList = new ArrayList();
		int rootPathLength = resource.getResource().getLocation().toString().length() + 1;
		for (int i = 0; i < statuses.length; i++) {
			if (statuses[i].getFile() != null && !statuses[i].getNodeKind().equals(SVNNodeKind.DIR)) {
				SVNStatusKind textStatus = statuses[i].getTextStatus();
				boolean propertyChanges = !statuses[i].getPropStatus().equals(SVNStatusKind.NORMAL) && !statuses[i].getPropStatus().equals(SVNStatusKind.NONE);
				boolean localChanges = false;
				if (textStatus.equals(SVNStatusKind.NONE) && propertyChanges && statuses[i].getNodeKind().equals(SVNNodeKind.FILE)) {
					IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(statuses[i].getPath()));
					if (file != null) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(file);
						try {
							LocalResourceStatus localStatus = svnResource.getStatus();
							if (localStatus != null) {
								localChanges = localStatus.isAdded() || localStatus.isDirty();
							}
						} catch (SVNException e) {}
					}
				}
				if (!textStatus.equals(SVNStatusKind.NONE) || !propertyChanges || localChanges) {
					SVNDiffKind diffKind = null;
					if (statuses[i].getTextStatus().equals(SVNStatusKind.ADDED)) diffKind = SVNDiffKind.ADDED;
					else if (statuses[i].getTextStatus().equals(SVNStatusKind.DELETED)) diffKind = SVNDiffKind.DELETED;
					else diffKind = SVNDiffKind.MODIFIED;
					SVNDiffSummary diffSummary = new SVNDiffSummary(statuses[i].getPath().substring(rootPathLength).replaceAll("\\\\", "/"), diffKind, propertyChanges, statuses[i].getNodeKind().toInt());
					diffSummaryList.add(diffSummary);
				}
			}
		}
		SVNDiffSummary[] diffSummaries = new SVNDiffSummary[diffSummaryList.size()];
		diffSummaryList.toArray(diffSummaries);
		return diffSummaries;
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

	public boolean canRunAsJob() {
		return true;
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
