/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.repo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.team.core.TeamException;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * The list of known repositories
 *
 */
public class SVNRepositories 
{
    private Map<String, ISVNRepositoryLocation> repositories = new HashMap<String, ISVNRepositoryLocation>();
    private static final String REPOSITORIES_STATE_FILE = ".svnProviderState"; //$NON-NLS-1$
    
    // version numbers for the state file 
    private static final int REPOSITORIES_STATE_FILE_VERSION_1 = 1;
    private static final int REPOSITORIES_STATE_FILE_VERSION_2 = 2;
    private static final int REPOSITORIES_STATE_FILE_VERSION_3 = 3;

    /*
     * Add the repository location to the cached locations
     */
    private void addToRepositoriesCache(ISVNRepositoryLocation repository) {
        repositories.put(repository.getLocation(), repository);
        SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().repositoryAdded(repository);
    }
    
    /*
     * Remove the repository location from the cached locations
     */
    private void removeFromRepositoriesCache(ISVNRepositoryLocation repository) {
        if (repositories.remove(repository.getLocation()) != null) {
            SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().repositoryRemoved(repository);
        }
    }

    /**
     * Add the repository to the receiver's list of known repositories or update it. 
     * Doing this will enable password caching accross platform invokations.
     */
    public void addOrUpdateRepository(ISVNRepositoryLocation repository) throws SVNException {
        // Check the cache for an equivalent instance and if there is one, just update the cache
        SVNRepositoryLocation existingLocation = (SVNRepositoryLocation)repositories.get(repository.getLocation());
        if (existingLocation != null) {
            SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().repositoryModified(repository);
            ((SVNRepositoryLocation)repository).updateCache();            
        } else {
            // Cache the password and register the repository location
            addToRepositoriesCache(repository);
            ((SVNRepositoryLocation)repository).updateCache();
        }
        saveState();
    }
    
    /**
     * Dispose of the repository location
     * 
     * Removes any cached information about the repository such as a remembered password.
     */
    public void disposeRepository(ISVNRepositoryLocation repository) throws SVNException {
        ((SVNRepositoryLocation)repository).dispose();
        removeFromRepositoriesCache(repository);
    }

    /** 
     * Return a list of the know repository locations
     */
    public ISVNRepositoryLocation[] getKnownRepositories(IProgressMonitor monitor) {
        IProgressMonitor progress = Policy.monitorFor(monitor);
    	IEclipsePreferences prefs = (IEclipsePreferences) SVNRepositoryLocation.getParentPreferences();
		try {
			String[] keys = prefs.childrenNames();
	        progress.beginTask(Policy.bind("SVNRepositories.refresh"), keys.length); //$NON-NLS-1$
			for (String key : keys) {
				progress.worked(1);
				try {
					IEclipsePreferences node = (IEclipsePreferences) prefs.node(key);
					String location = node.get(SVNRepositoryLocation.PREF_LOCATION, null);
					if (location != null && !exactMatchExists(location)) {
						ISVNRepositoryLocation repos = SVNRepositoryLocation.fromString(location);
						try {
							repos.validateConnection(new NullProgressMonitor());
						} catch(SVNException swallow){}
						addToRepositoriesCache(repos);
					} else {
						node.removeNode();
						prefs.flush();
					}
				} catch (SVNException e) {
					// Log and continue
					SVNProviderPlugin.log(e);
				}
			}
		} catch (BackingStoreException e) {
			// Log and continue (although all repos will be missing)
			SVNProviderPlugin.log(SVNException.wrapException(e)); 
		}
		progress.done();
		return (ISVNRepositoryLocation[])repositories.values().toArray(new ISVNRepositoryLocation[repositories.size()]);
    }

    public void refreshRepositoriesFolders(IProgressMonitor monitor) {
        ISVNRepositoryLocation[] repositories = getKnownRepositories(monitor);
        for (ISVNRepositoryLocation repository : repositories) {
            repository.refreshRootFolder();
        }
    }

