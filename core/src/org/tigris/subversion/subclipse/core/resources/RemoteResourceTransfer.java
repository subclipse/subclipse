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
package org.tigris.subversion.subclipse.core.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.RevisionUtils;

/**
 * This class is used when copying and pasting remote resources to/from clipboard
 *
 */
public class RemoteResourceTransfer extends ByteArrayTransfer {
	
	private static final String REMOTERESOURCENAME = "ISVNRemoteResource"; //$NON-NLS-1$
	private static final int REMOTERESOURCEID = registerType(REMOTERESOURCENAME);
	private static RemoteResourceTransfer _instance = new RemoteResourceTransfer();
	
    public static RemoteResourceTransfer getInstance () {
    	return _instance;
    }
    
    public void javaToNative (Object object, TransferData transferData) {
    	if (object == null || !(object instanceof ISVNRemoteResource)) return;
    	
    	if (isSupportedType(transferData)) {
    		ISVNRemoteResource remoteResource = (ISVNRemoteResource) object;	
    		try {
    			// write data to a byte array and then ask super to convert to pMedium
    			ByteArrayOutputStream out = new ByteArrayOutputStream();
    			DataOutputStream writeOut = new DataOutputStream(out);
                
   				// write 1 if remote resource is folder, 0 otherwise
                byte isFolder = (remoteResource.isFolder()?(byte)1:(byte)0);
                writeOut.write(isFolder);
                    
                // we write the url of the remote resource
                byte[] buffer = remoteResource.getUrl().toString().getBytes();
    			writeOut.writeInt(buffer.length);
    			writeOut.write(buffer);
    				
                // we write the url of the repository
                buffer = remoteResource.getRepository().getUrl().toString().getBytes();
    			writeOut.writeInt(buffer.length);
    			writeOut.write(buffer);
    		    
                // we write the revision
                buffer = remoteResource.getRevision().toString().getBytes();
                writeOut.writeInt(buffer.length);
                writeOut.write(buffer);
                
                buffer = out.toByteArray();
    		    writeOut.close();
    
                // converts a java byte[]to a platform specific representation
    		    super.javaToNative(buffer, transferData);
    			
    	   } catch (IOException e) {
    	   }
    	}
    }
    
    public Object nativeToJava(TransferData transferData){	
    
    	if (isSupportedType(transferData)) {
    		
    		byte[] buffer = (byte[])super.nativeToJava(transferData);
    		if (buffer == null) return null;
    		
            ISVNRemoteResource[] remoteResources = new ISVNRemoteResource[0];
    		try {
    			ByteArrayInputStream in = new ByteArrayInputStream(buffer);
    			DataInputStream readIn = new DataInputStream(in);

                byte isFolder = (byte)readIn.read();

                // first, we read the url of the remote resource
    			int size = readIn.readInt();
    			byte[] name = new byte[size];
    			readIn.read(name);
    			URL urlResource = new URL(new String(name));
                    
                // then we read the url of the repository
    			size = readIn.readInt();
    			name = new byte[size];
    			readIn.read(name);
                String location = new String(name);
                
                // we read the revision
                size = readIn.readInt();
                name = new byte[size];
                readIn.read(name);
                Revision revision = RevisionUtils.getRevision(new String(name));
                    
                ISVNRepositoryLocation repositoryLocation = SVNProviderPlugin.getPlugin().getRepository(location);
                    
                ISVNRemoteResource remoteResource;
                if (isFolder == 1)
                    remoteResource = new RemoteFolder(repositoryLocation,urlResource, revision);
                else
                    remoteResource = new RemoteFile(repositoryLocation,urlResource, revision);
    				
                ISVNRemoteResource[] temp = new ISVNRemoteResource[remoteResources.length + 1];
    			System.arraycopy(remoteResources, 0, temp, 0, remoteResources.length);
                temp[remoteResources.length] = remoteResource;
                remoteResources = temp;

    			readIn.close();
            } catch (ParseException e) {
                return null;
            } catch (SVNException e) {
                return null; 
    		} catch (IOException e) {
    			return null;
    		}
    		return remoteResources[0];
    	}
    
    	return null;
    }
    protected String[] getTypeNames(){
    	return new String[]{REMOTERESOURCENAME};
    }
    protected int[] getTypeIds(){
    	return new int[] {REMOTERESOURCEID};
    }
}
