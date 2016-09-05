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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.tigris.subversion.svnclientadapter.utils.StringUtils;

/**
 * We could have used URL, using custom protocols (svn, svn+ssl) 
 * (@see http://developer.java.sun.com/developer/onlineTraining/protocolhandlers/)
 * but this is not really necessary as we don't want to open a connection 
 * directly with this class.
 * We just want a string which represent a SVN url which can be used with our JNI
 * methods.
 *
 * An SVNUrl is immutable. 
 *
 * @author C�dric Chabanois 
 *         <a href="mailto:cchabanois@ifrance.com">cchabanois@ifrance.com</a>
 *
 */
public class SVNUrl {
	
	private static final String SVN_PROTOCOL = "svn";
	private static final String SVNSSH_PROTOCOL = "svn+";
	private static final String HTTP_PROTOCOL = "http";
	private static final String HTTPS_PROTOCOL = "https";
	private static final String FILE_PROTOCOL = "file";
	
	protected static final char SEGMENT_SEPARATOR = '/'; 
	
    private String protocol; // http, file, svn or svn+ssh
    private String[] segments;
    private String host;
    private int port;

    /**
     * Constructor
     * @param svnUrl a string to parse url from
     * @throws MalformedURLException when parsing failed
     */
    public SVNUrl(String svnUrl) throws MalformedURLException {
        if(svnUrl == null)
            throw new MalformedURLException("Svn url cannot be null. Is this a versioned resource?");
   
        parseUrl(svnUrl.trim());
    }
    
    private SVNUrl(String protocol, String host, int port, String[] segments)
    {
    	super();
    	this.protocol = protocol;
    	this.host = host;
    	this.port = port;
    	this.segments = segments;
    }

    /**
     * Asnwer a new SVNUrl with added segments
     * @param path a String of path segment(s) to ba appended to receiver
     * @return new SVNUrl 
     */
    public SVNUrl appendPath(String path)
    {
    	String[] segmentsToAdd = StringUtils.split(path, SEGMENT_SEPARATOR);
    	//Skip the starting slash
    	if ((segmentsToAdd.length > 0) && (segmentsToAdd[0].equals("")))
    	{
    		String[] newSegmentsToAdd = new String[segmentsToAdd.length - 1];
    		System.arraycopy(segmentsToAdd, 1, newSegmentsToAdd, 0, segmentsToAdd.length - 1);
    		segmentsToAdd = newSegmentsToAdd;    		
    	}
    	
    	String[] newSegments = new String[segments.length + segmentsToAdd.length];
    	System.arraycopy(segments, 0, newSegments, 0, segments.length);
   		System.arraycopy(segmentsToAdd, 0, newSegments, segments.length, segmentsToAdd.length);
    	return new SVNUrl(this.protocol, this.host, this.port, newSegments);
    }
    
    /**
     * verifies that the url is correct
     * @throws MalformedURLException
     */
    private void parseUrl(String svnUrl) throws MalformedURLException{
        String parsed = svnUrl;

        // SVNUrl have this format :
        // scheme://host[:port]/path
        
        // parse protocol
        int i = parsed.indexOf("://");
        if (i == -1)
            throw new MalformedURLException("Invalid svn url: "+svnUrl);
        protocol = parsed.substring(0,i).toLowerCase();
        if ((!protocol.equalsIgnoreCase(HTTP_PROTOCOL)) &&
            (!protocol.equalsIgnoreCase(HTTPS_PROTOCOL)) &&
            (!protocol.equalsIgnoreCase(FILE_PROTOCOL)) &&
            (!protocol.equalsIgnoreCase(SVN_PROTOCOL)) &&
            (!protocol.startsWith(SVNSSH_PROTOCOL)) ) {
            throw new MalformedURLException("Invalid svn url: "+svnUrl);
        }
        parsed = parsed.substring(i+3);
		if (parsed.length() == 0) {
			throw new MalformedURLException("Invalid svn url: "+svnUrl);
		}

        // parse host & port        
        i = parsed.indexOf("/");
        if (i == -1) {
            i = parsed.length();
        }
        if (!protocol.equalsIgnoreCase(FILE_PROTOCOL)) {
	        String hostPort = parsed.substring(0,i).toLowerCase();
	        String[] hostportArray = StringUtils.split(hostPort,':');
	        if (hostportArray.length == 0) {
	        	throw new MalformedURLException("Invalid svn url: "+svnUrl);                    
	        } else if (hostportArray.length == 2) {
	            this.host = hostportArray[0];
	            try {
	                this.port = Integer.parseInt(hostportArray[1]);
	            } catch (NumberFormatException e) {
	                throw new MalformedURLException("Invalid svn url: "+svnUrl);
	            }
	        } else {
	            this.host = hostportArray[0];
	            this.port = getDefaultPort(protocol);
	        }
        } else {
            this.port = -1;
            // parse path
            if (i == 0) {
                this.host = "";
            } else {
                this.host = parsed.substring(0,i);
            }
        }
        // parse path
        if (i < parsed.length()) {
            parsed = parsed.substring(i+1);
        } else {
            parsed = "";
        }
        segments = StringUtils.split(parsed,'/');
    }

