/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.compare;

 
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * A compare input for comparing remote resources. Use <code>SVNLocalCompareInput</code> 
 * when comparing resources in the workspace to remote resources.
 * Used from CompareRemoteResourcesAction
 */
public class SVNCompareEditorInput extends SVNAbstractCompareEditorInput {
	private ITypedElement left;
	private ITypedElement right;
	private ITypedElement ancestor;	
	private Image leftImage;
	private Image rightImage;
	private Image ancestorImage;
	private String leftLabel;
	private String rightLabel;
	
	/**
	 * Creates a new SVNCompareEditorInput.
	 */
	public SVNCompareEditorInput(ResourceEditionNode left, ResourceEditionNode right) {
		this(left, right, null);
	}
	
	/**
	 * Creates a new SVNCompareEditorInput.
	 */
	public SVNCompareEditorInput(ResourceEditionNode left, ResourceEditionNode right, ResourceEditionNode ancestor) {
		super(new CompareConfiguration());
		this.left = left;
		this.right = right;
		this.ancestor = ancestor;
		if (left != null) {
			this.leftImage = left.getImage();
			if (left.getRemoteResource() instanceof RemoteResource) {
				if (((RemoteResource)left.getRemoteResource()).getPegRevision() == null) {
					((RemoteResource)left.getRemoteResource()).setPegRevision(SVNRevision.HEAD);
				}
			}
		}
		if (right != null) {
			this.rightImage = right.getImage();
			if (right.getRemoteResource() instanceof RemoteResource) {
				if (((RemoteResource)right.getRemoteResource()).getPegRevision() == null) {
					((RemoteResource)right.getRemoteResource()).setPegRevision(SVNRevision.HEAD);
				}
			}
		}
		if (ancestor != null) {
			this.ancestorImage = ancestor.getImage();
		}
	}
	
	/**
	 * Returns the label for the given input element (which is a ResourceEditionNode).
	 */
	private String getLabel(ITypedElement element) {
		if (element instanceof ResourceEditionNode) {
			ISVNRemoteResource edition = ((ResourceEditionNode)element).getRemoteResource();
			SVNRevision revision = edition.getLastChangedRevision();
			if (revision == null) {
				revision = edition.getRevision();
			}
			if (edition instanceof ISVNRemoteFile) {
				return Policy.bind("nameAndRevision", edition.getName(), revision.toString()); //$NON-NLS-1$
			}
			if (edition.isContainer()) {
				return Policy.bind("SVNCompareEditorInput.inHead", edition.getName()); //$NON-NLS-1$
			} else {
				return Policy.bind("SVNCompareEditorInput.repository", new Object[] {edition.getName(), revision.toString()}); //$NON-NLS-1$
			}
		}
		return element.getName();
	}
	
	/**
	 * Returns the label for the given input element. (which is a ResourceEditionNode)
	 */
	private String getVersionLabel(ITypedElement element) {
		if (element instanceof ResourceEditionNode) {
			ISVNRemoteResource edition = ((ResourceEditionNode)element).getRemoteResource();
			SVNRevision revision = edition.getLastChangedRevision();
			if (revision == null) {
				revision = edition.getRevision();
			}
			if (edition.isContainer()) {
				return Policy.bind("SVNCompareEditorInput.headLabel"); //$NON-NLS-1$
			} else {
				return revision.toString();
			}
		}
		return element.getName();
	}
		
	/*
	 * Returns a guess of the resource name being compared, for display
	 * in the title.
	 */
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
	
	/**
	 * Sets up the title and pane labels for the comparison view.
	 */
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();

        ITypedElement left = this.left;
        ITypedElement right = this.right;
        ITypedElement ancestor = this.ancestor;
        
        if (left != null) {
        	leftLabel = getLabel(left);
            cc.setLeftLabel(leftLabel);
            cc.setLeftImage(leftImage);
        }
    
        if (right != null) {
        	rightLabel = getLabel(right);
            cc.setRightLabel(rightLabel);
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
	
 	
	/* (Non-javadoc)
	 * Method declared on CompareEditorInput
	 */
	public boolean isSaveNeeded() {
		return false;
	}

	/* (non-Javadoc)
	 * Method declared on CompareEditorInput
	 */
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
				result[0] = new RevisionAwareDifferencer().findDifferences(threeWay, sub, null, ancestor, left, right);
			} finally {
				sub.done();
			}
			return result[0];
		} catch (OperationCanceledException e) {
			throw new InterruptedException(e.getMessage());
		} catch (RuntimeException e) {
			return e.getMessage();
		} finally {
			monitor.done();
		}
	}

	public boolean canRunAsJob() {
		return true;
	}

	public String getLeftLabel() {
		return leftLabel;
	}

	public String getRightLabel() {
		return rightLabel;
	}
	
}
