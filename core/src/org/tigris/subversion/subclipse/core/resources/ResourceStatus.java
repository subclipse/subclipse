package org.tigris.subversion.subclipse.core.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

/**
 * This class has an interface which is very similar to ISVNStatus but we make
 * sure to take as little memory as possible. This class also have a getBytes()
 * method and a constructor/factory method that takes bytes.
 * (However, the bytes are not complete representation of this status, just subset of interesting attributes) 
 * 
 * @see org.tigris.subversion.svnclientadapter.ISVNStatus
 */
public class ResourceStatus implements Serializable {

    static final long serialVersionUID = 1L;

    protected static int FORMAT_VERSION_1 = 1;
    protected static int FORMAT_VERSION_2 = 2;

    protected String url;
    protected String path; // absolute path -- not stored in bytes in this class. Superclasses may store it ...
    protected long lastChangedRevision;
    protected long lastChangedDate;
    protected String lastCommitAuthor;
    protected int textStatus;
    protected int propStatus;
    protected long revision;
    protected int nodeKind;
	
    protected ResourceStatus() {}

	public ResourceStatus(ISVNStatus status) {
		super();
    	/** a temporary variable serving as immediate cache for various status values */
    	Object aValue = null;

    	aValue = status.getUrl();
        if (aValue == null) {
            this.url = null;
        } else {
            this.url = ((SVNUrl) aValue).toString();
        }

        aValue = status.getLastChangedRevision();
        if (aValue == null) {
            this.lastChangedRevision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
            this.lastChangedRevision = ((SVNRevision.Number) aValue).getNumber();
        }

        aValue = status.getLastChangedDate();
        if (aValue == null) {
            this.lastChangedDate = -1;
        } else {
            this.lastChangedDate = ((Date) aValue).getTime();
        }

        this.lastCommitAuthor = status.getLastCommitAuthor();
        this.textStatus = status.getTextStatus().toInt();
        this.propStatus = status.getPropStatus().toInt();

        aValue = status.getRevision();
        if (aValue == null) {
            this.revision = SVNRevision.SVN_INVALID_REVNUM;
        } else {
            this.revision = ((SVNRevision.Number) aValue).getNumber();
        }

        this.nodeKind = status.getNodeKind().toInt();
        
        this.path = status.getFile().getAbsolutePath();
	}
	
	public void setNodeKind(SVNNodeKind informedKind) {
		this.nodeKind = informedKind.toInt();
	}
	
    public String toString()
    {
    	return ((path != null) ? path : "") + " (" + revision + ") " + getTextStatus().toString();
    }
	
    /**
     * @return Returns the file.
     */
    public File getFile() {
        return new File(path);
    }

    /**
     * @return Returns the absolute resource path.
     * (It is absolute since it was constructed as status.getFile().getAbsolutePath())
     */
    public IPath getPath() {
        return new Path(path);
    }

    /**
     * @return Returns the absolute resource path (as String).
     * (It is absolute since it was constructed as status.getFile().getAbsolutePath())
     */
    public String getPathString() {
        return path;
    }

    public SVNStatusKind getTextStatus() {
        return SVNStatusKind.fromInt(textStatus);
    }

    public SVNStatusKind getPropStatus() {
        return SVNStatusKind.fromInt(propStatus);
    }

	/**
	 * Answer a 'join' of text and property statuses.
	 * The text has priority, i.e. the prop does not override the text status
	 * unless it is harmless - i.e. SVNStatusKind.NORMAL
	 * @param textStatus
	 * @param propStatus
	 * @return
	 */
	public SVNStatusKind getStatusKind()
	{
		if (!SVNStatusKind.NORMAL.equals(getTextStatus()))
		{
			return getTextStatus(); 
		}
		else
		{
			if (SVNStatusKind.MODIFIED.equals(getPropStatus()) || SVNStatusKind.CONFLICTED.equals(getPropStatus()))
			{
				return getPropStatus();
			}
			else
			{
				return getTextStatus();
			}
		}    		
	}

