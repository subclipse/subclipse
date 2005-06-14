/*
 * Created on 23 Ιουλ 2004
 */
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
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
    private SVNRevision.Number revision;

    public StatusCommand(File file, boolean descend, boolean getAll, boolean contactServer) {
        this.file = file;
        this.descend = descend;
        this.getAll = getAll;
        this.contactServer = contactServer;
    }

    public void execute(ISVNClientAdapter client) throws SVNClientException {
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

            // we calculated the statuses of some resources. We update the cache manager
            // so that it does not have to redo the status retrieving itself
            SVNProviderPlugin.getPlugin().getStatusCacheManager().setStatuses(convert(statuses));
        }
        finally {
            client.removeNotifyListener( revisionListener );
        }
    }

    private LocalResourceStatus[] convert(ISVNStatus[] svnStatuses) {
    	LocalResourceStatus[] localStatuses = new LocalResourceStatus[svnStatuses.length];
    	for (int i = 0; i < svnStatuses.length;i++) {
    		localStatuses[i] = new LocalResourceStatus(svnStatuses[i]);
    	}
    	return localStatuses;
    }
    
    public ISVNStatus[] getStatuses() {
        return statuses;
    }

    public SVNRevision.Number getRevision() {
        return revision;
    }
}
