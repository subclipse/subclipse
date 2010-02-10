package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;

public class StatusAwareDifferencer extends Differencer {
    /* (non-Javadoc)
     * @see org.eclipse.compare.structuremergeviewer.Differencer#contentsEqual(java.lang.Object, java.lang.Object)
     */
    protected boolean contentsEqual(Object left, Object right) {
        ISVNLocalResource local = null;
        
        if (left instanceof SVNLocalResourceNode) {
            local = ((SVNLocalResourceNode)left).getLocalResource();
        }
        
        if (local == null || right == null) {
            return false;
        }
        
        try {
            if (!local.isManaged()) {
                return false;
            }
            return !(local.isDirty());
        } catch (SVNException e) {
        }
        
        return super.contentsEqual(left, right);
    }
}
