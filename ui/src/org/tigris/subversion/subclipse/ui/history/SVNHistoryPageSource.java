/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;

/**
 * <code>IHistoryPageSource</code> implementation for Subclipse history
 * 
 * @author Eugene Kuleshov
 */
public class SVNHistoryPageSource extends HistoryPageSource {

  public boolean canShowHistoryFor(Object object) {
    return (object instanceof IResource && ((IResource) object).getType() != IResource.ROOT)
        || (object instanceof ISVNRemoteResource);
  }

  public Page createPage(Object object) {
    SVNHistoryPage page = new SVNHistoryPage(object);
    return page;
  }

}

