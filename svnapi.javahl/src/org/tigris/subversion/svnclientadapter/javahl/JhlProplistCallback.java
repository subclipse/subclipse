package org.tigris.subversion.svnclientadapter.javahl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.subversion.javahl.callback.ProplistCallback;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class JhlProplistCallback implements ProplistCallback {
	private boolean isFile;
	List<ISVNProperty> props;
	
	public JhlProplistCallback(boolean file) {
		isFile = file;
		props = new ArrayList<ISVNProperty>();
	}

	public void singlePath(String path, Map<String, byte[]> properties) {
		Set<String> keys = properties.keySet();
		for (String key : keys) {
			if (isFile) {
				props.add(JhlPropertyData.newForFile(path, key, properties.get(key)));
			} else {
				props.add(JhlPropertyData.newForUrl(path, key, properties.get(key)));
			}
		}
	}
	
	public ISVNProperty[] getPropertyData() {
		ISVNProperty[] propArray = new ISVNProperty[props.size()];
		return props.toArray(propArray);
	}

}
