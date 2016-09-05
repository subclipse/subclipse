/*******************************************************************************
 * Copyright (c) 2003, 2006 svnClientAdapter project and others.
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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * tells which keywords are enabled for a given resource
 */
public class SVNKeywords {
    
    public static final String LAST_CHANGED_DATE = "LastChangedDate";
    public static final String DATE = "Date";
    
    public static final String LAST_CHANGED_REVISION = "LastChangedRevision";
    public static final String REV = "Rev";
    
    public static final String LAST_CHANGED_BY = "LastChangedBy";
    public static final String AUTHOR = "Author";
    
    public static final String HEAD_URL = "HeadURL";
    public static final String URL = "URL";
    
    public static final String ID = "Id";
    
    private boolean lastChangedDate = false;
    private boolean lastChangedRevision = false;
    private boolean lastChangedBy = false;
    private boolean headUrl = false;
    private boolean id = false;

    public SVNKeywords() {
        
    }

    public SVNKeywords(String keywords) {
        if (keywords == null)
            return;
        StringTokenizer st = new StringTokenizer(keywords," ");

        while (st.hasMoreTokens()) {
            String keyword = st.nextToken();
            // don't know if keywords are case sensitive or no
            if ((keyword.equals(SVNKeywords.HEAD_URL)) ||
                (keyword.equals(SVNKeywords.URL)))
                headUrl = true;
            else
            if (keyword.equals(SVNKeywords.ID))
                id = true;
            else
            if ((keyword.equals(SVNKeywords.LAST_CHANGED_BY)) ||
                (keyword.equals(SVNKeywords.AUTHOR)))
                lastChangedBy = true;
            else
            if ((keyword.equals(SVNKeywords.LAST_CHANGED_DATE)) ||
                (keyword.equals(SVNKeywords.DATE)))
                lastChangedDate = true;
            else
            if ((keyword.equals(SVNKeywords.LAST_CHANGED_REVISION)) ||
                (keyword.equals(SVNKeywords.REV)))
                lastChangedRevision = true;
        }
        
    }
    

    public SVNKeywords(
        boolean lastChangedDate, boolean lastChangedRevision,
        boolean lastChangedBy, boolean headUrl, boolean id) {
    
        this.lastChangedDate = lastChangedDate;
        this.lastChangedRevision = lastChangedRevision;
        this.lastChangedBy = lastChangedBy;
        this.headUrl = headUrl;
        this.id = id;        
    }
    
	public boolean isHeadUrl() {
		return headUrl;
	}

	public boolean isId() {
		return id;
	}

	public boolean isLastChangedBy() {
		return lastChangedBy;
	}

	public boolean isLastChangedDate() {
		return lastChangedDate;
	}

	public boolean isLastChangedRevision() {
		return lastChangedRevision;
	}

    /**
     * 
     * @return the list of keywords
     */
    public List getKeywordsList() {
        ArrayList list = new ArrayList();
        if (headUrl)
            list.add(HEAD_URL);
        if (id)
            list.add(ID);
        if (lastChangedBy)  
            list.add(LAST_CHANGED_BY);
        if (lastChangedDate)
            list.add(LAST_CHANGED_DATE);
        if (lastChangedRevision)
            list.add(LAST_CHANGED_REVISION); 
        return list;
    }

    public String toString()
    {
        String result = "";
        
        for (Iterator it = getKeywordsList().iterator(); it.hasNext();) {
            String keyword = (String) it.next();
            result += keyword;
            if (it.hasNext())
                result += ' '; 
        }
        return result;
    }

	public void setHeadUrl(boolean b) {
		headUrl = b;
	}

	public void setId(boolean b) {
		id = b;
	}

	public void setLastChangedBy(boolean b) {
		lastChangedBy = b;
	}

	public void setLastChangedDate(boolean b) {
		lastChangedDate = b;
	}

	public void setLastChangedRevision(boolean b) {
		lastChangedRevision = b;
	}

}
