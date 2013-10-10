package org.tigris.subversion.subclipse.ui.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.BufferedResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SVNLocalBaseCompareInput extends CompareEditorInput implements ISaveableWorkbenchPart {
	private Object fRoot;
	private final SVNRevision remoteRevision;
	private boolean readOnly;
	
	private SVNLocalResourceNode[] localResourceNodes;
	private ResourceEditionNode[] remoteResourceNodes;
	
	public SVNLocalBaseCompareInput(ISVNLocalResource[] resources, SVNRevision revision, boolean readOnly) throws SVNException, SVNClientException {
		super(new CompareConfiguration());
        this.remoteRevision = revision;
        this.readOnly = readOnly;
        
        localResourceNodes = new SVNLocalResourceNode[resources.length];
        remoteResourceNodes = new ResourceEditionNode[resources.length];
        for (int i = 0; i < resources.length; i++) {
        	localResourceNodes[i] = new SVNLocalResourceNode(resources[i]);
        	ISVNRemoteResource remoteResource = null;
        	LocalResourceStatus status = resources[i].getStatus();
            if (status != null && status.isCopied()) {
            	ISVNClientAdapter svnClient = null;
            	try {
	            	svnClient = resources[i].getRepository().getSVNClient();
	            	ISVNInfo info = svnClient.getInfoFromWorkingCopy(resources[i].getFile());
	            	SVNUrl copiedFromUrl = info.getCopyUrl();
	            	if (copiedFromUrl != null) {
	            		GetRemoteResourceCommand getRemoteResourceCommand = new GetRemoteResourceCommand(resources[i].getRepository(), copiedFromUrl, SVNRevision.HEAD);
	            		getRemoteResourceCommand.run(null);
	            		remoteResource = getRemoteResourceCommand.getRemoteResource();
	            	}
            	}
            	finally {
            		resources[i].getRepository().returnSVNClient(svnClient);
            	}
            }
            if (remoteResource == null) remoteResource = resources[i].getRemoteResource(revision);
            remoteResourceNodes[i] = new ResourceEditionNode(remoteResource);
            remoteResourceNodes[i].setLocalResource(localResourceNodes[i]);
            localResourceNodes[i].setRemoteResource(remoteResourceNodes[i]);
        }
	}

	/**
	 * Constructor which allows 
	 * @throws SVNException
	 * creates a SVNLocalCompareInput, defaultin to read/write.  
	 */
	public SVNLocalBaseCompareInput(ISVNLocalResource[] resources, SVNRevision revision) throws SVNException, SVNClientException {
		this(resources, revision, false);
	}
	
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		cc.setLeftEditable(! readOnly);
		cc.setRightEditable(false);
		String title;
		String leftLabel;
		String rightLabel;
		if (localResourceNodes.length > 1) {
			title = Policy.bind("SVNLocalBaseCompareInput.0") + remoteRevision; //$NON-NLS-1$
			leftLabel = Policy.bind("SVNLocalBaseCompareInput.1"); //$NON-NLS-1$
			rightLabel = remoteRevision.toString();
		} else {
			title = Policy.bind("SVNCompareRevisionsInput.compareResourceAndVersions", new Object[] {localResourceNodes[0].getName()}); //$NON-NLS-1$
			leftLabel = Policy.bind("SVNCompareRevisionsInput.workspace", new Object[] {localResourceNodes[0].getName()}); //$NON-NLS-1$
			rightLabel = Policy.bind("SVNCompareRevisionsInput.repository", new Object[] {localResourceNodes[0].getName()}); //$NON-NLS-1$
		}
		setTitle(title);			
		cc.setLeftLabel(leftLabel);		
		cc.setRightLabel(rightLabel);
	}

	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		initLabels();
		MultipleSelectionNode left = new MultipleSelectionNode(localResourceNodes);
		MultipleSelectionNode right = new MultipleSelectionNode(remoteResourceNodes);
        Object differences = new StatusAwareDifferencer().findDifferences(false, monitor,null,null,left,right);
        if (differences instanceof DiffNode) {
        	DiffNode diffNode = (DiffNode)differences;
        	if (!diffNode.hasChildren()) {
        		return null;
        	}
        }
        fRoot = differences;
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
	
	public void saveChanges(IProgressMonitor pm) throws CoreException {
		super.saveChanges(pm);
		if (fRoot instanceof DiffNode) {
			try {
				commit(pm, (DiffNode) fRoot);
			} finally {		
				setDirty(false);
			}
		}
	}
	
	private static void commit(IProgressMonitor pm, DiffNode node) throws CoreException {
		ITypedElement left= node.getLeft();
		if (left instanceof BufferedResourceNode)
			((BufferedResourceNode) left).commit(pm);
			
		ITypedElement right= node.getRight();
		if (right instanceof BufferedResourceNode)
			((BufferedResourceNode) right).commit(pm);

		IDiffElement[] children= node.getChildren();
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				IDiffElement element= children[i];
				if (element instanceof DiffNode)
					commit(pm, (DiffNode) element);
			}
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
