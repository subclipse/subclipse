/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.decorator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class ConflictResolutionGenerator implements IMarkerResolutionGenerator2 {

    public ConflictResolutionGenerator() {
        super();
    }

    public boolean hasResolutions(IMarker marker) {
        return true;
    }

    public IMarkerResolution[] getResolutions(IMarker marker) {
        IMarkerResolution[] conflictResolutions = {new EditConflictsResolution(), new AcceptMineResolution(), new AcceptTheirsResolution(), new MarkAsResolvedResolution()};
        return conflictResolutions;
    }

}
