/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eugene Kuleshov - initial API and implementation
 ******************************************************************************/

package org.tigris.subversion.subclipse.mylyn;

import java.util.Iterator;
import java.util.List;

import org.eclipse.mylyn.tasks.core.IRepositoryManager;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Eugene Kuleshov
 */
public class SubclipseTeamPlugin extends AbstractUIPlugin implements IStartup {

	public static final String PLUGIN_ID = "org.tigris.subversion.subclipse.mylyn";

	private static SubclipseTeamPlugin plugin;
	
	public SubclipseTeamPlugin() {
	}

  public void earlyStartup() {
    // all done in start
  }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
    super.stop(context);
	}

	public static TaskRepository getRepository(String url, IRepositoryManager repositoryManager) {
	  if(url!=null) {
      List repositories = repositoryManager.getAllRepositories();
      for (Iterator it = repositories.iterator(); it.hasNext();) {
        TaskRepository repository = (TaskRepository) it.next();
        if (url.startsWith(repository.getUrl())) {
          return repository;
        }
      }
	  }
    return null;
  }

  /**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SubclipseTeamPlugin getDefault() {
		return plugin;
	}

}