	public ISVNRepositoryLocation getRepository()
	{
		if (getUrlString() != null)
		{
			try {
				return SVNProviderPlugin.getPlugin().getRepository(getUrlString());
			} catch (SVNException e) {
				// an exception is thrown when resource	is not managed
				SVNProviderPlugin.log(e);
				return null;
			}
		}
		else
		{
			try {
				SVNTeamProvider teamProvider = (SVNTeamProvider)RepositoryProvider.getProvider(getResource().getProject(), SVNProviderPlugin.getTypeId());
				return teamProvider.getSVNWorkspaceRoot().getRepository();
			} catch (SVNException e) {
				// an exception is thrown when resource	is not managed
				SVNProviderPlugin.log(e);
				return null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getRevision()
	 */
    public Number getRevision() {
        if (revision == SVNRevision.SVN_INVALID_REVNUM) {
            return null;
        } else {
            return new SVNRevision.Number(revision);
        }
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getNodeKind()
	 */
    public SVNNodeKind getNodeKind() {
        return SVNNodeKind.fromInt(nodeKind);
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getUrl()
	 */
    public SVNUrl getUrl() {
        if (url == null) {
            return null;
        } else {
            try {
                return new SVNUrl(url);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    public String getUrlString()
    {
    	return url;
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastChangedRevision()
	 */
    public Number getLastChangedRevision() {
        if (lastChangedRevision == SVNRevision.SVN_INVALID_REVNUM) {
            return null;
        } else {
            return new SVNRevision.Number(lastChangedRevision);
        }
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastChangedDate()
	 */
    public Date getLastChangedDate() {
        if (lastChangedDate == -1) {
            return null;
        } else {
            return new Date(lastChangedDate);
        }
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNStatus#getLastCommitAuthor()
	 */
    public String getLastCommitAuthor() {
        return lastCommitAuthor;
    }

    /**
     * (Re)Construct an object from the given bytes 
     * @param bytes
     * @throws SVNException
     */
    protected ResourceStatus(byte[] bytes) throws SVNException {
    	super();    	
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(in);
    	initFromBytes(new DataInputStream(in));
    }
    
    /**
     * Initialize receivers internal state according the information encoded in the given DataInputStream.
     * (Which contents was created with {@link #getBytes()}
     * @param dis
     * @return encoding format version
     * @throws SVNException
     */
    protected int initFromBytes(DataInputStream dis) throws SVNException {
    	int version; 
        try {
            version = dis.readInt();
            if (version != FORMAT_VERSION_1 && version != FORMAT_VERSION_2) {
                throw new SVNException("Invalid format");
            }

            // url
            String urlString = dis.readUTF();
            if (urlString.equals("")) {
                url = null;
            } else {
                url = urlString;
            }

            // lastChangedRevision
            lastChangedRevision = dis.readLong();

            // lastChangedDate
            lastChangedDate = dis.readLong();

            // lastCommitAuthor
            String lastCommitAuthorString = dis.readUTF();
            if ((url == null) && (lastCommitAuthorString.equals(""))) {
                lastCommitAuthor = null;
            } else {
                lastCommitAuthor = lastCommitAuthorString;
            }

            // textStatus
            textStatus = dis.readInt();

            // propStatus
            propStatus = dis.readInt();

            // revision
            revision = dis.readLong();

            // nodeKind
            nodeKind = dis.readInt();
            
        } catch (IOException e) {
            throw new SVNException(
                    "cannot create RemoteResourceStatus from bytes", e);
        }
        return version;
    }

    /**
     * Get the status encoded in bytes
     * @return
     */
    public byte[] getBytes() {    	
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getBytesInto(new DataOutputStream(out));
        return out.toByteArray();
    }

    /**
     * Encode the internal state into given DataOutputStream
     * Decoding is done by {@link #initFromBytes(DataInputStream)}
     * @param dos
     * @return
     */
    protected DataOutputStream getBytesInto(DataOutputStream dos) {
        try {
            dos.writeInt(FORMAT_VERSION_2);

            // url
            if (url == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(url);
            }

            // lastChangedRevision
            dos.writeLong(lastChangedRevision);

            // lastChangedDate
            dos.writeLong(lastChangedDate);

            // lastCommitAuthor
            if (lastCommitAuthor == null) {
                dos.writeUTF("");
            } else {
                dos.writeUTF(lastCommitAuthor);
            }

            // textStatus
            dos.writeInt(textStatus);

            // propStatus
            dos.writeInt(propStatus);

            // revision
            dos.writeLong(revision);

            // nodeKind
            dos.writeInt(nodeKind);

        } catch (IOException e) {
            return null;
        }
        return dos;
    }
    
    /**
     * Gets the resource this status is corresponding to.
     * Use either ResourceInfo.getType() or getNodeKind() to determine the proper kind of resource.
     * The resource does not need to exists (yet)
     * @return IResource
     */
    public IResource getResource()
    {
    	if (this.path == null) return null;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath resourcePath = SVNWorkspaceRoot.pathForLocation(this.getPath());
		int kind = SVNWorkspaceRoot.getResourceType(resourcePath);			

		if (kind == IResource.FILE)
		{
			return root.getFile(resourcePath);
		}
		else if(kind == IResource.FOLDER)
		{
			return root.getFolder(resourcePath);
		}
		else if (kind == IResource.PROJECT)
		{
			return root.getProject(resourcePath.segment(0));
		}
		else if (kind == IResource.ROOT)
		{
			return root;
		}
		else if (this.getNodeKind() == SVNNodeKind.FILE)
		{
			return root.getFile(resourcePath);
		}
		else
		{
			if (resourcePath.isRoot())
			{
				return root;
			}
			else if (resourcePath.segmentCount() == 1)
			{
				return root.getProject(resourcePath.segment(0));
			}
		}
//        if (this.getNodeKind() == SVNNodeKind.UNKNOWN) {
//        	File file = this.getPath().toFile();
//        	if (file.isDirectory())
//        		resource = workspaceRoot.getContainerForLocation(pathEclipse);
//        	else
//        		resource = workspaceRoot.getFileForLocation(pathEclipse);
//        }

		return root.getFolder(resourcePath);
    }

}
