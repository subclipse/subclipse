package org.tigris.subversion.subclipse.ui.decorator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.actions.EditConflictsAction;

public class EditConflictsResolution implements IMarkerResolution {

    public EditConflictsResolution() {
        super();
    }

    public String getLabel() {
        return Policy.bind("EditConflicts.Label"); //$NON-NLS-1$
    }

    public void run(IMarker marker) {
        new EditConflictsAction((IFile)marker.getResource()).run(null);
    }

}
