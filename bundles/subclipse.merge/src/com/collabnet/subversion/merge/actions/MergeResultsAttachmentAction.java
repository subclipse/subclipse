/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

public abstract class MergeResultsAttachmentAction extends BaseSelectionListenerAction
    implements IViewActionDelegate {
  protected ISelection currentSelection;

  public MergeResultsAttachmentAction(String text) {
    super(text);
  }

  public void init(IViewPart view) {}

  public void selectionChanged(IAction action, ISelection selection) {
    this.currentSelection = selection;
    if (action != null) action.setEnabled(enabledForSelection());
  }

  public abstract boolean enabledForSelection();

  //	protected RepositoryAttachment getRepositoryAttachment() {
  //		if (currentSelection instanceof StructuredSelection) {
  //			Object object = ((StructuredSelection) currentSelection).getFirstElement();
  //			if (object instanceof RepositoryAttachment) {
  //				return (RepositoryAttachment)object;
  //			}
  //		}
  //		return null;
  //	}

}
