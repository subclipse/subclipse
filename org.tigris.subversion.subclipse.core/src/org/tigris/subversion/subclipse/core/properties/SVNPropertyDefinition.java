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
package org.tigris.subversion.subclipse.core.properties;

/**
 * @author Brock Janiczak
 */
public class SVNPropertyDefinition  implements Comparable {
    
    private final String name;
    private final String description;
    private String type = STRING;
    private int showFor;
    private boolean allowRecurse = true;
    
    public final static int BOTH = 0;
    public final static int FOLDER = 1;
    public final static int FILE = 2;
    
    public final static String STRING = "String"; //$NON-NLS-1$
    public final static String NUMBER = "Number"; //$NON-NLS-1$
    public final static String URL = "URL"; //$NON-NLS-1$
    public final static String BOOLEAN = "Boolean"; //$NON-NLS-1$

    public SVNPropertyDefinition(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public SVNPropertyDefinition(String name, String description, int showFor) {
        this(name, description);
        this.showFor = showFor;
    }
    
    public SVNPropertyDefinition(String name, String description, int showFor, boolean allowRecurse) {
        this(name, description, showFor);
        this.allowRecurse = allowRecurse;
    }
    
    public SVNPropertyDefinition(String name, String description, int showFor, boolean allowRecurse, String type) {
        this(name, description, showFor, allowRecurse);
        this.type = type;
    }      
    
    public String getDescription() {
        return this.description;
    }
    
    public String getName() {
        return this.name;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.name;
    }

    public int compareTo(Object property) {
        SVNPropertyDefinition compare = (SVNPropertyDefinition)property;
        return name.compareTo(compare.getName());
    }
    
    public int getShowFor() {
        return showFor;
    }
    
    public boolean showForFile() {
        return ((showFor == BOTH) || (showFor == FILE));
    }
    
    public boolean showForFolder() {
        return ((showFor == BOTH) || (showFor == FOLDER));
    }    
    
    public boolean isAllowRecurse() {
        return allowRecurse;
    }
    public String getType() {
        return type;
    }
    
    public boolean isNumber() {
        return type.equals(NUMBER);
    }
    
    public boolean isBoolean() {
        return type.equals(BOOLEAN);
    }
    
    public boolean isUrl() {
        return type.equals(URL);
    }

	public boolean equals(Object object) {
		if (!(object instanceof SVNPropertyDefinition)) return false;
		return ((SVNPropertyDefinition)object).getName().equals(name);
	}
    
    
}
