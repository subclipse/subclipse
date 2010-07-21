package org.tigris.subversion.subclipse.ui.compare;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperationWC;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SVNLocalBranchTagCompareInput extends CompareEditorInput implements ISaveableWorkbenchPart {
	private ISVNLocalResource[] resources;
	private SVNUrl[] urls;
	private SVNRevision remoteRevision;
	private IWorkbenchPart targetPart;
	private Exception getDiffException;
	
	private SVNLocalResourceNode[] localResourceNodes;
	private ResourceEditionNode[] remoteResourceNodes;
	
	public SVNLocalBranchTagCompareInput(ISVNLocalResource[] resources, SVNUrl[] urls, SVNRevision remoteRevision, IWorkbenchPart targetPart) throws SVNException {
		super(new CompareConfiguration());
		this.resources = resources;
		this.urls = urls;
		this.remoteRevision = remoteRevision;
		this.targetPart = targetPart;
		
        localResourceNodes = new SVNLocalResourceNode[resources.length];
        remoteResourceNodes = new ResourceEditionNode[resources.length];
        
        for (int i = 0; i < resources.length; i++) {
        	localResourceNodes[i] = new SVNLocalResourceNode(resources[i]);
        	ISVNRemoteResource remoteResource;
        	if (resources[i] instanceof ISVNLocalFolder) {
        		remoteResource = new RemoteFolder(resources[i].getRepository(), urls[i], remoteRevision);
        	} else {
        		remoteResource = new RemoteFile(resources[i].getRepository(), urls[i], remoteRevision);
        	}
        	
            remoteResourceNodes[i] = new ResourceEditionNode(remoteResource);
            remoteResourceNodes[i].setLocalResource(localResourceNodes[i]);
            localResourceNodes[i].setRemoteResource(remoteResourceNodes[i]);
        }
	}
	
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();	
		setTitle("Compare <workspace> and versions");
		cc.setLeftEditable(true);
		cc.setRightEditable(false);		
		String leftLabel = "<workspace>";
		cc.setLeftLabel(leftLabel);
		String rightLabel = "Repository";
		cc.setRightLabel(rightLabel);
	}
	
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		initLabels();	
		
		File[] diffFiles = new File[localResourceNodes.length];
		try {
			for (int i = 0; i < localResourceNodes.length; i++) {
				File file = File.createTempFile("revision", ".diff");
				file.deleteOnExit();
				final ShowDifferencesAsUnifiedDiffOperationWC operation = new ShowDifferencesAsUnifiedDiffOperationWC(targetPart, localResourceNodes[i].getLocalResource().getFile(), remoteResourceNodes[i].getRemoteResource().getUrl(), remoteRevision, file);
				operation.setGraphicalCompare(true);
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						try {
							operation.run();
						} catch (Exception e) {}
					}					
				});
				diffFiles[i] = operation.getFile();
			}
		} catch (Exception e) {}
		
		MultipleSelectionNode left = new MultipleSelectionNode(localResourceNodes);
		MultipleSelectionNode right = new MultipleSelectionNode(remoteResourceNodes);
        Object differences = new RevisionAwareDifferencer(diffFiles).findDifferences(false, monitor,null,null,left,right);
        if (differences instanceof DiffNode) {
        	DiffNode diffNode = (DiffNode)differences;
        	if (!diffNode.hasChildren()) {
        		return null;
        	}
        }
        return differences;
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

}
