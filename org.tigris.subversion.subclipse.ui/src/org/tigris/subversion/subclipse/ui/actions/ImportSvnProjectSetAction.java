package org.tigris.subversion.subclipse.ui.actions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.ImportSvnProjectSetWizard;

public class ImportSvnProjectSetAction extends SVNAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		try {
			ISVNRemoteFile[] files = getSelectedRemoteFiles();
			IStorage storage = ((IResourceVariant)files[0]).getStorage(new NullProgressMonitor());
			File tempFile = writeToTempFile(storage);
			ImportSvnProjectSetWizard wizard = new ImportSvnProjectSetWizard(tempFile.getAbsolutePath());
	    	WizardDialog dialog = new WizardDialog(getShell(), wizard);
	    	dialog.open();
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Import a Team Project Set", e.getMessage()); //$NON-NLS-1$
		}
	}

	@Override
	protected boolean isEnabled() throws TeamException {
		ISVNRemoteFile[] resources = getSelectedRemoteFiles();
		if (resources == null || resources.length != 1 || !resources[0].getName().endsWith(".psf")) return false; //$NON-NLS-1$
		return true;
	}
	
	public File writeToTempFile(IStorage storage) throws IOException, CoreException {
		File tempFile = null;
		InputStream in = null;
		BufferedOutputStream fOut = null;
		tempFile = null;
		// Save InputStream to the file.
		in = storage.getContents();
		try {
			tempFile = File.createTempFile("svn", ".psf"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			throw new IOException (Policy.bind("RemoteFileEditorInput.3") + e.toString()); //$NON-NLS-1$
		}
		try {
			tempFile.deleteOnExit();			
			fOut = new BufferedOutputStream(new FileOutputStream(tempFile));
			byte[] buffer = new byte[32 * 1024];
			int bytesRead = 0;
			while ((bytesRead = in.read(buffer)) != -1) {
				fOut.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {
			throw new IOException(Policy.bind("RemoteFileEditorInput.4") + e.toString()); //$NON-NLS-1$
		} finally {
			if (in != null) {
				in.close();
			}
			if (fOut != null) {
				fOut.close();
			}
		}
		return tempFile;
	}

}
