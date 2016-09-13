package org.tigris.subversion.subclipse.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

public class SVNExternal {
	private File file;
	private String propertyLine;
	private String folder;
	private String path;
	private String url;
	private long revision = -1;
	private long fixedAtRevision = -1;
	private boolean selected;
	
    private static final String REGEXP_REVISION = "-r ?(\\d+)";
    private static final Pattern pattern = Pattern.compile(REGEXP_REVISION);
	
	public SVNExternal(File file, String propertyLine) {
		super();
		this.file = file;
		this.propertyLine = propertyLine;
		if (propertyLine != null) {
			Matcher matcher = pattern.matcher(propertyLine);
			if (matcher.find()) {
				String revision = matcher.group();
				if (revision.startsWith("-r ")) {
					fixedAtRevision = Long.parseLong(revision.substring(3));
				}
				else {
					fixedAtRevision = Long.parseLong(revision.substring(2));
				}
				if (matcher.start() == 0) {
					String urlAndFolder = propertyLine.substring(matcher.end() + 1);
					int index = urlAndFolder.lastIndexOf(" ");
					if (index != -1) {
						url = urlAndFolder.substring(0, index);
						folder = urlAndFolder.substring(index + 1);
						if (file != null) {
							path = file.getAbsolutePath() + File.separator + urlAndFolder.substring(index + 1);
						}
					}
				}
				else {
					url = propertyLine.substring(matcher.end() + 1);
					folder = propertyLine.substring(0, matcher.start() - 1);
					if (file != null) {
						path = file.getAbsolutePath() + File.separator + propertyLine.substring(0, matcher.start() - 1);
					}
				}
			}
			else {
				int index = propertyLine.indexOf(" ");
				if (index != -1) {
					try {
						new URL(propertyLine.substring(0, index));
						int lastIndex = propertyLine.lastIndexOf(" ");
						if (lastIndex != -1) {
							url = propertyLine.substring(0, lastIndex);
							folder = propertyLine.substring(lastIndex + 1);
							if (file != null) {
								path = file.getAbsolutePath() + File.separator + propertyLine.substring(lastIndex + 1);
							}
						}
					} catch (MalformedURLException e) {
						url = propertyLine.substring(index + 1);
						folder = propertyLine.substring(0, index);
						if (file != null) {
							path = file.getAbsolutePath() + File.separator + propertyLine.substring(0, index);
						}
					}
				}
			}
			if (fixedAtRevision == -1 && url != null) {
				int index = url.lastIndexOf("@");
				if (index != -1) {
					try {
						fixedAtRevision = Long.parseLong(url.substring(index + 1));
					} catch (Exception e) {}
				}
			}
			if (path != null) {
				ISVNLocalResource svnResource = getSvnResource(path);	
				if (svnResource != null) {
					try {
						revision = svnResource.getStatus().getLastChangedRevision().getNumber();
					} catch (SVNException e) {}
				}
			}
			if (revision == -1 && fixedAtRevision != -1) {
				revision = fixedAtRevision;
			}
		}
	}

	public File getFile() {
		return file;
	}

	public String getPropertyLine() {
		return propertyLine;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getFolder() {
		return folder;
	}

	public String getPath() {
		return path;
	}
	
	public String getUrl() {
		return url;
	}

	public long getRevision() {
		return revision;
	}

	public long getFixedAtRevision() {
		return fixedAtRevision;
	}
	
	public String toString() {
		if (!selected || revision == -1) {
			return propertyLine;
		}
		else {
			return "-r" + revision + " " + url + " " + folder;
		}
	}
	
    private static ISVNLocalResource getSvnResource(String path) {
    	IResource resource = null;
    	ISVNLocalResource svnResource = null;
    	if (path != null) {
	    	File file = new File(path);
			if (file.exists()) {
				if (file.isDirectory()) {
					resource = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(path));
				}
				else {
					resource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(path));
				}
				if (resource != null) {
					svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				}
			}
    	}
		return svnResource;
    }
}
