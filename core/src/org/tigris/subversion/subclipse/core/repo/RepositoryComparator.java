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
package org.tigris.subversion.subclipse.core.repo;

import java.util.Comparator;

import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

/**
 * This class allows to sort ISVNRepositoryLoction's alphabetically using the
 * URL or the label (if set). The case of the strings is ignored.
 */
public class RepositoryComparator implements Comparator {
    /**
     * @see java.util.Comparator#compare(Obejct, Object)
     */
    public int compare(Object o1, Object o2) {
        if (o1 instanceof ISVNRepositoryLocation
                && o2 instanceof ISVNRepositoryLocation) {
            ISVNRepositoryLocation loc1 = (ISVNRepositoryLocation) o1;
            ISVNRepositoryLocation loc2 = (ISVNRepositoryLocation) o2;

            String label1 = (loc1.getLabel() == null || loc1.getLabel()
                    .length() == 0) ? loc1.getLocation() : loc1.getLabel();
            String label2 = (loc2.getLabel() == null || loc2.getLabel()
                    .length() == 0) ? loc2.getLocation() : loc2.getLabel();

            return label1.compareToIgnoreCase(label2);
        }

        return 0;
    }
}