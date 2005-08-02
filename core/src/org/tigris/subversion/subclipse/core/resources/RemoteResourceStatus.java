package org.tigris.subversion.subclipse.core.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * This class has an interface which is very similar to ISVNStatus but we make
 * sure to take as little memory as possible. This class also have a getBytes()
 * method and a constructor/factory method that takes bytes.
 * (However, the bytes are not complete representation of this status, just subset of interesting attributes) 
 * 
 * Since we want to add/modify the nodeKind the setter is also there
 * @see org.tigris.subversion.svnclientadapter.ISVNStatus
 * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus
 */
public class RemoteResourceStatus extends ResourceStatus {

    static final long serialVersionUID = 1L;

    public static RemoteResourceStatus fromBytes(byte[] bytes) throws SVNException
    {
    	return ((bytes != null) && (bytes.length > 0)) ? new RemoteResourceStatus(bytes) : null;
    }

    private RemoteResourceStatus() {}

	public RemoteResourceStatus(ISVNStatus realStatus, SVNRevision.Number revision) {
		super(realStatus);

		/** a temporary variable serving as immediate cache for various status values */
    	Object aValue = null;
		
        this.textStatus = realStatus.getRepositoryTextStatus().toInt();
        this.propStatus = realStatus.getRepositoryPropStatus().toInt();

        aValue = revision;
        if (aValue == null) {
            this.revision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
            this.revision = ((SVNRevision.Number) aValue).getNumber();
        }
	}
	
	public void setNodeKind(SVNNodeKind informedKind) {
		this.nodeKind = informedKind.toInt();
	}
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#getBytes()
     */
    public byte[] getBytes() {    	
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getBytesInto(new DataOutputStream(out));
        return out.toByteArray();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#getBytesInto(java.io.DataOutputStream)
     */
    protected DataOutputStream getBytesInto(DataOutputStream dos) {
    	super.getBytesInto(dos);
        try {
            // file
            dos.writeUTF(path);

        } catch (IOException e) {
            return null;
        }
        return dos;
    }

    /**
     * (Re)Construct an object from the given bytes 
     * @param bytes
     * @throws SVNException
     */
    protected RemoteResourceStatus(byte[] bytes) throws SVNException {
    	super();    	
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(in);
    	initFromBytes(new DataInputStream(in));
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.resources.ResourceStatus#initFromBytes(java.io.DataInputStream)
     */
    protected int initFromBytes(DataInputStream dis) throws SVNException {
    	int version = super.initFromBytes(dis);
        try {
            // path
            path = dis.readUTF();
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create LocalResourceStatus from bytes", e);
        }
        return version;
    }

    
}
