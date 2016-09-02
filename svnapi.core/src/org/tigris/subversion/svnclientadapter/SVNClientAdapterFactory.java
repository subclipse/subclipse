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

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Factory for SVNClientAdapter. Real factories should extend this class and 
 * register themselves with the method #registerAdapterFactory 
 *
 * @author Cédric Chabanois 
 *         <a href="mailto:cchabanois@ifrance.com">cchabanois@ifrance.com</a>
 * 
 * @author Panagiotis Korros 
 *         <a href="mailto:pkorros@bigfoot.com">pkorros@bigfoot.com</a>
 * 
 */
public abstract class SVNClientAdapterFactory {
    
    private static Map ourFactoriesMap;
    
    // the first factory added is the preferred one
    private static SVNClientAdapterFactory preferredFactory; 
    
    /**
     * Real Factories should implement these methods.
     */
    protected abstract ISVNClientAdapter createSVNClientImpl();

    protected abstract String getClientType();
    
    /**
     * creates a new ISVNClientAdapter. You can create a javahl client or a command line
     * client.
     * 
     * @param clientType
     * @return the client adapter that was requested or null if that client adapter is not
     *         available or doesn't exist.
     */
    public static ISVNClientAdapter createSVNClient(String clientType) {
        if (ourFactoriesMap == null || !ourFactoriesMap.containsKey(clientType)) {
            return null;
        }
        SVNClientAdapterFactory factory = (SVNClientAdapterFactory) ourFactoriesMap.get(clientType);
        if (factory != null) {
            return factory.createSVNClientImpl();
        }
        return null;
    }

    /**
     * tells if the given clientType is available or not
     * 
     * @param clientType
     * @return true if the given clientType is available 
     */
    public static boolean isSVNClientAvailable(String clientType) {
        return ourFactoriesMap != null && ourFactoriesMap.containsKey(clientType);
    }

	/**
	 * @return the best svn client interface
	 * @throws SVNClientException
	 */
	public static String getPreferredSVNClientType() throws SVNClientException {
        if (preferredFactory != null) {
            return preferredFactory.getClientType();
        }
		throw new SVNClientException("No subversion client interface found.");
	}
    
    /**
     * Extenders should register themselves with this method. First registered factory
     * will be considered as the preferred one
     * 
     * @throws SVNClientException when factory with specified type is already registered.
     */
    protected static void registerAdapterFactory(SVNClientAdapterFactory factory) throws SVNClientException {
        if (factory == null) {
            return;
        }
        if (ourFactoriesMap == null) {
            ourFactoriesMap = new HashMap();
        }
        String type = factory.getClientType();
        if (!ourFactoriesMap.containsKey(type)) {
            ourFactoriesMap.put(type, factory);
            if (preferredFactory == null) {
                preferredFactory = factory;
            }
        } else {
            throw new SVNClientException("factory for type " + type + " already registered");
        }
    }

}
