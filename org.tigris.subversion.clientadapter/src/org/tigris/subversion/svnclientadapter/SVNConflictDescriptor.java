/*******************************************************************************
 * Copyright (c) 2005, 2006 svnClientAdapter project and others.
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
 * The description of a merge conflict, encountered during
 * merge/update/switch operations.
 *
 * copied from JavaHL ConflictDescriptor
 */
public class SVNConflictDescriptor
{
    private String path;

    /**
     * @see .Kind
     */
    private int conflictKind;

    /**
     * @see org.tigris.subversion.javahl.NodeKind
     */
    private int nodeKind;

    private String propertyName;

    private boolean isBinary;
    private String mimeType;

    private int action;
    private int reason;
    private int operation;
    private SVNConflictVersion srcLeftVersion;
    private SVNConflictVersion srcRightVersion;

    // File paths, present only when the conflict involves the merging
    // of two files descended from a common ancestor, here are the
    // paths of up to four fulltext files that can be used to
    // interactively resolve the conflict.
    private String basePath;
    private String theirPath;
    private String myPath;
    private String mergedPath;

    public SVNConflictDescriptor(String path, int conflictKind, int nodeKind, 
    		           String propertyName, boolean isBinary,
                       String mimeType, int action, int reason, int operation,
                       SVNConflictVersion srcLeftVersion, SVNConflictVersion srcRightVersion,
                       String basePath, String theirPath,
                       String myPath, String mergedPath)
    {
        this.path = path;
        this.conflictKind = conflictKind;
        this.nodeKind = nodeKind;
        this.propertyName = propertyName;
        this.isBinary = isBinary;
        this.mimeType = mimeType;
        this.action = action;
        this.reason = reason;
        this.srcLeftVersion = srcLeftVersion;
        this.srcRightVersion = srcRightVersion;
        this.operation = operation;
        this.basePath = basePath;
        this.theirPath = theirPath;
        this.myPath = myPath;
        this.mergedPath = mergedPath;
    }
    
    public SVNConflictDescriptor(String path, int action, int reason, int operation, SVNConflictVersion srcLeftVersion, SVNConflictVersion srcRightVersion) {
    	this.path = path;
    	this.action = action;
    	this.reason = reason;
    	this.operation = operation;
        this.srcLeftVersion = srcLeftVersion;
        this.srcRightVersion = srcRightVersion;
    }

    public String getPath()
    {
        return path;
    }

    public int getConflictKind()
    {
        return conflictKind;
    }

    public int getNodeKind()
    {
        return nodeKind;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public boolean isBinary()
    {
        return isBinary;
    }

    public String getMIMEType()
    {
        return mimeType;
    }

    public int getAction()
    {
        return action;
    }

    public int getReason()
    {
        return reason;
    }
    
    public boolean isTreeConflict() {
    	return reason == SVNConflictDescriptor.Reason.deleted || reason == SVNConflictDescriptor.Reason.moved_away || reason == SVNConflictDescriptor.Reason.missing || reason == SVNConflictDescriptor.Reason.obstructed;
    }
    
    public int getOperation()
    {
    	return operation;
    }
    
    public SVNConflictVersion getSrcLeftVersion()
    {
    	return srcLeftVersion;
    }
    
    public SVNConflictVersion getSrcRightVersion()
    {
    	return srcRightVersion;
    }    

    public String getBasePath()
    {
        return basePath;
    }

    public String getTheirPath()
    {
        return theirPath;
    }

    public String getMyPath()
    {
        return myPath;
    }

    public String getMergedPath()
    {
        return mergedPath;
    }
    /**
     * From JavaHL.
     */
    public final class Kind
    {
        /**
         * Attempting to change text or props.
         */
        public static final int text = 0;

        /**
         * Attempting to add object.
         */
        public static final int property = 1;
    }
   
    /**
     * From JavaHL
     */
    public final class Action
    {
        /**
         * Attempting to change text or props.
         */
        public static final int edit = 0;

        /**
         * Attempting to add object.
         */
        public static final int add = 1;

        /**
         * Attempting to delete object.
         */
        public static final int delete = 2;
    }

    /**
     * From JavaHL
     */
    public final class Reason
    {
        /**
         * Local edits are already present.
         */
        public static final int edited = 0;

        /**
         * Another object is in the way.
         */
        public static final int obstructed = 1;

        /**
         * Object is already schedule-delete.
         */
        public static final int deleted = 2;

        /**
         * Object is unknown or missing.
         */
        public static final int missing = 3;

        /**
         * Object is unversioned.
         */
        public static final int unversioned = 4;
        
        /**
         * Object is already added or schedule-add.
         */
        public static final int added = 5;
        
        /**
         * Object is already replaced.
         */
        public static final int replaced = 6;
        
        /**
         * Object is moved away.
         */
        public static final int moved_away = 7;
        
        /**
         * Object is moved here.
         */
        public static final int moved_here = 8;
    }
    
    public final class Operation
    {
        public static final int _none = 0;
        public static final int _update = 1;
        public static final int _switch = 2;
        public static final int _merge = 3;
    }    
}
