/*******************************************************************************
 * Copyright (c) 2007 svnClientAdapter project and others.
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


import java.util.Set;

import org.apache.subversion.javahl.types.Mergeinfo;
import org.tigris.subversion.svnclientadapter.ISVNMergeInfo;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;

public class JhlMergeInfo implements ISVNMergeInfo {
	
	Mergeinfo info;
	
	public JhlMergeInfo() {
		super();
		info = new Mergeinfo();
	}

	public JhlMergeInfo(Mergeinfo info) {
		super();
		this.info = info;
	}

	public void addRevisionRange(String path, SVNRevisionRange range) {
		if (info == null)
			info = new Mergeinfo();
		info.addRevisionRange(path, JhlConverter.convert(range));
	}

	public void addRevisions(String path, SVNRevisionRange[] range) {
		if (info == null)
			info = new Mergeinfo();
		info.addRevisions(path, JhlConverter.convert(range));
	}

	public String[] getPaths() {
		if (info == null)
			return null;
		Set<String> paths = info.getPaths();
		String[] pathArray = new String[paths.size()];
		return paths.toArray(pathArray);
	}

	public SVNRevisionRange[] getRevisionRange(String path) {
		if (info == null)
			return null;
		return JhlConverter.convertRevisionRange(info.getRevisionRange(path));
	}

	public SVNRevisionRange[] getRevisions(String path) {
		if (info == null)
			return null;
		return JhlConverter.convertRevisionRange(info.getRevisions(path));
	}

	public void loadFromMergeInfoProperty(String mergeInfo) {
		if (info == null)
			info = new Mergeinfo();
		info.loadFromMergeinfoProperty(mergeInfo);
	}

}
