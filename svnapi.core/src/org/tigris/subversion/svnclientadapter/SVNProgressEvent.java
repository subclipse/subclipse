package org.tigris.subversion.svnclientadapter;

public class SVNProgressEvent {
	private long progress;
	private long total;
	
	public final static long UNKNOWN = -1;
	
	public SVNProgressEvent(long progress, long total) {
		super();
		this.progress = progress;
		this.total = total;
	}

	public long getProgress() {
		return progress;
	}

	public long getTotal() {
		return total;
	}
	
}
