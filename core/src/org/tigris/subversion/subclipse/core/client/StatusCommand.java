/*
 * Created on 23 Ιουλ 2004
 */
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Panagiotis K
 */
public class StatusCommand {
    private final File file;
    private final boolean descend;
    private final boolean getAll;
    private final boolean contactServer;

    private ISVNStatus[] statuses;
    protected SVNRevision.Number revision;

    public StatusCommand(File file, boolean descend, boolean getAll, boolean contactServer) {
        this.file = file;
        this.descend = descend;
        this.getAll = getAll;
        this.contactServer = contactServer;
    }

    /**
     * 
     * @param client
     * @throws SVNClientException
     * @deprecated use {@link #execute(ISVNClientAdapter, IProgressMonitor)} now ...
     */
    public void execute(ISVNClientAdapter client) throws SVNClientException {
    	execute(client, new NullProgressMonitor());
    }

    public void execute(final ISVNClientAdapter client, final IProgressMonitor monitor) throws SVNClientException {
        ISVNNotifyListener revisionListener = new ISVNNotifyListener() {
            public void setCommand(int command) {}
            public void logCommandLine(String commandLine) {}
            public void logMessage(String message) {}
            public void logError(String message) {}
            public void logRevision(long revision) {
                StatusCommand.this.revision = new SVNRevision.Number(revision);
            }
            public void logCompleted(String message) {}
            public void onNotify(File path, SVNNodeKind kind) {}
        };

        try{
            client.addNotifyListener( revisionListener );
            statuses = client.getStatus( file, descend, getAll, contactServer );
        }
        finally {
            client.removeNotifyListener( revisionListener );
        }
    }

    public ISVNStatus[] getStatuses() {
        return statuses;
    }

    public SVNRevision.Number getRevision() {
        return revision;
    }
}