    /**
     * Create a repository instance from the given properties.
     * The supported properties are:
     * 
     *   user The username for the connection (optional)
     *   password The password used for the connection (optional)
     *   url The url where the repository resides
     *   rootUrl The root url of the subversion repository (optional) 
     * 
     * The created instance is not known by the provider and it's user information is not cached.
     * The purpose of the created location is to allow connection validation before adding the
     * location to the provider.
     * 
     * This method will throw a SVNException if the location for the given configuration already
     * exists.
     */
    public ISVNRepositoryLocation createRepository(Properties configuration) throws SVNException {
        // Create a new repository location
        SVNRepositoryLocation location = SVNRepositoryLocation.fromProperties(configuration);
        
        // Check the cache for an equivalent instance and if there is one, throw an exception
        SVNRepositoryLocation existingLocation = (SVNRepositoryLocation)repositories.get(location.getLocation());
        if (existingLocation != null) {
            throw new SVNException(new SVNStatus(SVNStatus.ERROR, Policy.bind("SVNProvider.alreadyExists"))); //$NON-NLS-1$
        }

        return location;
    }
    
    public ISVNRepositoryLocation getRepository(String location) throws SVNException {
    	return getRepository(location, true);
    }

	/**
	 * Get the repository instance which matches the given String. 
	 * The format of the String is an url
	 */
	public ISVNRepositoryLocation getRepository(String location, boolean useRootUrl) throws SVNException {
		
		Set<String> keys = repositories.keySet();
		for(String url : keys){
			if (url.equals(location) || location.indexOf(url + "/") != -1){
			    return (ISVNRepositoryLocation) repositories.get(url);
			}    	
		}

		// If we haven't found a matching repository yet, check to see if the default
		// port is redundantly specified in the location.  If it is, check the known
		// repositories again to see if there is a match for the location with the 
		// default port stripped out (normalizedLocation).
		String normalizedLocation = getNormalizedLocation(location);
		if (!normalizedLocation.equals(location)) {
			for(String url : keys){
				if (url.equals(normalizedLocation) || normalizedLocation.indexOf(url + "/") != -1){
				    return (ISVNRepositoryLocation) repositories.get(url);
				}    	
			}			
		}
		
		//else we couldn't find it, fall through to adding new repo.
		
		ISVNRepositoryLocation repository = SVNRepositoryLocation.fromString(location, false, useRootUrl);
		addToRepositoriesCache(repository);
        
		return repository;
	}
    
	// If the default port is redundantly specified in the location, strip it out.
	private String getNormalizedLocation(String location) {
		try {
			URL url = new URL(location);
			if (url.getPort() == -1 || url.getDefaultPort() == -1 || url.getPort() != url.getDefaultPort())
				return location;
			url = new URL(url.getProtocol(), url.getHost(), -1, url.getPath());
			return url.toString();
		} catch (MalformedURLException e) {
			return location;
		}
	}

