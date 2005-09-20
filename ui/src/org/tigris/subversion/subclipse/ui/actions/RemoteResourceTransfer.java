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
package org.tigris.subversion.subclipse.ui.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

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
                writeOut.writeBoolean(remoteResource.isFolder());
                    
                // we write the url of the remote resource
    			writeOut.writeUTF(remoteResource.getUrl().toString());
    				
                // we write the url of the repository
    			writeOut.writeUTF(remoteResource.getRepository().getUrl().toString());
    		    
                // we write the revision
                writeOut.writeUTF(remoteResource.getRevision().toString());

                // we write the last changed revision
                writeOut.writeUTF(remoteResource.getLastChangedRevision().toString());
                
                writeOut.writeLong( remoteResource.getDate().getTime());
                writeOut.writeUTF( remoteResource.getAuthor());
                
                writeOut.close();

                // converts a java byte[]to a platform specific representation
                byte[] buffer = out.toByteArray();
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

                boolean isFolder = readIn.readBoolean();

                // first, we read the url of the remote resource
    			SVNUrl urlResource = new SVNUrl(readIn.readUTF());
                    
                // then we read the url of the repository
                String location = readIn.readUTF();
                
                // we read the revision
                SVNRevision revision = SVNRevision.getRevision(readIn.readUTF());

                // we read the last changed revision
                SVNRevision.Number lastChangedRevision = ( Number) SVNRevision.getRevision(readIn.readUTF());
                
                Date date = new Date(readIn.readLong());
                String author = readIn.readUTF();
                
                ISVNRepositoryLocation repositoryLocation = SVNProviderPlugin.getPlugin().getRepository(location);
                    
                ISVNRemoteResource remoteResource;
                if (isFolder)
                    remoteResource = new RemoteFolder(null, repositoryLocation,urlResource, revision, lastChangedRevision, date, author);
                else
                    remoteResource = new RemoteFile(null, repositoryLocation,urlResource, revision, lastChangedRevision, date, author);
    				
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
