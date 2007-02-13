/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.synchronize.ChangeSetDiffNode;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;

/**
 * Put action that appears in the synchronize view. It's main purpose is
 * to filter the selection and delegate its execution to the put operation.
 */
public class CommitSynchronizeAction extends SynchronizeModelAction {

	public CommitSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
	 */
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		String url = null;
	    IStructuredSelection selection = getStructuredSelection();
		Iterator iter = selection.iterator();
		String proposedComment = "";
		while (iter.hasNext()) {
			ISynchronizeModelElement synchronizeModelElement = (ISynchronizeModelElement)iter.next();
			proposedComment = getProposedComment(proposedComment, synchronizeModelElement);
			if (!(synchronizeModelElement instanceof ChangeSetDiffNode)) {
				if (url == null && selection.size() == 1) {
				    IResource resource = synchronizeModelElement.getResource();
				    if (resource != null) {
					    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			            try {
			                url = svnResource.getStatus().getUrlString();
			                if ((url == null) || (resource.getType() == IResource.FILE)) url = Util.getParentUrl(svnResource);
			            } catch (SVNException e) {
			                e.printStackTrace();
			            }	    
				    }
				}
			}
		}
	    return new CommitSynchronizeOperation(configuration, elements, url, proposedComment);
	}	
	
	private String getProposedComment(String proposedComment, ISynchronizeModelElement synchronizeModelElement) {
		if (synchronizeModelElement instanceof ChangeSetDiffNode) {
			if (proposedComment.length() > 0) proposedComment = proposedComment + System.getProperty("line.separator"); //$NON-NLS-1$
			ChangeSet set = ((ChangeSetDiffNode)synchronizeModelElement).getSet();
			return proposedComment + set.getComment();
		}
		IDiffContainer parent = synchronizeModelElement.getParent();
		while (parent != null) {
			if (parent instanceof ChangeSetDiffNode) {
				if (proposedComment.length() > 0) proposedComment = proposedComment + System.getProperty("line.separator"); //$NON-NLS-1$
				ChangeSet set = ((ChangeSetDiffNode)parent).getSet();
				return proposedComment + set.getComment();				
			} else parent = parent.getParent();
		}
		return proposedComment;
	}
}
