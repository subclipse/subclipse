package org.tigris.subversion.subclipse.ui.conflicts;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.tigris.subversion.subclipse.core.util.StringMatcher;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class MergeFileAssociation implements Comparable {
	private String fileType;
	private int type;
	private String mergeProgram;
	private String parameters;
	
	public final static int BUILT_IN = 0;
	public final static int DEFAULT_EXTERNAL = 1;
	public final static int CUSTOM_EXTERNAL = 2;
	
	public static final String PREF_MERGE_FILE_ASSOCIATIONS_NODE = "mergeFileAssociations"; //$NON-NLS-1$
	
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getMergeProgram() {
		return mergeProgram;
	}
	public void setMergeProgram(String mergeProgram) {
		this.mergeProgram = mergeProgram;
	}
	public String getParameters() {
		return parameters;
	}
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
	
	public static Preferences getParentPreferences() {
		return SVNUIPlugin.getPlugin().getInstancePreferences().node(PREF_MERGE_FILE_ASSOCIATIONS_NODE);
	}
	
	public boolean matches(String fileName) {
		StringMatcher stringMatcher = new StringMatcher(getFileType(), false, false);
		return stringMatcher.match(fileName);
	}
	
	public int compareTo(Object object) {
		if (!(object instanceof MergeFileAssociation)) toString().compareTo(object.toString());
		MergeFileAssociation compareTo = (MergeFileAssociation)object;
		return fileType.compareTo(compareTo.getFileType());
	}
	
	public boolean equals(Object object) {
		if (!(object instanceof MergeFileAssociation)) return false;
		MergeFileAssociation compareTo = (MergeFileAssociation)object;
		return fileType.equals(compareTo.getFileType());
	}
	
	public int hashCode() {
		return fileType.hashCode();
	}
	
	public boolean remove() {
		Preferences node = MergeFileAssociation.getParentPreferences().node(getFileType());
		if (node != null) {
			try {
				node.removeNode();
			} catch (BackingStoreException e) {
			}
		}
		return false;
	}

}
