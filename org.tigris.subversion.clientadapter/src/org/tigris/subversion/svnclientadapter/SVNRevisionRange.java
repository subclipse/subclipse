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

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Object that describes a revision range
 * 
 * copied from JavaHL implementation
 *
 */
public class SVNRevisionRange implements Comparable, java.io.Serializable
{
    // Update the serialVersionUID when there is a incompatible change
    // made to this class.  See any of the following, depending upon
    // the Java release.
    // http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/version.doc7.html
    // http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf
    // http://java.sun.com/j2se/1.5.0/docs/guide/serialization/spec/version.html#6678
    // http://java.sun.com/javase/6/docs/platform/serialization/spec/version.html#6678
    private static final long serialVersionUID = 1L;

    private SVNRevision from;
    private SVNRevision to;

     public SVNRevisionRange(SVNRevision from, SVNRevision to)
    {
        this.from = from;
        this.to = to;
    }

     public SVNRevisionRange(SVNRevision.Number from, SVNRevision.Number to, boolean convertToNMinusOne)
     {
    	 if (convertToNMinusOne) {
    		 this.from = new SVNRevision.Number(from.getNumber() - 1);
    	 } else
    		 this.from = from;
         this.to = to;
     }

    /**
     * Accepts a string in one of these forms: n m-n Parses the results into a
     * from and to revision
     * @param revisionElement revision range or single revision
     */
    public SVNRevisionRange(String revisionElement)
    {
        super();
        if (revisionElement == null)
        {
            return;
        }

        int hyphen = revisionElement.indexOf('-');
        if (hyphen > 0)
        {
            try
            {
                long fromRev = Long
                        .parseLong(revisionElement.substring(0, hyphen));
                long toRev = Long.parseLong(revisionElement
                        .substring(hyphen + 1));
                this.from = new SVNRevision.Number(fromRev);
                this.to = new SVNRevision.Number(toRev);
            }
            catch (NumberFormatException e)
            {
                return;
            }

        }
        else
        {
            try
            {
                long revNum = Long.parseLong(revisionElement.trim());
                this.from = new SVNRevision.Number(revNum);
                this.to = this.from;
            }
            catch (NumberFormatException e)
            {
                return;
            }
        }
    }

    public SVNRevision getFromRevision()
    {
        return from;
    }

    public SVNRevision getToRevision()
    {
        return to;
    }

    public String toString()
    {
        if (from != null && to != null)
        {
            if (from.equals(to))
                return from.toString();
            else
                return from.toString() + '-' + to.toString();
        }
        return super.toString();
    }

    public static Long getRevisionAsLong(SVNRevision rev)
    {
        long val = 0;
        if (rev != null && rev instanceof SVNRevision.Number)
        {
            val = ((SVNRevision.Number) rev).getNumber();
        }
        return new Long(val);
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((to == null) ? 0 : to.hashCode());
        return result;
    }

    /**
     * @param range The RevisionRange to compare this object to.
     */
    public boolean equals(Object range)
    {
        if (this == range)
            return true;
        if (!super.equals(range))
            return false;
        if (getClass() != range.getClass())
            return false;

        final SVNRevisionRange other = (SVNRevisionRange) range;

        if (from == null)
        {
            if (other.from != null)
                return false;
        }
        else if (!from.equals(other.from))
        {
            return false;
        }

        if (to == null)
        {
            if (other.to != null)
                return false;
        }
        else if (!to.equals(other.to))
        {
            return false;
        }

        return true;
    }

    /**
     * @param range The RevisionRange to compare this object to.
     */
    public int compareTo(Object range)
    {
        if (this == range)
            return 0;

        SVNRevision other = ((SVNRevisionRange) range).getFromRevision();
        return SVNRevisionRange.getRevisionAsLong(this.getFromRevision())
            .compareTo(SVNRevisionRange.getRevisionAsLong(other));
    }
    
    public static SVNRevisionRange[] getRevisions(SVNRevision.Number[] selectedRevisions, SVNRevision.Number[] allRevisions) {
    	Arrays.sort(selectedRevisions);
    	Arrays.sort(allRevisions);
    	ArrayList svnRevisionRanges = new ArrayList();
    	SVNRevision.Number fromRevision = null;
    	SVNRevision.Number toRevision = null; 
    	
    	int j = 0;
    	for (int i = 0; i < selectedRevisions.length; i++) {
    		if (fromRevision == null) {
    			fromRevision = selectedRevisions[i];
    			while (allRevisions[j++].getNumber() != selectedRevisions[i].getNumber()) {}
    		} else {
    			if (selectedRevisions[i].getNumber() != allRevisions[j++].getNumber()) {
    				SVNRevisionRange revisionRange = new SVNRevisionRange(fromRevision, toRevision, true);
    				svnRevisionRanges.add(revisionRange);
    				fromRevision = selectedRevisions[i];
    				while (allRevisions[j++].getNumber() != selectedRevisions[i].getNumber()) {}
    			}
    		}
    		toRevision = selectedRevisions[i];
    	}
    	if (toRevision != null) {
			SVNRevisionRange revisionRange = new SVNRevisionRange(fromRevision, toRevision, true);
			svnRevisionRanges.add(revisionRange);    		
		}    	
    	
    	SVNRevisionRange[] revisionRangeArray = new SVNRevisionRange[svnRevisionRanges.size()];
    	svnRevisionRanges.toArray(revisionRangeArray);
    	return revisionRangeArray;    	
    }
    
    /**
     * Returns boolean whether revision is contained in the range
     * @param revision
     * @param inclusiveFromRev - include an exact match of from revision
     * @return
     */
    public boolean contains(SVNRevision revision, boolean inclusiveFromRev) {
    	long fromRev = SVNRevisionRange.getRevisionAsLong(from).longValue();
    	long toRev = SVNRevisionRange.getRevisionAsLong(to).longValue();
    	long rev = SVNRevisionRange.getRevisionAsLong(revision).longValue();
    	if (inclusiveFromRev) {
	    	if (rev >= fromRev && (to.equals(SVNRevision.HEAD) || rev <= toRev))
	    		return true;
	    	else
	    		return false;
    	} else {
	    	if (rev > fromRev && (to.equals(SVNRevision.HEAD) || rev <= toRev))
	    		return true;
	    	else
	    		return false;
    	}
    }

	public String toMergeString() {
    	long fromRev = SVNRevisionRange.getRevisionAsLong(from).longValue();
    	long toRev = SVNRevisionRange.getRevisionAsLong(to).longValue();
    	if ((fromRev + 1) == toRev) {
    		return "-c " + toRev;
    	}
		return "-r " + fromRev + ":" + toRev;
	}
}
