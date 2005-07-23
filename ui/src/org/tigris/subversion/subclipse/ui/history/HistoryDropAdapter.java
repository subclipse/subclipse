/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.ui.actions.RemoteResourceTransfer;

/**
 * Used to perform a drop of a file to HistoryView 
 */
public class HistoryDropAdapter extends ViewerDropAdapter {
	HistoryView view;
	
	public HistoryDropAdapter(StructuredViewer viewer, HistoryView view) {
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
        if(data instanceof IResource[]) {
           IResource[] sources = (IResource[])data;
           if (sources.length == 0) return false;
           IResource resource = sources[0];
           view.showHistory(resource, true /* fetch */);
           return true;
        } else if( data instanceof ISVNRemoteResource) {
            view.showHistory((ISVNRemoteResource) data, null); // true /* fetch */);
            return true;
        }
        return false;        
	}
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (transferType != null && 
            (ResourceTransfer.getInstance().isSupportedType(transferType) ||
             RemoteResourceTransfer.getInstance().isSupportedType(transferType))) {
			return true;
		}
		return false;
	}
}

