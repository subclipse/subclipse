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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * A JavaHL based implementation of {@link ISVNProperty}.
 * Actually just an adapter from {@link org.tigris.subversion.javahl.PropertyData}
 * 
 * @author philip schatz
 */
public class JhlPropertyData implements ISVNProperty
{
    private String key;
    private byte[] data;
    private String path;
    private boolean isForUrl;
    
    /**
     * Factory method for properties on local resource (file or dir)
     * @param propertyData
     * @return a JhlPropertyData constructed from supplied propertyData
     */
    public static JhlPropertyData newForFile(String path, String key, byte[] data)
    {
    	return new JhlPropertyData(path, key, data, false);
    }

    /**
     * Factory method for properties on remote resource (url)
     * @param propertyData
     * @return a JhlPropertyData constructed from supplied propertyData
     */
    public static JhlPropertyData newForUrl(String path, String key, byte[] data)
    {
    	return new JhlPropertyData(path, key, data, true);
    }

    /**
     * Constructor
     * @param propertyData
     */
    private JhlPropertyData(String path, String key, byte[] data, boolean isForUrl)
    {
    	this.path = path;
        this.key = key;
        this.data = data;
        this.isForUrl = isForUrl;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNProperty#getName()
     */
    public String getName()
    {
        return key;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNProperty#getValue()
     */
    public String getValue()
    {
        try {
			return new String(data, "UTF8");
		} catch (UnsupportedEncodingException e) {
			return new String(data);
		}
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNProperty#getFile()
     */
    public File getFile()
    {
    	return isForUrl ? null : new File(path).getAbsoluteFile();
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNProperty#getUrl()
     */
    public SVNUrl getUrl()
    {
		try {
	    	return isForUrl ? new SVNUrl(path) : null;
        } catch (MalformedURLException e) {
            //should never happen.
            return null;
        }
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNProperty#getData()
     */
    public byte[] getData()
    {
        return data;
    }
}
