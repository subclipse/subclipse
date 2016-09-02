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
package org.tigris.subversion.svnclientadapter;


/**
 * An interface defining a change path for a log message
 *
 */
public interface ISVNLogMessageChangePath {
	
	/**
	 * Retrieve the path to the commited item
	 * @return  the path to the commited item
	 */
	public abstract String getPath();

	/**
	 * Retrieve the copy source revision if any or null otherwise 
	 * @return  the copy source revision (if any)
	 */
	public abstract SVNRevision.Number getCopySrcRevision();

	/**
	 * Retrieve the copy source path (if any) or null otherwise
	 * @return  the copy source path (if any)
	 */
	public abstract String getCopySrcPath();

	/**
	 * Retrieve action performed.
	 * I.e. 'A'dd, 'D'elete, 'R'eplace, 'M'odify
	 * 
	 * @return  action performed
	 */
	public abstract char getAction();
}