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
package org.tigris.subversion.subclipse.ui.pending;

 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;
import org.tigris.subversion.subclipse.core.SVNException;

/**
 * Used to perform a drop of a file to HistoryView 
 */
public class PendingDropAdapter extends ViewerDropAdapter {
	PendingOperationsView view;
	
	public PendingDropAdapter(StructuredViewer viewer, PendingOperationsView view) {
		super(viewer);
		this.view = view;
	}
	/*
	 * Override dragOver to slam the detail to DROP_LINK, as we do not
	 * want to really execute a DROP_MOVE, although we want to respond
	 * to it.
	 */
	public void dragOver(DropTargetEvent event) {
		if ((event.operations & DND.DROP_LINK) == DND.DROP_LINK) {
			event.detail = DND.DROP_LINK;
		}
		super.dragOver(event);
	}
	/*
	 * Override drop to slam the detail to DROP_LINK, as we do not
	 * want to really execute a DROP_MOVE, although we want to respond
	 * to it.
	 */
	public void drop(DropTargetEvent event) {
		super.drop(event);
		event.detail = DND.DROP_LINK;
	}
	public boolean performDrop(Object data) {
		if (data == null) return false;
		IResource[] sources = (IResource[])data;
		if (sources.length == 0) return false;
		IResource resource = sources[0];
		if (!(resource instanceof IContainer)) return false;
		try {
            view.showPending((IContainer)resource);
        } catch (SVNException e) {
            return false;
        }
		return true;
	}
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (transferType != null && ResourceTransfer.getInstance().isSupportedType(transferType)) {
			return true;
		}
		return false;
	}
}

