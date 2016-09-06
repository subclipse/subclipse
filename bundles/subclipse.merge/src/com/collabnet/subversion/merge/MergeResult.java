/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class MergeResult implements IPropertySource, Comparable {
	private String action;
	private String propertyAction;
	private String treeConflictAction;
	private String path;
	private boolean error;
	private String conflictResolution = " "; //$NON-NLS-1$
	private String propertyResolution = " "; //$NON-NLS-1$
	private String treeConflictResolution = " "; //$NON-NLS-1$
	private IResource resource;
	private int type = 1;
	private MergeOutput mergeOutput;
	
	public static final String ACTION_CONFLICT = "C"; //$NON-NLS-1$
	public static final String ACTION_CHANGE = "U"; //$NON-NLS-1$
	public static final String ACTION_ADD = "A"; //$NON-NLS-1$
	public static final String ACTION_MERGE = "G"; //$NON-NLS-1$
	public static final String ACTION_DELETE = "D"; //$NON-NLS-1$
	public static final String ACTION_SKIP = "S"; //$NON-NLS-1$
	public static final String ACTION_EXISTING = "E"; //$NON-NLS-1$
	
	public static final int FILE = 1;
	public static final int FOLDER = 2;
	
	public static String P_ID_RESOURCE = "resource"; //$NON-NLS-1$
	public static String P_RESOURCE = Messages.MergeResult_resource;
	public static String P_ID_TEXT_STATUS = "textSts"; //$NON-NLS-1$
	public static String P_TEXT_STATUS = Messages.MergeResult_textStatus;
	public static String P_ID_PROPERTY_STATUS = "propSts"; //$NON-NLS-1$
	public static String P_PROPERTY_STATUS = Messages.MergeResult_propsStatus;
	public static String P_ID_TREE_CONFLICT = "treeConflict"; //$NON-NLS-1$
	public static String P_TREE_CONFLICT = Messages.MergeResult_treeConflict;	
	public static List descriptors;
	public static List descriptorsNoTreeConflict;
	static
	{	
		descriptors = new ArrayList();
		descriptors.add(new PropertyDescriptor(P_ID_RESOURCE, P_RESOURCE));
		descriptors.add(new PropertyDescriptor(P_ID_TEXT_STATUS, P_TEXT_STATUS));
		descriptors.add(new PropertyDescriptor(P_ID_PROPERTY_STATUS, P_PROPERTY_STATUS));
		descriptors.add(new PropertyDescriptor(P_ID_TREE_CONFLICT, P_TREE_CONFLICT));
		descriptorsNoTreeConflict = new ArrayList();
		descriptorsNoTreeConflict.add(new PropertyDescriptor(P_ID_RESOURCE, P_RESOURCE));
		descriptorsNoTreeConflict.add(new PropertyDescriptor(P_ID_TEXT_STATUS, P_TEXT_STATUS));
		descriptorsNoTreeConflict.add(new PropertyDescriptor(P_ID_PROPERTY_STATUS, P_PROPERTY_STATUS));
	}			
	
	public MergeResult(String action, String propertyAction, String treeConflictAction, String path, boolean error) {
		super();
		this.action = action;
		this.propertyAction = propertyAction;
		this.treeConflictAction = treeConflictAction;
		this.path = path;
		this.error = error;
	}

	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}
	
	public boolean isSkip() {
		return action != null && action.equals(MergeResult.ACTION_SKIP);
	}

	public String toString() {
		String tag = null;
		if (error) tag = "Error:   "; //$NON-NLS-1$
		else tag = "Message: "; //$NON-NLS-1$
		return tag + type + " " + action + " " + propertyAction + " " + conflictResolution + " " + propertyResolution + " " + treeConflictAction + " " + treeConflictResolution + " " + path; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	}

	public IResource getResource() {
		return resource;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}	
	
	public String getPropertyAction() {
		return propertyAction;
	}

	public void setPropertyAction(String propertyAction) {
		this.propertyAction = propertyAction;
	}	
	
	public String getTreeConflictAction() {
		return treeConflictAction;
	}
	
	public void setTreeConflictAction(String treeConflictAction) {
		this.treeConflictAction = treeConflictAction;
	}
	
	public boolean isConflicted() {
		return action.equals(ACTION_CONFLICT);
	}
	
	public boolean isPropertyConflicted() {
		return propertyAction.equals(ACTION_CONFLICT);
	}	
	
	public boolean hasTreeConflict() {
		return treeConflictAction != null && treeConflictAction.equals(ACTION_CONFLICT);
	}
	
	public boolean isDelete() {
		return action.equals(ACTION_DELETE);
	}
	
	public boolean isPropertyDelete() {
		return propertyAction.equals(ACTION_DELETE);
	}	
	
	public String getConflictResolution() {
		return conflictResolution;
	}

	public void setConflictResolution(String conflictResolution) {
		this.conflictResolution = conflictResolution;
	}	
	
	public boolean isResolved() {
		return !isConflicted() || (conflictResolution != null && conflictResolution.trim().length() > 0);
	}
	
	public String getPropertyResolution() {
		return propertyResolution;
	}

	public void setPropertyResolution(String propertyResolution) {
		this.propertyResolution = propertyResolution;
	}
	
	public boolean isPropertyResolved() {
		return !isPropertyConflicted() || (propertyResolution != null && propertyResolution.trim().length() > 0);
	}
	
	public String getTreeConflictResolution() {
		return treeConflictResolution;
	}
	
	public void setTreeConflictResolution(String treeConflictResolution) {
		this.treeConflictResolution = treeConflictResolution;
	}
	
	public boolean isTreeConflictResolved() {
		return !hasTreeConflict() || (treeConflictResolution != null && treeConflictResolution.trim().length() > 0);
	}

	public int compareTo(Object compare) {
		if (!(compare instanceof MergeResult)) return 0;
		MergeResult compareResult = (MergeResult)compare;
		return path.compareTo(compareResult.getPath());
	}

	public boolean equals(Object obj) {
		if (obj instanceof MergeResult) {
			MergeResult compareToResult = (MergeResult)obj;
			return compareToResult.getPath().equals(path);
		}
		return super.equals(obj);
	}

	public MergeOutput getMergeOutput() {
		return mergeOutput;
	}

	public void setMergeOutput(MergeOutput mergeOutput) {
		this.mergeOutput = mergeOutput;
	}

	public Object getEditableValue() {
		return resource.getFullPath().toString();
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		return (IPropertyDescriptor[])getDescriptors(hasTreeConflict()).toArray(new IPropertyDescriptor[getDescriptors(hasTreeConflict()).size()]);
	}
	
	private static List getDescriptors(boolean treeConflict) {
		if (treeConflict) return descriptors;
		else return descriptorsNoTreeConflict;
	}		

	public Object getPropertyValue(Object id) {
		if (P_ID_RESOURCE.equals(id)) return resource.getFullPath().toString();
		if (P_ID_TEXT_STATUS.equals(id)) {
			if (action.equals(ACTION_ADD)) return Messages.MergeResult_added;
			if (action.equals(ACTION_CHANGE) || action.equals(ACTION_MERGE)) return Messages.MergeResult_modified;
			if (action.equals(ACTION_DELETE)) return Messages.MergeResult_deleted;
			if (action.equals(ACTION_EXISTING)) return Messages.MergeResult_existing;
//			if (action.equals(ACTION_MERGE)) return "Merged";
			if (action.equals(ACTION_SKIP)) return Messages.MergeResult_skipped;
			if (action.equals(ACTION_CONFLICT)) {
				if (isResolved()) return Messages.MergeResult_resolvedConflict + SVNConflictResolver.getResolutionDescription(conflictResolution) + Messages.MergeResult_33;
				else return Messages.MergeResult_conflicted;
			}
		}
		if (P_ID_PROPERTY_STATUS.equals(id)) {
			if (propertyAction.equals(ACTION_ADD)) return Messages.MergeResult_added;
			if (propertyAction.equals(ACTION_CHANGE) || propertyAction.equals(ACTION_MERGE)) return Messages.MergeResult_modified;
			if (propertyAction.equals(ACTION_DELETE)) return Messages.MergeResult_deleted;
//			if (propertyAction.equals(ACTION_MERGE)) return "Merged";
			if (propertyAction.equals(ACTION_SKIP)) return Messages.MergeResult_skipped;
			if (propertyAction.equals(ACTION_CONFLICT)) {
				if (isPropertyResolved()) return Messages.MergeResult_resolvedConflict + SVNConflictResolver.getResolutionDescription(propertyResolution) + ")"; //$NON-NLS-1$
				else return Messages.MergeResult_conflicted;
			}
		}	
		if (P_ID_TREE_CONFLICT.equals(id)) {
			if (treeConflictAction != null && treeConflictAction.equals(ACTION_CONFLICT)) {
				if (isTreeConflictResolved()) return Messages.MergeResult_resolved;
				else return Messages.MergeResult_unresolved;
			}
			else return ""; //$NON-NLS-1$
		}
		return null;
	}

	public boolean isPropertySet(Object id) {
		return false;
	}

	public void resetPropertyValue(Object id) {
	}

	public void setPropertyValue(Object id, Object value) {
	}

}
