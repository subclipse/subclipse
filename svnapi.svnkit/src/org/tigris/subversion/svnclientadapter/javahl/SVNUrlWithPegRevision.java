package org.tigris.subversion.svnclientadapter.javahl;

import java.net.MalformedURLException;
import java.text.ParseException;

import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SVNUrlWithPegRevision {
	private SVNUrl url;
	private SVNRevision pegRevision;

	public SVNUrlWithPegRevision(SVNUrl url) {
		parse(url);
	}

	public SVNUrl getUrl() {
		return url;
	}

	public SVNRevision getPegRevision() {
		return pegRevision;
	}
	
	private void parse(SVNUrl url) {
		String urlString = url.toString();
		if (!urlString.endsWith("@")) {
			int index = urlString.lastIndexOf("@");
			if (index != -1) {
				String rev = urlString.substring(index + 1);
				try {
					pegRevision = SVNRevision.getRevision(rev);
				} catch (ParseException e) {}
				if (pegRevision != null) {
					urlString = urlString.substring(0, index);
					try {
						this.url = new SVNUrl(urlString);
					} catch (MalformedURLException e) {
						this.url = url;
						pegRevision = null;
					}
					return;
				}
			}
		}
		this.url = url;
	}

}
