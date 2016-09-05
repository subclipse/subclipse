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


public class SVNCopySource {
    /**
     * The source path or URL.
     */
    private String path;

    /**
     * The source revision.
     */
    private SVNRevision revision;

    /**
     * The peg revision.
     */
    private SVNRevision pegRevision;

    /**
     * Create a new instance.
     *
     * @param path
     * @param revision The source revision.
     * @param pegRevision The peg revision.  Typically interpreted as
     * {@link org.tigris.subversion.javahl.SVNRevision#HEAD} when
     * <code>null</code>.
     */
    public SVNCopySource(String path, SVNRevision revision, SVNRevision pegRevision)
    {
        this.path = path;
        this.revision = revision;
        this.pegRevision = pegRevision;
    }

    /**
     * @return The source path or URL.
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * @return The source revision.
     */
    public SVNRevision getRevision()
    {
        return this.revision;
    }

    /**
     * @return The peg revision.
     */
    public SVNRevision getPegRevision()
    {
        return this.pegRevision;
    }

}
