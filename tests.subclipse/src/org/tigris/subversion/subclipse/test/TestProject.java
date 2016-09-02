/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: Erich Gamma (erich_gamma@ch.ibm.com) and Kent Beck
 * (kent@threeriversinstitute.org)
 ******************************************************************************/

package org.tigris.subversion.subclipse.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.launching.JavaRuntime;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class TestProject {
	private IProject project;
	private IJavaProject javaProject;
	private IPackageFragmentRoot sourceFolder;

	public TestProject(String projectName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(projectName);
		project.create(null);
		project.open(null);
		javaProject = JavaCore.create(project);

		IFolder binFolder = createBinFolder();

		setJavaNature();
		javaProject.setRawClasspath(new IClasspathEntry[0], null);

		createOutputFolder(binFolder);
		addSystemLibraries();
	}

	public IProject getProject() {
		return project;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public void addJar(Plugin plugin, String jar) throws MalformedURLException,
			IOException, JavaModelException {
		Path result = findFileInPlugin(plugin, jar);
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newLibraryEntry(result, null,
				null);
		javaProject.setRawClasspath(newEntries, null);
	}

	public boolean hasJar(String jar) throws JavaModelException {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getPath().lastSegment().equals(jar)) {
				return true;
			}
		}
		return false;
	}
	public IPackageFragment createPackage(String name) throws CoreException {
		if (sourceFolder == null)
			sourceFolder = createSourceFolder();
		return sourceFolder.createPackageFragment(name, false, null);
	}

	public IType createJavaType(IPackageFragment pack, String cuName,
			String source) throws JavaModelException {
		StringBuffer buf = new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("\n");
		buf.append(source);
		ICompilationUnit cu = pack.createCompilationUnit(cuName,
				buf.toString(), false, null);
		return cu.getTypes()[0];
	}

	private IFile createFile(IContainer folder, String name,
			InputStream contents) throws JavaModelException {
		IFile file = folder.getFile(new Path(name));
		try {
			file.create(contents, IResource.FORCE, null);

		} catch (CoreException e) {
			throw new JavaModelException(e);
		}

		return file;
	}

	public void dispose() throws CoreException {
		waitForIndexer();
		project.delete(true, true, null);
	}

	private IFolder createBinFolder() throws CoreException {
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(false, true, null);
		return binFolder;
	}

	private void setJavaNature() throws CoreException {
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[]{JavaCore.NATURE_ID});
		project.setDescription(description, null);
	}

	private void createOutputFolder(IFolder binFolder)
			throws JavaModelException {
		IPath outputLocation = binFolder.getFullPath();
		javaProject.setOutputLocation(outputLocation, null);
	}

	private IPackageFragmentRoot createSourceFolder() throws CoreException {
		IFolder folder = project.getFolder("src");
		folder.create(false, true, null);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);

		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
		javaProject.setRawClasspath(newEntries, null);
		return root;
	}

	private void addSystemLibraries() throws JavaModelException {
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaRuntime
				.getDefaultJREContainerEntry();
		javaProject.setRawClasspath(newEntries, null);
	}

	private Path findFileInPlugin(Plugin plugin, String file)
			throws MalformedURLException, IOException {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		URL base = null;
		if (plugin instanceof SVNUIPlugin) {
			base = SVNUIPlugin.getPlugin().getBundle().getEntry("/");
		} else if (plugin instanceof SVNProviderPlugin) {
			base = SVNProviderPlugin.getPlugin().getBundle().getEntry("/");
		}
		URL jarURL = new URL(base, file);
		URL localJarURL = Platform.asLocalURL(jarURL);
		return new Path(localJarURL.getPath());
	}

	public static void waitForIndexer() throws JavaModelException {

		ITypeNameRequestor requestor = new ITypeNameRequestor() {
			public void acceptClass(char[] packageName, char[] simpleTypeName,
					char[][] enclosingTypeNames, String path) {
			}
			public void acceptInterface(char[] packageName,
					char[] simpleTypeName, char[][] enclosingTypeNames,
					String path) {
			}
		};
		int matchRule = SearchPattern.R_EXACT_MATCH
				| SearchPattern.R_CASE_SENSITIVE;

		new SearchEngine().searchAllTypeNames(null, null, matchRule,
				IJavaSearchConstants.CLASS,
				SearchEngine.createWorkspaceScope(), requestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);

	}

}