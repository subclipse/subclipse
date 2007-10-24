/*******************************************************************************
 * copied from: org.eclipse.compare.internal.Utilities
 *
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui.compare.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Convenience and utility methods.
 */
public class Utilities {
	
	public static IWorkbenchPartSite findSite(Control c) {
		while (c != null && !c.isDisposed()) {
			Object data= c.getData();
			if (data instanceof IWorkbenchPart)
				return ((IWorkbenchPart)data).getSite();
			c= c.getParent();
		}
		return null;
	}

	public static void setEnableComposite(Composite composite, boolean enable) {
		Control[] children= composite.getChildren();
		for (int i= 0; i < children.length; i++)
			children[i].setEnabled(enable);
	}

	public static boolean getBoolean(CompareConfiguration cc, String key, boolean dflt) {
		if (cc != null) {
			Object value= cc.getProperty(key);
			if (value instanceof Boolean)
				return ((Boolean) value).booleanValue();
		}
		return dflt;
	}
	
	public static boolean okToUse(Widget widget) {
		return widget != null && !widget.isDisposed();
	}
	
	private static ArrayList internalGetResources(ISelection selection, Class type) {
		
		ArrayList tmp= new ArrayList();

		if (selection instanceof IStructuredSelection) {
		
			Object[] s= ((IStructuredSelection)selection).toArray();
				
			for (int i= 0; i < s.length; i++) {
				
				IResource resource= null;
				
				Object o= s[i];
				if (type.isInstance(o)) {
					resource= (IResource) o;
						
				} else if (o instanceof IAdaptable) {
					IAdaptable a= (IAdaptable) o;
					Object adapter= a.getAdapter(IResource.class);
					if (type.isInstance(adapter))
						resource= (IResource) adapter;
				}
				
				if (resource != null && resource.isAccessible())
					tmp.add(resource);
			}
		}
		
		return tmp;
	}
	
	/*
	 * Convenience method: extract all accessible <code>IResources</code> from given selection.
	 * Never returns null.
	 */
	public static IResource[] getResources(ISelection selection) {
		ArrayList tmp= internalGetResources(selection, IResource.class);
		return (IResource[]) tmp.toArray(new IResource[tmp.size()]);
	}
	
	/*
	 * Convenience method: extract all accessible <code>IFiles</code> from given selection.
	 * Never returns null.
	 */
	public static IFile[] getFiles(ISelection selection) {
		ArrayList tmp= internalGetResources(selection, IFile.class);
		return (IFile[]) tmp.toArray(new IFile[tmp.size()]);
	}

	public static byte[] readBytes(InputStream in) {
		ByteArrayOutputStream bos= new ByteArrayOutputStream();
		try {		
			while (true) {
				int c= in.read();
				if (c == -1)
					break;
				bos.write(c);
			}
					
		} catch (IOException ex) {
			return null;
		
		} finally {
			Utilities.close(in);
			try {
				bos.close();
			} catch (IOException x) {
				// silently ignored
			}
		}
		
		return bos.toByteArray();
	}

	public static String getIconPath(Display display) {
		return "icons/full/";	//$NON-NLS-1$
	}
	
	public static void initToggleAction(IAction a, ResourceBundle bundle, String prefix, boolean checked) {

		String tooltip= null;
		if (checked)
			tooltip= getString(bundle, prefix + "tooltip.checked", null);	//$NON-NLS-1$
		else
			tooltip= getString(bundle, prefix + "tooltip.unchecked", null);	//$NON-NLS-1$
		if (tooltip == null)
			tooltip= getString(bundle, prefix + "tooltip", null);	//$NON-NLS-1$
		
		if (tooltip != null)
			a.setToolTipText(tooltip);
			
		String description= null;
		if (checked)
			description= getString(bundle, prefix + "description.checked", null);	//$NON-NLS-1$
		else
			description= getString(bundle, prefix + "description.unchecked", null);	//$NON-NLS-1$
		if (description == null)
			description= getString(bundle, prefix + "description", null);	//$NON-NLS-1$
		
		if (description != null)
			a.setDescription(description);
			
	}

	public static String getString(ResourceBundle bundle, String key, String dfltValue) {
		
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return dfltValue;
	}
	
	public static String getFormattedString(ResourceBundle bundle, String key, String arg) {
		
		if (bundle != null) {
			try {
				return MessageFormat.format(bundle.getString(key), new String[] { arg });
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
	}
	
	public static String getString(String key) {
		try {
			return CompareUI.getResourceBundle().getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	public static String getFormattedString(String key, String arg) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle().getString(key), new String[] { arg });
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}	
	}

	public static String getFormattedString(String key, String arg0, String arg1) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle().getString(key), new String[] { arg0, arg1 });
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}	
	}

	public static String getString(ResourceBundle bundle, String key) {
		return getString(bundle, key, key);
	}
	
	public static int getInteger(ResourceBundle bundle, String key, int dfltValue) {
		
		if (bundle != null) {
			try {
				String s= bundle.getString(key);
				if (s != null)
					return Integer.parseInt(s);
			} catch (NumberFormatException x) {
				// NeedWork
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return dfltValue;
	}

	/**
	 * Answers <code>true</code> if the given selection contains resources that don't
	 * have overlapping paths and <code>false</code> otherwise. 
	 */
	/*
	public static boolean isSelectionNonOverlapping() throws TeamException {
		IResource[] resources = getSelectedResources();
		// allow operation for non-overlapping resource selections
		if(resources.length>0) {
			List validPaths = new ArrayList(2);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				
				// only allow cvs resources to be selected
				if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
					return false;
				}
				
				// check if this resource overlaps other selections		
				IPath resourceFullPath = resource.getFullPath();
				if(!validPaths.isEmpty()) {
					for (Iterator it = validPaths.iterator(); it.hasNext();) {
						IPath path = (IPath) it.next();
						if(path.isPrefixOf(resourceFullPath) || 
					       resourceFullPath.isPrefixOf(path)) {
							return false;
						}
					}
				}
				validPaths.add(resourceFullPath);
				
				// ensure that resources are managed
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
				if(cvsResource.isFolder()) {
					if( ! ((ICVSFolder)cvsResource).isCVSFolder()) return false;
				} else {
					if( ! cvsResource.isManaged()) return false;
				}
			}
			return true;
		}
		return false;
	}
	*/
	
	/* validate edit utilities */
	
	/*
	 * Returns null if an error occurred.
	 */
	public static String readString(InputStream is, String encoding) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, encoding));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
			// NeedWork
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
		return null;
	}
	
	public static String getCharset(IResource resource) {
		if (resource instanceof IEncodedStorage) {
			try {
				return ((IEncodedStorage)resource).getCharset();
			} catch (CoreException ex) {
				// fall  through
			}
		}
		return ResourcesPlugin.getEncoding();
	}
	
	public static byte[] getBytes(String s, String encoding) {
		byte[] bytes= null;
		if (s != null) {
			try {
				bytes= s.getBytes(encoding); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				bytes= s.getBytes();
			}
		}
		return bytes;
	}

	public static String readString(IStreamContentAccessor sa) throws CoreException {
		InputStream is= sa.getContents();
		String encoding= null;
		if (sa instanceof IEncodedStreamContentAccessor)
			encoding= ((IEncodedStreamContentAccessor)sa).getCharset();
		if (encoding == null)
			encoding= ResourcesPlugin.getEncoding();
		return Utilities.readString(is, encoding);
	}

	public static void close(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException ex) {
				// silently ignored
			}
		}
	}
}

