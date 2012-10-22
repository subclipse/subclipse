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
package org.tigris.subversion.subclipse.ui.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;
import org.tigris.subversion.svnclientadapter.SVNUrl;

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
    		// write data to a byte array and then ask super to convert to pMedium
  			byte[] buffer = toByteArray((ISVNRemoteResource) object);
  		    super.javaToNative(buffer, transferData);
    	}
    }

    public Object nativeToJava(TransferData transferData){	
    	if (!isSupportedType(transferData)) return null;
    	
  		byte[] buffer = (byte[])super.nativeToJava(transferData);
  		if (buffer == null) return null;
        
        return fromByteArray(buffer);
    }

    public byte[] toByteArray(ISVNRemoteResource remoteResource) {
      try {
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
        
        if (remoteResource.getAuthor() != null) {
        	writeOut.writeUTF( remoteResource.getAuthor());
        }
        
        writeOut.close();
        
        // converts a java byte[]to a platform specific representation
        return out.toByteArray();
      } catch (IOException e) {
        return null;
      }
    }
    
    public Object fromByteArray(byte[] buffer) {
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
        
        String author = null;
        try {
        	author = readIn.readUTF();
        } catch (Exception e) {
        	// Ignore null author
        }
        
        ISVNRepositoryLocation repositoryLocation = SVNProviderPlugin.getPlugin().getRepository(location);
            
        if (isFolder) {
          return new RemoteFolder(null, repositoryLocation,urlResource, revision, lastChangedRevision, date, author);
        }
        return new RemoteFile(null, repositoryLocation,urlResource, revision, lastChangedRevision, date, author);
      } catch(Exception ex) {
        return null;        
      }
    }

    protected String[] getTypeNames(){
    	return new String[]{REMOTERESOURCENAME};
    }
    protected int[] getTypeIds(){
    	return new int[] {REMOTERESOURCEID};
    }

}
