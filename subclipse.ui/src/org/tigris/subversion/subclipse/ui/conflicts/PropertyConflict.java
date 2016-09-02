package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class PropertyConflict {
	private String propertyName;
	private String oldLocalValue;
	private String newLocalValue;
	private String oldIncomingValue;
	private String newIncomingValue;
	
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	public String getOldLocalValue() {
		return oldLocalValue;
	}
	public void setOldLocalValue(String oldLocalValue) {
		this.oldLocalValue = oldLocalValue;
	}
	public String getNewLocalValue() {
		return newLocalValue;
	}
	public void setNewLocalValue(String newLocalValue) {
		this.newLocalValue = newLocalValue;
	}
	public String getOldIncomingValue() {
		return oldIncomingValue;
	}
	public void setOldIncomingValue(String oldIncomingValue) {
		this.oldIncomingValue = oldIncomingValue;
	}
	public String getNewIncomingValue() {
		return newIncomingValue;
	}
	public void setNewIncomingValue(String newIncomingValue) {
		this.newIncomingValue = newIncomingValue;
	}	

	public boolean equals(Object obj) {
		if (obj instanceof PropertyConflict) {
			PropertyConflict compareTo = (PropertyConflict)obj;
			return compareTo.getPropertyName().equals(propertyName);
		}
		return super.equals(obj);
	}
	public static PropertyConflict[] getPropertyConflicts(ISVNLocalResource svnResource) throws Exception {
		PropertyConflict[] propertyConflicts = null;
		String conflictFileContents = getConflictSummary(svnResource);
		if (conflictFileContents != null) {
			List propertyConflictList = new ArrayList();
			ISVNProperty[] properties = svnResource.getSvnProperties();
			for (int i = 0; i < properties.length; i++) {
				if (conflictFileContents.indexOf("property '" + properties[i].getName() + "'") != -1) {
					PropertyConflict conflict = new PropertyConflict();
					conflict.setPropertyName(properties[i].getName());
					propertyConflictList.add(conflict);
				}
			}
			propertyConflicts = new PropertyConflict[propertyConflictList.size()];
			propertyConflictList.toArray(propertyConflicts);
		}
		return propertyConflicts;
	}
	
	public static String getConflictSummary(ISVNLocalResource svnResource) throws Exception {
		String conflictSummary = null;
		IResource resource = svnResource.getResource();
		IResource conflictFile = null;
		if (resource instanceof IContainer) {
			conflictFile = ((IContainer)resource).getFile(new Path("dir_conflicts.prej"));
		} else {
			IContainer parent = resource.getParent();
			if (parent != null) {
				conflictFile = parent.getFile(new Path(resource.getName() + ".prej"));
			}
		}	
		if (conflictFile != null && conflictFile.exists()) {
			conflictSummary = getConflictFileContents(new File(conflictFile.getLocation().toString()));
		}
		return conflictSummary;
	}
	
	private static String getConflictFileContents(File conflictFile) throws IOException {
		StringBuffer fileData = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(conflictFile));
		char[] buf = new char[1024];
		int numRead = 0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();		
		return fileData.toString();
	}

}
