/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Table;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class TableSetter {
    private IDialogSettings settings;

    public TableSetter() {
        super();
        settings = SVNUIPlugin.getPlugin().getDialogSettings();
    }
    
    public void saveColumnWidths(Table table, String qualifier) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            settings.put(qualifier + ".column" + i, table.getColumn(i).getWidth()); //$NON-NLS-1$
        }
    }
    
    public void saveColumnWeights(Table table, String qualifier) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            settings.put(qualifier + ".columnWeight" + i, getColumnWeight(table, i)); //$NON-NLS-1$
        }        
    }
    
    public float getColumnWeight(Table table, int column) {
        int tableWidth = table.getSize().x;
        int columnWidth = table.getColumn(column).getWidth();
        if (tableWidth > columnWidth) return ((float)columnWidth)/tableWidth;
        return 1/3F;
    }

    public void saveSorterColumn(String qualifier, int sortColumn) {
        settings.put(qualifier + ".sortColumn", sortColumn); //$NON-NLS-1$
    }
    
    public void saveSorterReversed(String qualifier, boolean sorterReversed) {
        settings.put(qualifier + ".sortReversed", sorterReversed); //$NON-NLS-1$
    }
    
    public int[] getColumnWidths(String qualifier, int columnCount) {
        int[] widths = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            try {
                widths[i] = settings.getInt(qualifier + ".column" + i); //$NON-NLS-1$
            } catch (NumberFormatException e) {
                widths[i] = 150;
            }
        }
        return widths;
    }
    
    public float[] getColumnWeights(String qualifier, int columnCount) {
        float[] weights = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            try {
                weights[i] = settings.getFloat(qualifier + ".columnWeight" + i); //$NON-NLS-1$
            } catch (NumberFormatException e) {
                weights[i] = 1/3F;
            }
        }
        return weights;        
    }
    
    public int getSorterColumn(String qualifier) {
        int sortColumn;
        try {
            sortColumn = settings.getInt(qualifier + ".sortColumn"); //$NON-NLS-1$
        } catch (NumberFormatException e) {
            sortColumn = -1;
        }        
        return sortColumn;
    }
    
    public boolean getSorterReversed(String qualifier) {
       return settings.getBoolean(qualifier + ".sortReversed"); //$NON-NLS-1$
    }

}
