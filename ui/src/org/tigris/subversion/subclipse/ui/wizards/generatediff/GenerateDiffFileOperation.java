/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.generatediff;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

/**
 * An operation to run the SVN diff operation on a set of resources. The result
 * of the diff is written to a file. If there are no differences found, the
 * user is notified and the output file is not created.
 */
public class GenerateDiffFileOperation implements IRunnableWithProgress {

	private File outputFile;
	private IResource resource;
	private Shell shell;
	private boolean recursive;
	private boolean toClipboard;

	GenerateDiffFileOperation(IResource resource, File file, boolean toClipboard, boolean recursive, Shell shell) {
		this.resource = resource;
		this.outputFile = file;
		this.shell = shell;
        this.recursive = recursive;
		this.toClipboard = toClipboard;
	}

	/**
	 * @see IRunnableWithProgress#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException {
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

            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
            ISVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
			try {
                svnClient.diff(svnResource.getFile(),tmpFile,recursive);
                
                InputStream is = new FileInputStream(tmpFile);
                byte[] buffer = new byte[30000];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer,0,length);                    
                }
			} finally {
				os.close();
			}

			boolean emptyDiff = false;
			
			if(toClipboard) {				
				ByteArrayOutputStream baos = (ByteArrayOutputStream)os;
				if(baos.size() == 0) {
					emptyDiff = true;
				} else {
					TextTransfer plainTextTransfer = TextTransfer.getInstance();
					Clipboard clipboard= new Clipboard(shell.getDisplay());		
					clipboard.setContents(
						new String[]{baos.toString()}, 
						new Transfer[]{plainTextTransfer});	
					clipboard.dispose();
				}
			} else {
				if(outputFile.length() == 0) {
					emptyDiff = true;
					outputFile.delete();
				}	
			}

			//check for empty diff and report			
			if (emptyDiff) {
				MessageDialog.openInformation(
					shell,
					Policy.bind("GenerateSVNDiff.noDiffsFoundTitle"), //$NON-NLS-1$
					Policy.bind("GenerateSVNDiff.noDiffsFoundMsg")); //$NON-NLS-1$
			}
        } catch (Exception e) {    
            throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}
}