    /**
     * get the default port for given protocol
     * @param protocol
     * @return port number or -1 if protocol is unknown
     */
    public static int getDefaultPort(String protocol) {
        int port = -1;
        if (SVN_PROTOCOL.equals(protocol)) {
            port = 3690;
        } else if (HTTP_PROTOCOL.equals(protocol)) {
            port = 80;
        } else if (HTTPS_PROTOCOL.equals(protocol)) {
            port = 443;
        } else if (protocol != null && protocol.startsWith(SVNSSH_PROTOCOL)) {
            port = 22;
        }
        return port;
    }
    
    /**
     * Get the url as String. The url returned never ends with "/"
     * @return String representation of this url instance
     */
    private String get() {
    	//Be sofisticated and compute the StringBuffer size up-front. 
    	StringBuffer buffer = new StringBuffer(calculateUrlLength());
        buffer.append(getProtocol());
        buffer.append("://");
        buffer.append(getHost()); 
        if (getPort() != getDefaultPort(getProtocol())) {
        	buffer.append(":");
        	buffer.append(getPort());
        }

        for (int i = 0; i < segments.length;i++) {
        	buffer.append(SEGMENT_SEPARATOR);
        	buffer.append(segments[i]);
        }
        return buffer.toString(); 
    }
    
    private int calculateUrlLength()
    {
    	int result = 3; // Size of "://"
    	if (getProtocol() != null) result += getProtocol().length();
    	if (getHost() != null) result += getHost().length();
        if (getPort() != getDefaultPort(getProtocol())) {
        	result++; //Add one for ":"
        	result += String.valueOf(getPort()).length();
        }
        for (int i = 0; i < segments.length;i++) {
            result++; // Add 1 for separator
            result += segments[i].length();
        }
        return result;
    }
    
    /**
     * get the protocol
     * @return either http, https, file, svn or svn+ssh
     */
    public String getProtocol() {
        return protocol;
    }
    
    /**
     * @return Returns the host.
     */
    public String getHost() {
        return host;
    }
    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * get the path of the url. 
     * @return an arrray of url path segments
     */
    public String[] getPathSegments() {
    	return segments;
    }
    
    /**
     * @return the "file" name, i.e. the element after last /
     */
    public String getLastPathSegment() {
        if (segments.length == 0)
            return "";
    	return segments[segments.length-1];
    }
    
    /**
     * Return new SVNUrl which represents parent of the receiver 
     * @return the parent url or null if no parent
     */
    public SVNUrl getParent() {
    	if ((segments.length == 0) ||
    		((segments.length == 1) && ((host == null) || (host.length() == 0))))
    	{
    		return null;
    	}
    	String[] parentSegments = new String[segments.length - 1];
    	System.arraycopy(segments, 0, parentSegments, 0, segments.length - 1);
    	return new SVNUrl(this.protocol, this.host, this.port, parentSegments);
    }
    
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object target) {
	    // this method is not very accurate because :
	    // url before repository is not always case sensitive
		if (this == target)
			return true;
		if (!(target instanceof SVNUrl))
			return false;
		SVNUrl url = (SVNUrl) target;
		return get().equals(url.get());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return get().hashCode();
	}
	
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	// The URI class will throw Exception if there are spaces in the URL, but it seems
    	// to handle other classes OK.  I tested with @ + and Unicode characters.  It leaves
    	// the @ and + alone and converts Unicode to %nn.  It is possible there are other
    	// characters we need to replace here besides space.
    	String s = get().replace(" ", "%20").replace("[", "%5B").replace("]","%5D");
		try {
			URI u = new URI(s);
			return u.toASCIIString();
		} catch (URISyntaxException e) {
			return s;
		}
    }
}
