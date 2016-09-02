/*******************************************************************************
 * Copyright (c) 2004, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter;

/**
 * Schedule kind an entry can be in.
 * @see ISVNInfo#getSchedule()
 */
public class SVNScheduleKind {
    private int kind;
    
    private static final int normal = 0;
    private static final int add = 1;
    private static final int delete = 2;
    private static final int replace = 3;
    
    /** exists, but uninteresting */
    public static final SVNScheduleKind NORMAL = new SVNScheduleKind(normal);

    /** Slated for addition */
    public static final SVNScheduleKind ADD = new SVNScheduleKind(add);

    /** Slated for deletion */
    public static final SVNScheduleKind DELETE = new SVNScheduleKind(delete);

    /** Slated for replacement (delete + add) */
    public static final SVNScheduleKind REPLACE = new SVNScheduleKind(replace);
 
    private SVNScheduleKind(int kind) {
         this.kind = kind;
    }

    /**
     * @return an integer value representation of the scheduleKind
     */
    public int toInt() {
    	return kind;
    }
    
    /**
     * Returns the SVNScheduleKind corresponding to the given int representation.
     * (As returned by {@link SVNScheduleKind#toInt()} method)
     * @param scheduleKind
     * @return SVNScheduleKind representing the int value
     */
    public SVNScheduleKind fromInt(int scheduleKind) {
        switch(scheduleKind) 
        {
            case normal: 
                return NORMAL;
            case add: 
                return ADD;
            case delete: 
                return DELETE;
            case replace: 
                return REPLACE;
            default:
                return null;
        }
    }
    
    /**
     * returns the ScheduleKind corresponding to the given string or null
     * @param scheduleKind
     * @return SVNScheduleKind representing the supplied string value 
     */
    public static SVNScheduleKind fromString(String scheduleKind) {
    	if (NORMAL.toString().equals(scheduleKind)) {
    		return NORMAL;
    	} else
        if (ADD.toString().equals(scheduleKind)) {
        	return ADD;
        } else    		
        if (DELETE.toString().equals(scheduleKind)) {
        	return DELETE;
        } else
        if (REPLACE.toString().equals(scheduleKind)) {
        	return REPLACE;  
        } else
        	return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch(kind) 
        {
            case normal: 
                return "normal";
            case add: 
                return "add";
            case delete: 
                return "delete";
            case replace: 
                return "replace";
            default:
                return "";
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof SVNScheduleKind)) {
            return false;
        }
        return ((SVNScheduleKind)obj).kind == kind;
    }    

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return new Integer(kind).hashCode();
    }     
    
}
