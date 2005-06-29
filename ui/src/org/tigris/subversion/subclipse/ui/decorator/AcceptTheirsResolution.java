package org.tigris.subversion.subclipse.ui.decorator;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IMarkerResolution;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;

public class AcceptTheirsResolution implements IMarkerResolution {

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    public String getLabel() {
        return Policy.bind("AcceptTheirs.label"); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
     */
    public void run(IMarker marker) {
        try {
            new RevertOperation(null, new IResource[] {marker.getResource()}).run(new NullProgressMonitor());
        } catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof SVNException) {
				SVNUIPlugin.log((SVNException)e.getTargetException());
			} else {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
        } catch (InterruptedException e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
        }
        
    }

}
