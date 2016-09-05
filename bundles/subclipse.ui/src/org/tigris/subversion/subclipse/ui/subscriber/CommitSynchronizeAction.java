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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.synchronize.ChangeSetDiffNode;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Put action that appears in the synchronize view. It's main purpose is
 * to filter the selection and delegate its execution to the put operation.
 */
public class CommitSynchronizeAction extends SynchronizeModelAction {
	private ArrayList changeSets;

	public CommitSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	public CommitSynchronizeAction(String text, ISynchronizePageConfiguration configuration, ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING});
	}
	
	private static void collectAllNodes(IDiffElement element, Set nodes, List selected) {
		boolean added = false;
		if(element.getKind() != SyncInfo.IN_SYNC) {
			nodes.add(element);
			added = true;
		}
		if(element instanceof IDiffContainer) {
			if (added)
			{
				// if a container itself was changed/added then check if there is a selection inside it. 
				// Then only take that one and not auto all its children
				for (int i = 0; i < selected.size(); i++) {
					Object object = selected.get(i);
					if(object instanceof IDiffElement && object != element) {
						IDiffContainer parent = ((IDiffElement)object).getParent();
						while (parent != null)
						{
							if (parent == element) return;
							parent = parent.getParent();
						}
					}
				}
			}
			IDiffElement[] children = ((IDiffContainer)element).getChildren();
			for (int i = 0; i < children.length; i++) {
				collectAllNodes(children[i], nodes,selected);
			}
		}
	}

	
	/**
	 * Return the selected diff element for which this action is enabled.
	 * @return the list of selected diff elements for which this action is
	 *               enabled.
	 */
	protected final IDiffElement[] getFilteredDiffElementsOverride() {
		List selected = getStructuredSelection().toList();
		Set result = new HashSet();
		for (int i = 0; i < selected.size(); i++) {
			Object object = selected.get(i);
			if(object instanceof IDiffElement) {
				collectAllNodes((IDiffElement)object, result, selected);
			}
		}
		Iterator it = result.iterator();
		List filtered = new ArrayList();
		while(it.hasNext()) {
			IDiffElement e = (IDiffElement) it.next();
			if (e instanceof SyncInfoModelElement) {
				SyncInfo info = ((SyncInfoModelElement) e).getSyncInfo();
				if (info != null && getSyncInfoFilter().select(info)) {
					filtered.add(e);
				}
			}
		}
		return (IDiffElement[]) filtered.toArray(new IDiffElement[filtered.size()]);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
	 */
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		changeSets = new ArrayList();
		// override the elemenents (this has to be done this way because of final methods in eclipse api)
		elements = getFilteredDiffElementsOverride();
		String url = null;
		ChangeSet changeSet = null;
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
			            	if (!e.operationInterrupted()) {
			            		SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			            	}
			            }	    
				    }
				}
			} else {
				if (selection.size() == 1) {
					ChangeSetDiffNode changeSetDiffNode = (ChangeSetDiffNode)synchronizeModelElement;
					changeSet = changeSetDiffNode.getSet();
				}
			}
		}
		CommitSynchronizeOperation operation = new CommitSynchronizeOperation(configuration, elements, url, proposedComment);
	    operation.setChangeSet(changeSet);
		return operation;
	}	
	
	private String getProposedComment(String proposedComment, ISynchronizeModelElement synchronizeModelElement) {
		if (synchronizeModelElement instanceof ChangeSetDiffNode) {
			ChangeSet set = ((ChangeSetDiffNode)synchronizeModelElement).getSet();			
			if (!changeSets.contains(set)) {
				changeSets.add(set);
				if (proposedComment.length() > 0) proposedComment = proposedComment + System.getProperty("line.separator"); //$NON-NLS-1$
				return proposedComment + set.getComment();
			}
		}
		IDiffContainer parent = synchronizeModelElement.getParent();
		while (parent != null) {
			if (parent instanceof ChangeSetDiffNode) {
				ChangeSet set = ((ChangeSetDiffNode)parent).getSet();
				if (!changeSets.contains(set)) {
					changeSets.add(set);
					if (proposedComment.length() > 0) proposedComment = proposedComment + System.getProperty("line.separator"); //$NON-NLS-1$
					return proposedComment + set.getComment();	
				} else parent = parent.getParent();
			} else parent = parent.getParent();
		}
		return proposedComment;
	}
}
