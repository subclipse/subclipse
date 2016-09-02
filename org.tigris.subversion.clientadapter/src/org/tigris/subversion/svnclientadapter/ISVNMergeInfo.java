/*******************************************************************************
 * Copyright (c) 2007 svnClientAdapter project and others.
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

public interface ISVNMergeInfo {
    /**
     * Add one or more SVNRevisionRange objects to merge info. If path is already
     * stored, the list of revisions is replaced.
     * @param path The merge source path.
     * @param range List of SVNRevisionRange objects to add.
     */
    public void addRevisions(String path, SVNRevisionRange[] range);
 
    /**
     * Add a revision range to the merged revisions for a path.  If
     * the path already has associated revision ranges, add the
     * revision range to the existing list.
     * @param path The merge source path.
     * @param range The revision range to add.
     */
    public void addRevisionRange(String path, SVNRevisionRange range);
 
    /**
     * Get the merge source paths.
     * @return The merge source paths.
     */
    public String[] getPaths();

    /**
     * Get the revision ranges for the specified path.
     * @param path The merge source path.
     * @return List of SVNRevisionRange objects, or <code>null</code>.
     */
    public SVNRevisionRange[] getRevisions(String path);

    /**
     * Get the RevisionRange objects for the specified path
     * @param path The merge source path.
     * @return Array of RevisionRange objects, or <code>null</code>.
     */
    public SVNRevisionRange[] getRevisionRange(String path);

    /**
     * Parse the <code>svn:mergeinfo</code> property to populate the
     * merge source paths and revision ranges of this instance.
     * @param mergeInfo <code>svn:mergeinfo</code> property value.
     */
    public void loadFromMergeInfoProperty(String mergeInfo);

}
