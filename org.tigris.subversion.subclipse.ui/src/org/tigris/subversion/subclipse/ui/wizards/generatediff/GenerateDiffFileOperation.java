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
package org.tigris.subversion.subclipse.ui.wizards.generatediff;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * An operation to run the SVN diff operation on a set of resources. The result
 * of the diff is written to a file. If there are no differences found, the user
 * is notified and the output file is not created.
 */
public class GenerateDiffFileOperation implements IRunnableWithProgress {
	private static String ECLIPSE_PATCH_HEADER = "### Eclipse Workspace Patch 1.0"; //$NON-NLS-1$
	private static String ECLIPSE_PROJECT_MARKER = "#P "; //$NON-NLS-1$
	private static String EOL = System.getProperty("line.separator");
	
	private File outputFile;
	private IResource[] resources;
	private IResource[] unaddedResources;
	private Shell shell;
	private boolean recursive;
	private boolean toClipboard;
	private ArrayList newFiles;
	private IResource[] selectedResources;
	private final boolean eclipseFormat;
	private final boolean projectRelative;

	GenerateDiffFileOperation(IResource[] resources, IResource[] unaddedResources, File file, boolean toClipboard, boolean recursive, boolean eclipseFormat, boolean projectRelative, Shell shell) {
		this.resources = resources;
		this.unaddedResources = unaddedResources;
		this.outputFile = file;
		this.eclipseFormat = eclipseFormat;
		this.projectRelative = projectRelative;
		this.shell = shell;
        this.recursive = recursive;
		this.toClipboard = toClipboard;
	}

	
	/**
	 * Convenience method that maps the given resources to their providers.
	 * The returned Hashtable has keys which are ITeamProviders, and values
	 * which are Lists of IResources that are shared with that provider.
	 * 
	 * @return a hashtable mapping providers to their resources
	 */
	protected Hashtable getProviderMapping(IResource[] resources) {
		Hashtable result = new Hashtable();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}	
	/**
	 * @see IRunnableWithProgress#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		ISVNClientAdapter svnClient = null;
		ISVNRepositoryLocation repository = null;
		try {
			monitor.beginTask("", 500); //$NON-NLS-1$
			monitor.setTaskName(
				Policy.bind("GenerateSVNDiff.working")); //$NON-NLS-1$
			
			OutputStream os;
			if(toClipboard) {
				os = new ByteArrayOutputStream();
			} else {
				os = new FileOutputStream(outputFile);
			}
            File tmpFile = File.createTempFile("sub",""); //$NON-NLS-1$ //$NON-NLS-2$
            tmpFile.deleteOnExit();

            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);

             newFiles = new ArrayList();
			 if(unaddedResources.length > 0)
			 {
//					Display.getDefault().syncExec(new Runnable() {
//						 public void run() {
//							 DiffNewFilesDialog dialog = new DiffNewFilesDialog(shell,unaddedResources);
//							 	boolean revert = (dialog.open() == RevertDialog.OK);
//								if (revert) {
//									newFiles.addAll(Arrays.asList(dialog.getSelectedResources()));
//								}
//							 }
//					});
				    for (int i = 0; i < unaddedResources.length; i++)
				    	newFiles.add(unaddedResources[i]);
					if(newFiles.size() > 0)
					{
						try {
							// associate the resources with their respective RepositoryProvider					
							Hashtable table = getProviderMapping((IResource[])newFiles.toArray(new IResource[newFiles.size()]));
							Set keySet = table.keySet();
							Iterator iterator = keySet.iterator();
							while (iterator.hasNext()) {
								IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 100);
								SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
								List list = (List)table.get(provider);
								IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);

								provider.add(providerResources, IResource.DEPTH_INFINITE, subMonitor);
							}
						} catch (TeamException e) {
							throw new InvocationTargetException(e);
						} 
					}
			 }
			repository = svnResource.getRepository();
            svnClient = repository.getSVNClient();
			try {
					monitor.worked(100);
				File[] files = getVersionedFiles();
				if (selectedResources == null) svnClient.diff(files,tmpFile,recursive);
				else {
					if (eclipseFormat) {
						HashSet includedResources = new HashSet();
						includedResources.addAll(Arrays.asList(unaddedResources));
						includedResources.addAll(Arrays.asList(resources));
						createEclipsePatch((IResource[]) includedResources.toArray(new IResource[0]), tmpFile, recursive);
					} else {
						File relativeToPath = null;
						if (projectRelative) {
							relativeToPath = selectedResources[0].getProject().getLocation().toFile();
						} else {
							relativeToPath = getRelativeToPath();
							if (relativeToPath.isFile()) {
								relativeToPath = relativeToPath.getParentFile();
							}
						}
		                svnClient.createPatch(files, relativeToPath, tmpFile, recursive);
					}
				}
 				monitor.worked(300);     
                InputStream is = new FileInputStream(tmpFile);
                byte[] buffer = new byte[30000];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer,0,length);                    
                }
			} finally {
				os.close();
			}

			 if(newFiles.size() > 0)
			 {
				for (int i = 0; i < newFiles.size(); i++)
				{
					IResource resource = (IResource) newFiles.get(i);
					try {
						SVNWorkspaceRoot.getSVNResourceFor(resource).revert();
					} catch (Exception e) {}
				} 
			 }
			
			boolean emptyDiff = false;
			
			if(toClipboard) {				
				final ByteArrayOutputStream baos = (ByteArrayOutputStream)os;
				if(baos.size() == 0) {
					emptyDiff = true;
				} else {
					Display.getDefault().syncExec(new Runnable() {
						 public void run() {
							TextTransfer plainTextTransfer = TextTransfer.getInstance();
							Clipboard clipboard= new Clipboard(shell.getDisplay());		
							clipboard.setContents(
								new String[]{baos.toString()}, 
								new Transfer[]{plainTextTransfer});	
							clipboard.dispose();
						 }
					});
				} 
			} else {
				if(outputFile.length() == 0) {
					emptyDiff = true;
					outputFile.delete();
				}	
			}

			// check for empty diff and report
			if (emptyDiff) {
				Display.getDefault().syncExec(new Runnable() {
					 public void run() {
							MessageDialog.openInformation(
									shell,
									Policy.bind("GenerateSVNDiff.noDiffsFoundTitle"), //$NON-NLS-1$
									Policy.bind("GenerateSVNDiff.noDiffsFoundMsg")); //$NON-NLS-1$
						 }
				});
			}
      } catch (Exception e) {    
         throw new InvocationTargetException(e);
	  }				
      finally {
    	if (repository != null) {
    		repository.returnSVNClient(svnClient);
    	}
		monitor.done();
	  }
	}
	
	private File getRelativeToPath() {
		if (selectedResources.length == 1) {
			return new File(selectedResources[0].getLocation().toString());
		}
		String commonRoot = null;
		String path = selectedResources[0].getLocation().toString();
		tag1:		
		for (int i = 0; i < path.length(); i++) {
			String partialPath = path.substring(0, i+1);
			if (partialPath.endsWith("/") || partialPath.endsWith("\\")) {
	    		for (int j = 1; j < selectedResources.length; j++) {
	    			if (!selectedResources[j].getLocation().toString().startsWith(partialPath)) break tag1;
	    		}
	    		commonRoot = partialPath.substring(0, i);				
			}
		}
		if (commonRoot != null) return new File(commonRoot);
		return null;
	}
	
	private File[] getVersionedFiles() {
		ArrayList versionedFileList = new ArrayList();
		ArrayList unaddedResourceList = new ArrayList();
		for (int i = 0; i < unaddedResources.length; i++)
			unaddedResourceList.add(unaddedResources[i]);
		for (int i = 0; i < resources.length; i++) {
			if (!containsResource(unaddedResourceList, resources[i]) || containsResource(newFiles, resources[i]))
				versionedFileList.add(new File(resources[i].getLocation().toOSString()));
		}	
		File[] files = new File[versionedFileList.size()];
		versionedFileList.toArray(files);
		return files;
	}
	
	private boolean containsResource(ArrayList list, IResource resource) {
		if (list.contains(resource)) return true;
		IResource parent = resource;
		while (parent != null) {
			parent = parent.getParent();
			if (list.contains(parent)) return true;
		}
		return false;
	}


	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}
	
	private void createEclipsePatch(IResource[] paths, File outputFile, boolean recurse) throws SVNClientException {
		FileOutputStream os = null;
		InputStream is = null;
		ISVNClientAdapter client = null;
		ISVNRepositoryLocation repository = null;
		try {
			byte[] buffer = new byte[4096];
			
			os = new FileOutputStream(outputFile);
			if (paths.length > 0) {
				os.write(ECLIPSE_PATCH_HEADER.getBytes());
				os.write(EOL.getBytes());
			}
			
			Map projectToResources = new HashMap();
			
			for (int i = 0; i < paths.length; i++) {
				IResource resource = paths[i];
				IProject project = resource.getProject();
				List files = (List) projectToResources.get(project);
				if (files == null) {
					files = new ArrayList();
					projectToResources.put(project, files);
				}
				files.add(resource.getLocation().toFile());
			}
			for (Iterator iEntry = projectToResources.entrySet().iterator(); iEntry.hasNext();) {
				Entry entry = (Entry) iEntry.next();
				
				IResource project = (IResource) entry.getKey();
				List files = (List) entry.getValue();
				
				repository = SVNWorkspaceRoot.getSVNResourceFor(project).getRepository();
				client = repository.getSVNClient();

				os.write(ECLIPSE_PROJECT_MARKER.getBytes());
				os.write(project.getName().getBytes());
				os.write(EOL.getBytes());
				
				File tempFile = File.createTempFile("tempDiff", ".txt");
				tempFile.deleteOnExit();
				client.createPatch((File[]) files.toArray(new File[files.size()]), project.getLocation().toFile(), tempFile, recurse);
				
				SVNWorkspaceRoot.getSVNResourceFor(project).getRepository().returnSVNClient(client);
				client = null;
				repository = null;
				
				try {
					is = new FileInputStream(tempFile);
					
					int bytes_read;
					while ((bytes_read = is.read(buffer)) != -1)
						os.write(buffer, 0, bytes_read);				
				} finally {
					if (is != null) try {is.close();} catch (IOException e) {}
				}
			}
		} catch (Exception e) {
			throw new SVNClientException(e);
		} finally {
			if (os != null) try {os.close();} catch (IOException e) {}
			if (repository != null) {
				repository.returnSVNClient(client);
			}
		}
	}
}
