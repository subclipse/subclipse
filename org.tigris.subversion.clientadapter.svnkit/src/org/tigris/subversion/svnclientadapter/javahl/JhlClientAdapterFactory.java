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
package org.tigris.subversion.svnclientadapter.javahl;

import org.apache.subversion.javahl.ISVNClient;
import org.apache.subversion.javahl.SVNClient;
import org.apache.subversion.javahl.types.Version;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Concrete implementation of SVNClientAdapterFactory for javahl interface.
 * To register this factory, just call {@link JhlClientAdapterFactory#setup()} 
 */
public class JhlClientAdapterFactory extends SVNClientAdapterFactory {
    
    private static final String[] WINDOWSLIBS = new String[] {
    	"msvcr100", "msvcp100",
        "libapr-1", "libapriconv-1", "libeay32", "ssleay32", "libaprutil-1",
        "dbghelp", "libsasl",
        // libraries as of 1.5
        "libsvn_subr-1", "libsvn_delta-1", "libsvn_diff-1", "libsvn_wc-1",
        "libsvn_fs-1", "libsvn_repos-1", "libsvn_ra-1", "libsvn_client-1"
    };

    private static boolean availabilityCached = false;
    private static boolean available;
	private static StringBuffer javaHLErrors = new StringBuffer("Failed to load JavaHL Library.\nThese are the errors that were encountered:\n");
	
	/** Client adapter implementation identifier */
    public static final String JAVAHL_CLIENT = "javahl";

	/**
	 * Private constructor.
	 * Clients are expected the use {@link #createSVNClientImpl()}, res.
	 * ask the {@link SVNClientAdapterFactory}
	 */
    private JhlClientAdapterFactory() {
    	super();
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory#createSVNClientImpl()
	 */
	protected ISVNClientAdapter createSVNClientImpl() {
		return new JhlClientAdapter();
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory#getClientType()
     */
    protected String getClientType() {
        return JAVAHL_CLIENT;
    }
    
    /**
     * Setup the client adapter implementation and register it in the adapters factory
     * @throws SVNClientException
     */
    public static void setup() throws SVNClientException {
        if (!isAvailable()) {
        	throw new SVNClientException("Javahl client adapter is not available");
        }
        
    	SVNClientAdapterFactory.registerAdapterFactory(new JhlClientAdapterFactory());
    }
    
    @SuppressWarnings("rawtypes")
	public static boolean isAvailable() {
    	if (!availabilityCached) {
			Class c = null;
    		try {
    			// load a JavaHL class to see if it is found.  Do not use SVNClient as
    			// it will try to load native libraries and we do not want that yet
    			c = Class.forName("org.apache.subversion.javahl.ClientException");
    			if (c == null)
    				return false;
    		} catch (Throwable t) {
    			availabilityCached = true;
    			return false;
    		}
    		// if library is already loaded, it will not be reloaded

    		//workaround to solve Subclipse ISSUE #83
    		// we will ignore these exceptions to handle scenarios where
    		// javaHL was built diffently.  Ultimately, if javaHL fails to load
    		// because of a problem in one of these libraries the proper behavior
    		// will still occur -- meaning JavaHL adapter is disabled.
    		if(isOsWindows()) {
    		    
    		    for (int i = 0; i < WINDOWSLIBS.length; i++) {
    		        try {
    		            System.loadLibrary(WINDOWSLIBS[i]);
    		        } catch (Exception e) {
                        javaHLErrors.append(e.getMessage()).append("\n");
    		        } catch (UnsatisfiedLinkError e) {
                        javaHLErrors.append(e.getMessage()).append("\n");
    		        }
    		    }
    		    
    		}
    		//workaround to solve Subclipse ISSUE #83
    		available = false;
    		try {
    			/*
    			 * see if the user has specified the fully qualified path to the native
    			 * library
    			 */
    			try
    			{
    				String specifiedLibraryName =
    					System.getProperty("subversion.native.library");
    				if(specifiedLibraryName != null) {
    					System.load(specifiedLibraryName);
    					available = true;
    				}
    			}
    			catch(UnsatisfiedLinkError ex)
    			{
    				javaHLErrors.append(ex.getMessage()).append("\n");
    			}
    			if (!available) {
    				/*
    				 * first try to load the library by the new name.
    				 * if that fails, try to load the library by the old name.
    				 */
    				try
    				{
    					System.loadLibrary("libsvnjavahl-1");
    				}
    				catch(UnsatisfiedLinkError ex)
    				{
    					javaHLErrors.append(ex.getMessage() + "\n");
    					try
    					{
    						System.loadLibrary("svnjavahl-1");
    					}
    					catch (UnsatisfiedLinkError e)
    					{
    						javaHLErrors.append(e.getMessage()).append("\n");
    						System.loadLibrary("svnjavahl");
    					}
    				}

    				available = true;
    			}
    		} catch (Exception e) {
    			available = false;
    			javaHLErrors.append(e.getMessage()).append("\n");
    		} catch (UnsatisfiedLinkError e) {
    			available = false;
    			javaHLErrors.append(e.getMessage()).append("\n");
    		} finally {
    			availabilityCached = true;
    		}
    		if (!available) {
    			String libraryPath = System.getProperty("java.library.path");
    			if (libraryPath != null)
    				javaHLErrors.append("java.library.path = " + libraryPath);
    			// System.out.println(javaHLErrors.toString());
    		} else {
    			// At this point, the library appears to be available, but
    			// it could be too old version of JavaHL library.  We have to try
    			// to get the version of the library to be sure.
    			try {
	                ISVNClient svnClient = new SVNClient();
    				Version version = svnClient.getVersion();
    				if (version.getMajor() == 1 && version.getMinor() == 8)
    					available = true;
    				else {
    					available = false;
    					javaHLErrors = new StringBuffer("Incompatible JavaHL library loaded.  Subversion 1.8.x required.");
    				}
    			} catch (UnsatisfiedLinkError e) {
    				available = false;
    				javaHLErrors = new StringBuffer("Incompatible JavaHL library loaded.  1.8.x or later required.");
    			}
    		}
    	}

    	return available;
    }
    
	/**
	 * Answer whether running on Windows OS.
	 * (Actual code extracted from org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS)
	 * (For such one simple method it does make sense to introduce dependency on whole commons-lang.jar)
	 * @return true when the underlying 
	 */
	public static boolean isOsWindows()
	{
        try {
            return System.getProperty("os.name").startsWith("Windows");
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            return false;
        }
	}
	
    /**
     * @return an error string describing problems during loading platform native libraries (if any)
     */
    public static String getLibraryLoadErrors() {
        if (isAvailable())
            return "";
        else
            return javaHLErrors.toString();
    }

}
