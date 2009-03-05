package org.tigris.subversion.subclipse.ui;

import org.eclipse.swt.widgets.Combo;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;

public class DepthComboHelper {
	private static String[] comboValues = { 
		ISVNUIConstants.DEPTH_UNKNOWN,
		ISVNUIConstants.DEPTH_INFINITY,
		ISVNUIConstants.DEPTH_IMMEDIATES,
		ISVNUIConstants.DEPTH_FILES,
		ISVNUIConstants.DEPTH_EMPTY,
		ISVNUIConstants.DEPTH_EXCLUDE
	};
	
	private static int[] depthValues = {
		ISVNCoreConstants.DEPTH_UNKNOWN,
		ISVNCoreConstants.DEPTH_INFINITY,
		ISVNCoreConstants.DEPTH_IMMEDIATES,
		ISVNCoreConstants.DEPTH_FILES,
		ISVNCoreConstants.DEPTH_EMPTY,
		ISVNCoreConstants.DEPTH_EXCLUDE
	};

	public static void addDepths(Combo combo, boolean includeUnknown, String defaultSelection) {
		if (includeUnknown) combo.add(ISVNUIConstants.DEPTH_UNKNOWN);
		combo.add(ISVNUIConstants.DEPTH_INFINITY);
		combo.add(ISVNUIConstants.DEPTH_IMMEDIATES);
		combo.add(ISVNUIConstants.DEPTH_FILES);
		combo.add(ISVNUIConstants.DEPTH_EMPTY);
		if (defaultSelection != null) combo.select(combo.indexOf(defaultSelection));
	}
	
	public static void addDepths(Combo combo, boolean includeUnknown, boolean includeExclude, String defaultSelection) {
		if (includeUnknown) combo.add(ISVNUIConstants.DEPTH_UNKNOWN);
		combo.add(ISVNUIConstants.DEPTH_INFINITY);
		combo.add(ISVNUIConstants.DEPTH_IMMEDIATES);
		combo.add(ISVNUIConstants.DEPTH_FILES);
		combo.add(ISVNUIConstants.DEPTH_EMPTY);
		if (includeExclude) combo.add(ISVNUIConstants.DEPTH_EXCLUDE);
		if (defaultSelection != null) combo.select(combo.indexOf(defaultSelection));
	}	
	
	public static int getDepth(Combo combo) {
		for (int i = 0; i < comboValues.length; i++) {
			if (combo.getText().equals(comboValues[i])) return depthValues[i];
		}
		return ISVNCoreConstants.DEPTH_UNKNOWN;
	}
	
}
