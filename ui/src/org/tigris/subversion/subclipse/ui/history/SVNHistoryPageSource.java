
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

/**
 * <code>IHistoryPageSource</code> implementation for Subclipse history
 * 
 * @author Eugene Kuleshov
 */
public class SVNHistoryPageSource extends HistoryPageSource {

  public boolean canShowHistoryFor(Object object) {
    return object instanceof IResource && ((IResource) object).getType() != IResource.ROOT;
  }

  public Page createPage(Object object) {
    SVNHistoryPage page = new SVNHistoryPage(object);
    return page;
  }

}

