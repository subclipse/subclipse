/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;

public class FileNode extends BufferedContent implements ITypedElement, IEditableContent, IEncodedStreamContentAccessor {
	private File file;
	private String charSet;

	public FileNode(File file) {
		super();
		this.file = file;
		Assert.isNotNull(file);
	}
	
	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	public File getFile() {
		return file;
	}
	
	public String getName() {
		return file.getName();
	}
	
	public String getPrefix() {
		String prefix = null;
		int index = getName().indexOf("."); //$NON-NLS-1$
		if (index == -1) prefix = getName();
		else prefix = getName().substring(0, index);
		while (prefix.length() < 3) prefix = prefix + "_"; //$NON-NLS-1$
		return prefix;
	}
	
	public String getType() {
		int index = getName().indexOf("."); //$NON-NLS-1$
		if (index != -1 && getName().length() > index + 1) {
			return getName().substring(index + 1);
		}
		return ITypedElement.TEXT_TYPE;
	}

	protected InputStream createStream() throws CoreException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return null;
		}	
	}
	
	public void commit(IProgressMonitor pm) throws CoreException {
		byte[] bytes= getContent();
		FileOutputStream os;
		try {
			os = new FileOutputStream(file);
			os.write(bytes);
			os.close();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isEditable() {
		return true;
	}
	
	public boolean isReadOnly() {
		return false;
	}

	public Image getImage() {
		return null;
	}

	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		return null;
	}

	public String getCharset() throws CoreException {
		return charSet;
	}

}
