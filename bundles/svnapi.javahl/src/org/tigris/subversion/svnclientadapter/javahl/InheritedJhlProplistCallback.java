/*******************************************************************************
 * Copyright (c) 2004, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter.javahl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.subversion.javahl.callback.InheritedProplistCallback;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class InheritedJhlProplistCallback implements InheritedProplistCallback {
	private boolean isFile;
	List<ISVNProperty> props;

	public InheritedJhlProplistCallback(boolean file) {
		isFile = file;
		props = new ArrayList<ISVNProperty>();
	}
	
	public void singlePath(String path, Map<String, byte[]> properties, Collection<InheritedItem> inherited_properties) {
		if (properties != null) {
			Set<String> keys = properties.keySet();
			for (String key : keys) {
				if (isFile) {
					props.add(JhlPropertyData.newForFile(path, key, properties.get(key)));
				} else {
					props.add(JhlPropertyData.newForUrl(path, key, properties.get(key)));
				}
			}
		}
		if (inherited_properties != null) {
			for (InheritedItem inheritedItem : inherited_properties) {
				Set<String> inheritedKeySet = inheritedItem.properties.keySet();	
				for (String key : inheritedKeySet) {
					if (isFile) {
						props.add(JhlPropertyData.newForFile(inheritedItem.path_or_url, key, inheritedItem.properties.get(key)));
					} else {
						props.add(JhlPropertyData.newForUrl(inheritedItem.path_or_url, key, inheritedItem.properties.get(key)));
					}
				}
			}
		}
	}
	
	public ISVNProperty[] getPropertyData() {
		ISVNProperty[] propArray = new ISVNProperty[props.size()];
		return props.toArray(propArray);
	}

}