    /**
     * load the state of the plugin, ie the repositories locations 
     *
     */
    private void loadState() {
        try {
            IPath pluginStateLocation = SVNProviderPlugin.getPlugin().getStateLocation().append(REPOSITORIES_STATE_FILE);
            File file = pluginStateLocation.toFile();
            if (file.exists()) {
                try {
                    DataInputStream dis = new DataInputStream(new FileInputStream(file));
                    readState(dis);
                    dis.close();
                } catch (IOException e) {
                    throw new TeamException(new Status(Status.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNProvider.ioException"), e));  //$NON-NLS-1$
                }
            }
        } catch (TeamException e) {
            Util.logError(Policy.bind("SVNProvider.errorLoading"), e);//$NON-NLS-1$
        }
    }
    

    /**
     * Save the state of the plugin, ie the repositories locations 
     */
    private void saveState() {
        try {
            IPath pluginStateLocation = SVNProviderPlugin.getPlugin().getStateLocation();
            File tempFile = pluginStateLocation.append(REPOSITORIES_STATE_FILE + ".tmp").toFile(); //$NON-NLS-1$
            File stateFile = pluginStateLocation.append(REPOSITORIES_STATE_FILE).toFile();
            try {
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempFile));
                writeState(dos);
                dos.close();
                if (stateFile.exists()) {
                    stateFile.delete();
                }
                boolean renamed = tempFile.renameTo(stateFile);
                if (!renamed) {
                    throw new TeamException(new Status(Status.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNProvider.rename", tempFile.getAbsolutePath()), null)); //$NON-NLS-1$
                }
            } catch (IOException e) {
                throw new TeamException(new Status(Status.ERROR, SVNProviderPlugin.ID, TeamException.UNABLE, Policy.bind("SVNProvider.save",stateFile.getAbsolutePath()), e)); //$NON-NLS-1$
            }
        } catch (TeamException e) {
            Util.logError(Policy.bind("SVNProvider.errorSaving"), e);//$NON-NLS-1$
        }
    }
    
    /**
     * read the state of the plugin, ie the repositories locations
     * @param dis
     * @throws IOException
     * @throws SVNException
     */
    private void readState(DataInputStream dis) throws IOException, SVNException {
        int version = dis.readInt();
        
        if ((version < REPOSITORIES_STATE_FILE_VERSION_1) ||
           (version > REPOSITORIES_STATE_FILE_VERSION_3)) {
            Util.logError(Policy.bind("SVNProviderPlugin.unknownStateFileVersion", new Integer(version).toString()), null); //$NON-NLS-1$
            return;
        }
        
        int count = dis.readInt();
        for(int i = 0; i < count;i++){
        	ISVNRepositoryLocation root = SVNRepositoryLocation.fromString(dis.readUTF());
        	addToRepositoriesCache(root);
            if (version >= REPOSITORIES_STATE_FILE_VERSION_2) {
                String label = dis.readUTF();
                if (!label.equals("")) {
                    root.setLabel(label);
                }                
            }
            if (version >= REPOSITORIES_STATE_FILE_VERSION_3) {
                String repositoryRoot = dis.readUTF();
                if (!repositoryRoot.equals("")) {
                    root.setRepositoryRoot(new SVNUrl(repositoryRoot));
                }
            }
        }
    }
    
    /**
     * write the state of the plugin ie the repositories locations
     * @param dos
     * @throws IOException
     */
    private void writeState(DataOutputStream dos) throws IOException {
        // Write the repositories
        dos.writeInt(REPOSITORIES_STATE_FILE_VERSION_3);
        // Write out the repos
        Collection<ISVNRepositoryLocation> repos = repositories.values();
        dos.writeInt(repos.size());
        for (ISVNRepositoryLocation reposLocation : repos) {
            SVNRepositoryLocation root = (SVNRepositoryLocation)reposLocation;
            dos.writeUTF(root.getLocation());
            if (root.getLabel() == null) {
            	dos.writeUTF("");
            } else {
            	dos.writeUTF(root.getLabel());
            }
            if (root.getRepositoryRoot() == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(root.getRepositoryRoot().toString());
            }            
        }
		dos.flush();
		dos.close();
    }

    public void startup() {
        loadState();
    }

    public void shutdown() {
        saveState();
    }

	/**
	 * Answer whether the provided repository location is known by the provider or not.
	 * The location string corresponds to the Strin returned by ISVNRepositoryLocation#getLocation()
	 */
	public boolean isKnownRepository(String location, boolean requireExactMatch) {
		Set<String> keys = repositories.keySet();
		for(String checkLocation : keys){
			if(!requireExactMatch && location.indexOf(checkLocation)!=-1){
				return true;
			}
    		if (location.equals(checkLocation)) return true;
		}
		return false;
	}

	/**
	 * Answer whether the provided repository location already has an exact match location
	 * The location string corresponds to the Strin returned by ISVNRepositoryLocation#getLocation()
	 */
	public boolean exactMatchExists(String location) {
		Set<String> keys = repositories.keySet();
		for(String url : keys){
			if (url.equals(location)){
				return true;
			}
    		
		}
		return false;
	}

}
