package org.tigris.subversion.subclipse.ui.compare;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public class SVNFolderCompareEditorInput extends CompareEditorInput {
	private SummaryEditionNode left;
	private SummaryEditionNode right;
	private ISVNRemoteFolder folder1;
	private ISVNRemoteFolder folder2;
	private ITypedElement ancestor;	
	private Image leftImage;
	private Image rightImage;
	private Image ancestorImage;
	private ISVNResource localResource1;
	private ISVNResource localResource2;
	
	public SVNFolderCompareEditorInput(ISVNRemoteFolder folder1, ISVNRemoteFolder folder2) {
		super(new CompareConfiguration());
		this.folder1 = folder1;
		this.folder2 = folder2;
		left = new SummaryEditionNode(folder1);
		left.setRootFolder((RemoteFolder)folder1);
		left.setNodeType(SummaryEditionNode.LEFT);
		right = new SummaryEditionNode(folder2);
		right.setRootFolder((RemoteFolder)folder2);
		right.setNodeType(SummaryEditionNode.RIGHT);
	}

	private String getLabel(ITypedElement element) {
		if (element instanceof SummaryEditionNode) {
			ISVNRemoteResource edition = ((SummaryEditionNode)element).getRemoteResource();
			return Policy.bind("nameAndRevision", edition.getName(), edition.getRevision().toString()); //$NON-NLS-1$
		}
		return element.getName();
	}

	private String getVersionLabel(ITypedElement element) {
		if (element instanceof SummaryEditionNode) {
			ISVNRemoteResource edition = ((SummaryEditionNode)element).getRemoteResource();
			return edition.getRevision().toString();
		}
		return element.getName();
	}

	private String guessResourceName() {
		if (left != null) {
			return left.getName();
		}
		if (right != null) {
			return right.getName();
		}
		if (ancestor != null) {
			return ancestor.getName();
		}
		return ""; //$NON-NLS-1$
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

	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();

        ITypedElement left = this.left;
        ITypedElement right = this.right;
        ITypedElement ancestor = this.ancestor;
        
        if (left != null) {
            cc.setLeftLabel(getLabel(left));
            cc.setLeftImage(leftImage);
        }
    
        if (right != null) {
            cc.setRightLabel(getLabel(right));
            cc.setRightImage(rightImage);
        }
        
        if (ancestor != null) {
            cc.setAncestorLabel(getLabel(ancestor));
            cc.setAncestorImage(ancestorImage);
        }
		
		String title;
		if (ancestor != null) {
			title = Policy.bind("SVNCompareEditorInput.titleAncestor", new Object[] {guessResourceName(), getVersionLabel(ancestor), getVersionLabel(left), getVersionLabel(right)} ); //$NON-NLS-1$
		} else {
			String leftName = null;
			if (left != null) leftName = left.getName();
			String rightName = null;
			if (right != null) rightName = right.getName();
			
			if (leftName != null && !leftName.equals(rightName)) {
				title = Policy.bind("SVNCompareEditorInput.titleNoAncestorDifferent", new Object[] {leftName, getVersionLabel(left), rightName, getVersionLabel(right)} );  //$NON-NLS-1$
			} else {
				title = Policy.bind("SVNCompareEditorInput.titleNoAncestor", new Object[] {guessResourceName(), getVersionLabel(left), getVersionLabel(right)} ); //$NON-NLS-1$
			}
		}
		setTitle(title);
	}

	public boolean isSaveNeeded() {
		return false;
	}

	protected Object prepareInput(IProgressMonitor monitor) throws InterruptedException {
		final boolean threeWay = ancestor != null;
		if (right == null || left == null) {
			setMessage(Policy.bind("SVNCompareEditorInput.different")); //$NON-NLS-1$
			return null;
		}
		initLabels();
	
		try {	
			// do the diff	
			final Object[] result = new Object[] { null };
			monitor.beginTask(Policy.bind("SVNCompareEditorInput.comparing"), 30); //$NON-NLS-1$
			IProgressMonitor sub = new SubProgressMonitor(monitor, 30);
			sub.beginTask(Policy.bind("SVNCompareEditorInput.comparing"), 100); //$NON-NLS-1$
			try {
				ISVNClientAdapter svnClient = folder1.getRepository().getSVNClient();
				SVNDiffSummary[] diffSummary = null;
				if (folder1.getRepositoryRelativePath().equals(folder2.getRepositoryRelativePath()) && localResource1 != null) {
					IResource resource1 = localResource1.getResource();
					if (resource1 != null) {
						ISVNLocalResource svnResource1 = SVNWorkspaceRoot.getSVNResourceFor(resource1);
						if (svnResource1 != null) {
							SVNRevision pegRevision = svnResource1.getRevision();
							if (pegRevision != null) {
								diffSummary = svnClient.diffSummarize(folder1.getUrl(), pegRevision, folder1.getRevision(), folder2.getRevision(), Depth.infinity, false);
							}
						}
					} else {
						diffSummary = svnClient.diffSummarize(folder1.getUrl(), SVNRevision.HEAD, folder1.getRevision(), folder2.getRevision(), Depth.infinity, false);
					}
				}
				if (diffSummary == null) diffSummary = svnClient.diffSummarize(folder1.getUrl(), folder1.getRevision(), folder2.getUrl(), folder2.getRevision(), Depth.infinity, false);				
				diffSummary = getDiffSummaryWithSubfolders(diffSummary);
				left.setDiffSummary(diffSummary);
				right.setDiffSummary(diffSummary);
				left.setRoot(true);
				right.setRoot(true);
				result[0] = new SummaryDifferencer().findDifferences(threeWay, sub, null, ancestor, left, right);
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
					SVNDiffSummary folder = new SVNDiffSummary(path, SVNDiffKind.NORMAL, false, SVNNodeKind.DIR.toInt());
					diffs.add(folder);
				}
			}
		}
		diffSummary = new SVNDiffSummary[diffs.size()];
		diffs.toArray(diffSummary);
		return diffSummary;
	}

	public void setLocalResource1(ISVNResource localResource1) {
		this.localResource1 = localResource1;
	}

	public void setLocalResource2(ISVNResource localResource2) {
		this.localResource2 = localResource2;
	}

}
