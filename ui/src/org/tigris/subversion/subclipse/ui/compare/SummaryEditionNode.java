package org.tigris.subversion.subclipse.ui.compare;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;

public class SummaryEditionNode
		implements
			IStructureComparator,
			ITypedElement,
			IStreamContentAccessor,
			IEncodedStreamContentAccessor,
			Comparable {
	private ISVNRemoteResource resource;
	private SummaryEditionNode[] children;
	private String charset = null;
	private SVNDiffSummary[] diffSummary;
	private boolean root;
	private RemoteFolder rootFolder;
	private int nodeType = LEFT;
	
	public final static int LEFT = 0;
	public final static int RIGHT = 1;

	public SummaryEditionNode(ISVNRemoteResource resourceEdition) {
		this.resource = resourceEdition;
	}

	public ISVNRemoteResource getRemoteResource() {
		return resource;
	}

	public boolean equals(Object other) {
		if (other instanceof ITypedElement) {
			String otherName = ((ITypedElement) other).getName();
			return getName().equals(otherName);
		}
		return super.equals(other);
	}

	public Object[] getChildren() {
		if (children == null) {
			try {
				if (root) {
					children = getRoots();
				}
				else if (!resource.isContainer()) children = new SummaryEditionNode[0];
				else children = getChildNodes();
			}catch (Exception e) {}
		}
		return children;
	}
	
	private SummaryEditionNode[] getChildNodes() throws Exception {
		ArrayList childNodes = new ArrayList();	
		for (int i = 0; i < diffSummary.length; i++) {
			if (include(diffSummary[i])) {
				if (diffSummary[i].getNodeKind() == SVNNodeKind.DIR.toInt()) {
					RemoteFolder remoteFolder = new RemoteFolder(null, resource.getRepository(), new SVNUrl(rootFolder.getUrl().toString() + "/" + diffSummary[i].getPath()), resource.getRevision(), (SVNRevision.Number)resource.getRevision(), null, null);
					if (isChild(remoteFolder)) {
						SummaryEditionNode node = new SummaryEditionNode(remoteFolder);
						node.setDiffSummary(diffSummary);
						node.setRootFolder(rootFolder);
						node.setNodeType(nodeType);
						childNodes.add(node);
					}
				} else {
					RemoteFile remoteFile = new RemoteFile(null, resource.getRepository(), new SVNUrl(rootFolder.getUrl().toString() + "/" + diffSummary[i].getPath()), resource.getRevision(), (SVNRevision.Number)resource.getRevision(), null, null);
					if (isChild(remoteFile)) {
						SummaryEditionNode node = new SummaryEditionNode(remoteFile);	
						node.setDiffSummary(diffSummary);
						node.setRootFolder(rootFolder);
						node.setNodeType(nodeType);
						childNodes.add(node);	
					}
				}	
			}
		}	
		SummaryEditionNode[] childNodeArray = new SummaryEditionNode[childNodes.size()];
		childNodes.toArray(childNodeArray);
		Arrays.sort(childNodeArray);
		return childNodeArray;		
	}
	
	private boolean isChild(ISVNRemoteResource remoteResource) {
		File parentFile = new File(resource.getRepositoryRelativePath());
		File childFile = new File(remoteResource.getRepositoryRelativePath());
		if (childFile.getParent() != null && childFile.getParentFile().equals(parentFile))
			return true;
		else
			return false;
	}
	
	private SummaryEditionNode[] getRoots() throws Exception {
		ArrayList roots = new ArrayList();
		for (int i = 0; i < diffSummary.length; i++) {
			if (include(diffSummary[i])) {
				File file = new File(diffSummary[i].getPath());
				if (file.getParent() == null) {
					if (diffSummary[i].getNodeKind() == SVNNodeKind.DIR.toInt()) {
						RemoteFolder remoteFolder = new RemoteFolder(null, resource.getRepository(), new SVNUrl(resource.getUrl().toString() + "/" + diffSummary[i].getPath()), resource.getRevision(), (SVNRevision.Number)resource.getRevision(), null, null);
						SummaryEditionNode node = new SummaryEditionNode(remoteFolder);
						node.setDiffSummary(diffSummary);
						node.setRootFolder((RemoteFolder)resource);
						node.setNodeType(nodeType);
						roots.add(node);
					} else {
						RemoteFile remoteFile = new RemoteFile(null, resource.getRepository(), new SVNUrl(resource.getUrl().toString() + "/" + diffSummary[i].getPath()), resource.getRevision(), (SVNRevision.Number)resource.getRevision(), null, null);
						SummaryEditionNode node = new SummaryEditionNode(remoteFile);	
						node.setDiffSummary(diffSummary);
						node.setRootFolder((RemoteFolder)resource);
						node.setNodeType(nodeType);
						roots.add(node);				
					}				
				} else {
					while (file.getParent() != null) {
						file = file.getParentFile();
					}
					String path = file.getPath();
					if (!roots.contains(path)) {
						RemoteFolder remoteFolder = new RemoteFolder(null, resource.getRepository(), new SVNUrl(resource.getUrl().toString() + "/" + path), resource.getRevision(), (SVNRevision.Number)resource.getRevision(), null, null);
						SummaryEditionNode node = new SummaryEditionNode(remoteFolder);
						node.setDiffSummary(diffSummary);
						node.setRootFolder((RemoteFolder)resource);
						node.setNodeType(nodeType);
						roots.add(node);
					}
				}
			}
		}
		SummaryEditionNode[] rootArray = new SummaryEditionNode[roots.size()];
		roots.toArray(rootArray);
		Arrays.sort(rootArray);
		return rootArray;
	}

	public InputStream getContents() throws CoreException {
		if (resource == null || resource.isContainer()) {
			return null;
		}
		try {
			final InputStream[] holder = new InputStream[1];
			SVNUIPlugin.runWithProgress(null, true,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							try {
								holder[0] = resource.getStorage(monitor).getContents();
							} catch (CoreException e1) {
//								SVNUIPlugin.log(e1);
							}
						}
					});
			return holder[0];
		} catch (InterruptedException e) {
			// operation canceled
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof TeamException) {
				throw new CoreException(((TeamException) t).getStatus());
			}
			// should not get here
		}
		return new ByteArrayInputStream(new byte[0]);
	}

	public Image getImage() {
		return CompareUI.getImage(resource);
	}

	public String getName() {
		return resource == null ? "" : resource.getName(); //$NON-NLS-1$
	}

	public String getType() {
		if (resource == null) {
			return UNKNOWN_TYPE;
		}
		if (resource.isContainer()) {
			return FOLDER_TYPE;
		}
		String name = resource.getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		return name.length() == 0 ? UNKNOWN_TYPE : name;
	}

	public int hashCode() {
		return getName().hashCode();
	}
	
	public String getCharset() throws CoreException {
		return charset;
	}
	
	public void setCharset(String charset) throws CoreException {
		this.charset = charset;
	}

	public void setDiffSummary(SVNDiffSummary[] diffSummary) {
		this.diffSummary = diffSummary;
	}
	public void setRoot(boolean root) {
		this.root = root;
	}

	public int compareTo(Object obj) {
		if (obj instanceof SummaryEditionNode) {
			SummaryEditionNode compareTo = (SummaryEditionNode)obj;
			return resource.getRepositoryRelativePath().compareTo(compareTo.getRemoteResource().getRepositoryRelativePath());
		}
		return 0;
	}

	public String toString() {
		return resource.toString();
	}

	public void setRootFolder(RemoteFolder rootFolder) {
		this.rootFolder = rootFolder;
	}

	public void setNodeType(int nodeType) {
		this.nodeType = nodeType;
	}
	
	private boolean include(SVNDiffSummary diff) {
		if (diff.getDiffKind().equals(SVNDiffKind.ADDED) && nodeType == LEFT) return false;
		if (diff.getDiffKind().equals(SVNDiffKind.DELETED) && nodeType == RIGHT) return false;
		return true;
	}
	
}