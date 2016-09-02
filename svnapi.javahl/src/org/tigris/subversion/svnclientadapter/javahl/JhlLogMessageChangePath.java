/*******************************************************************************
 * Copyright (c) 2006 svnClientAdapter project and others.
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

import org.apache.subversion.javahl.types.ChangePath;
import org.tigris.subversion.svnclientadapter.SVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * JavaHL specific implementation of the {@link org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath}. 
 * Actually just an adapter from {@link org.tigris.subversion.javahl.ChangePath}
 * 
 */
public class JhlLogMessageChangePath extends SVNLogMessageChangePath {
	
	/**
	 * Constructor
	 * @param changePath
	 */
	public JhlLogMessageChangePath(ChangePath changePath) {
		super(
				changePath.getPath(),
				(changePath.getCopySrcRevision() != -1) ? new SVNRevision.Number(
						changePath.getCopySrcRevision()) : null, 
				changePath.getCopySrcPath(), 
				JhlConverter.convert(changePath.getAction()));
	}

}
