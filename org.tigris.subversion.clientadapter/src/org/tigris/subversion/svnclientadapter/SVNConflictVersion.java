package org.tigris.subversion.svnclientadapter;

public class SVNConflictVersion {
    private String reposURL;
    private long pegRevision;
    private String pathInRepos;
    private int nodeKind;

	public SVNConflictVersion(String reposURL, long pegRevision, String pathInRepos, int nodeKind) {
        this.reposURL = reposURL;
        this.pegRevision = pegRevision;
        this.pathInRepos = pathInRepos;
        this.nodeKind = nodeKind;
	}
	
    public String getReposURL()
    {
        return reposURL;
    }

    public long getPegRevision()
    {
        return pegRevision;
    }

    public String getPathInRepos()
    {
        return pathInRepos;
    }

    public int getNodeKind()
    {
        return nodeKind;
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer("(");
    	switch (nodeKind) {
		case NodeKind.none:
			sb.append("none");
			break;
		case NodeKind.file:
			sb.append("file");
			break;
		case NodeKind.directory:
			sb.append("dir");
			break;			
		default:
			sb.append(nodeKind);
			break;
		}
    	sb.append(") " + reposURL + "/" + pathInRepos + "@" + pegRevision);
    	return sb.toString();
    }
    
    public final class NodeKind
    {
        public static final int none = 0;
        public static final int file = 1;
        public static final int directory = 2;
    }

}
