/*
 * Created on 23 Ιουλ 2004
 */
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Peek for (get) the resource status.
 * Do not descend to children and DO NOT affect sync cache in any way !
 * This command should have no side effects.
 */
public class PeekStatusCommand {
    private final IResource resource;

    private ISVNStatus status = null;
    private SVNRevision.Number revision;

    public PeekStatusCommand(IResource resource) {
        this.resource = resource;
    }

    public void execute(ISVNClientAdapter client) throws SVNException {
        ISVNNotifyListener revisionListener = new ISVNNotifyListener() {
            public void setCommand(int command) {}
            public void logCommandLine(String commandLine) {}
            public void logMessage(String message) {}
            public void logError(String message) {}
            public void logRevision(long revision) {
                PeekStatusCommand.this.revision = new SVNRevision.Number(revision);
            }
            public void logCompleted(String message) {}
            public void onNotify(File path, SVNNodeKind kind) {}
        };

        try{
            client.addNotifyListener( revisionListener );
            File file = resource.getLocation().toFile();
            status = null;
            ISVNStatus[] statuses = client.getStatus( file, false, true, false);
            for (int i = 0; i < statuses.length; i++) {
				if (file.equals(statuses[i].getFile()))
				{
					status = statuses[i];
					break;
				}
			}
        }
        catch (SVNClientException e) {
        	throw SVNException.wrapException(e);
        }
        finally {
            client.removeNotifyListener( revisionListener );
        }
    }

    public ISVNStatus getStatus() {
        return status;
    }

    public LocalResourceStatus getLocalResourceStatus()
    {    	
    	return (status != null) ? new LocalResourceStatus(status) : null;
    }
    
    public SVNRevision.Number getRevision() {
        return revision;
    }
}
