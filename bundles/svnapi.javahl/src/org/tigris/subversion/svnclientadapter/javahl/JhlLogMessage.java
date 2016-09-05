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
package org.tigris.subversion.svnclientadapter.javahl;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.subversion.javahl.types.ChangePath;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * A JavaHL based implementation of {@link ISVNLogMessage}.
 * Actually just an adapter from {@link org.tigris.subversion.javahl.LogMessage}
 *  
 * @author philip schatz
 */
public class JhlLogMessage implements ISVNLogMessage {

	private static final String EMPTY = "";
	
	private List<ISVNLogMessage> children;
	private boolean hasChildren;
	private ISVNLogMessageChangePath[] changedPaths;
	private SVNRevision.Number revision;
	private Map<String, byte[]> revprops;

    private static final DateFormat formatter = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS z");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private long timeMicros;
    private Calendar cachedDate;

	public JhlLogMessage(Set<ChangePath> changedPaths, long revision,
			Map<String, byte[]> revprops, boolean hasChildren) {
		this.changedPaths = JhlConverter.convertChangePaths(changedPaths);
		this.revision = new SVNRevision.Number(revision);
		this.revprops = revprops;
		if (this.revprops == null) {
			this.revprops = new HashMap<String, byte[]>(2); // avoid NullPointerErrors
			this.revprops.put(AUTHOR, EMPTY.getBytes());
			this.revprops.put(MESSAGE, EMPTY.getBytes());		
			this.revprops.put(DATE, EMPTY.getBytes());
		}
		this.hasChildren = hasChildren;
		try {
			String datestr = new String(this.revprops.get(DATE));
			if (datestr != null && datestr.length() == 27 && datestr.charAt(26) == 'Z') {
				Date date;
				synchronized (formatter) {
					date = formatter.parse(datestr.substring(0, 23) + " UTC");
				}
		        cachedDate = Calendar.getInstance(UTC);
		        cachedDate.setTime(date);
		        timeMicros = cachedDate.getTimeInMillis() * 1000
		                        + Integer.parseInt(datestr.substring(23, 26));
			}
		} catch (Exception e) {}
	}

	public void addChild(ISVNLogMessage msg) {
		if (children == null)
			children = new ArrayList<ISVNLogMessage>();
		children.add(msg);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNLogMessage#getRevision()
	 */
	public SVNRevision.Number getRevision() {
		return revision;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNLogMessage#getAuthor()
	 */
	public String getAuthor() {
		byte[] author = revprops.get(AUTHOR);
		if (author == null) {
			return "";
		}
		else {
			try {
				return new String(author, "UTF8");
			} catch (UnsupportedEncodingException e) {
				return new String(author);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNLogMessage#getDate()
	 */
	public Date getDate() {
		if (cachedDate == null) {
			return new Date(0L);
		}
		else {
			return cachedDate.getTime();
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNLogMessage#getMessage()
	 */
	public String getMessage() {
		byte[] message = revprops.get(MESSAGE);
		if (message == null) {
			return "";
		}
		else {
			try {
				return new String(message, "UTF8");
			} catch (UnsupportedEncodingException e) {
				return new String(message);
			}
		}
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLogMessage#getChangedPaths()
     */
    public ISVNLogMessageChangePath[] getChangedPaths() {
    	return changedPaths;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getMessage();
    }

	public ISVNLogMessage[] getChildMessages() {
		if (hasChildren && children != null) {
			ISVNLogMessage[] childArray = new JhlLogMessage[children.size()];
			children.toArray(childArray);
			return childArray;
		} else
			return null;
	}

	public long getNumberOfChildren() {
		if (hasChildren && children != null)
			return children.size();
		else
			return 0L;
	}

	public long getTimeMillis() {
		if (cachedDate == null) {
			return 0L;
		}
		else {
			return cachedDate.getTimeInMillis();
		}
	}
	
	public long getTimeMicros() {
		return timeMicros;
	}

	public boolean hasChildren() {
		return hasChildren;
	}

}
