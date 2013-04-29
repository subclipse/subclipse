package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
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

	@Override
	protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
		return new BaseDiffNode((IDiffContainer) data, result, (ITypedElement)ancestor, (ITypedElement)left, (ITypedElement)right);
	}   
    
}
