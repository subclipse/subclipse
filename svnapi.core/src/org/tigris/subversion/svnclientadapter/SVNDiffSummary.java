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

import java.util.EventObject;

/**
 * The event passed to the {@link
 * DiffSummarizer.summarize(SVNDiffSummary)} API in response to path
 * differences reported by {@link ISVNClientAdapter#diffSummarize}.
 *
 */
public class SVNDiffSummary extends EventObject
{
    // Update the serialVersionUID when there is a incompatible change
    // made to this class.  See any of the following, depending upon
    // the Java release.
    // http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/version.doc7.html
    // http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf
    // http://java.sun.com/j2se/1.5.0/docs/guide/serialization/spec/version.html#6678
    // http://java.sun.com/javase/6/docs/platform/serialization/spec/version.html#6678
    private static final long serialVersionUID = 1L;

    private SVNDiffKind diffKind;
    private boolean propsChanged;
    private int nodeKind;

    /**
     * This constructor is to be used by the native code.
     *
     * @param path The path we have a diff for.
     * @param diffKind The kind of diff this describes.
     * @param propChanged Whether any properties have changed.
     * @param nodeKind The type of node which changed (corresponds to
     * the {@link SVNNodeKind} enumeration).
     */
    public SVNDiffSummary(String path, SVNDiffKind diffKind, boolean propsChanged,
                int nodeKind)
    {
        super(path);
        this.diffKind = diffKind;
        this.propsChanged = propsChanged;
        this.nodeKind = nodeKind;
    }

    /**
     * @return The path we have a diff for.
     */
    public String getPath()
    {
        return (String) super.source;
    }

    /**
     * @return The kind of summary this describes.
     */
    public SVNDiffKind getDiffKind()
    {
        return this.diffKind;
    }

    /**
     * @return Whether any properties have changed.
     */
    public boolean propsChanged()
    {
        return this.propsChanged;
    }

    /**
     * @return The type of node which changed (corresponds to the
     * {@link NodeKind} enumeration).
     */
    public int getNodeKind()
    {
        return this.nodeKind;
    }

    /**
     * @return The path.
     */
    public String toString()
    {
        return getPath();
    }

    /**
     * The type of difference being summarized.
     */
    public static class SVNDiffKind
    {
        // Corresponds to the svn_client_diff_summarize_kind_t enum.
        public static SVNDiffKind NORMAL = new SVNDiffKind(0);
        public static SVNDiffKind ADDED = new SVNDiffKind(1);
        public static SVNDiffKind MODIFIED = new SVNDiffKind(2);
        public static SVNDiffKind DELETED = new SVNDiffKind(3);

        private int kind;

        private SVNDiffKind(int kind)
        {
            this.kind = kind;
        }

        /**
         * @return The appropriate instance.
         * @throws IllegalArgumentException If the diff kind is not
         * recognized.
         */
        public static SVNDiffKind getInstance(int diffKind)
            throws IllegalArgumentException
        {
            switch (diffKind)
            {
            case 0:
                return NORMAL;
            case 1:
                return ADDED;
            case 2:
                return MODIFIED;
            case 3:
                return DELETED;
            default:
                throw new IllegalArgumentException("Diff kind " + diffKind +
                                                   " not recognized");
            }
        }

        /**
         * @param diffKind A DiffKind for comparison.
         * @return Whether both DiffKinds are of the same type.
         */
        public boolean equals(Object diffKind)
        {
            return (((SVNDiffKind) diffKind).kind == this.kind);
        }

        public int hashCode()
        {
            // Equivalent to new Integer(this.kind).hashCode().
            return this.kind;
        }

        /**
         * @return A textual representation of the type of diff.
         */
        public String toString()
        {
            switch (this.kind)
            {
            case 0:
                return "normal";
            case 1:
                return "added";
            case 2:
                return "modified";
            case 3:
                return "deleted";
            default:
                return "unknown";
            }
        }
    }

}
