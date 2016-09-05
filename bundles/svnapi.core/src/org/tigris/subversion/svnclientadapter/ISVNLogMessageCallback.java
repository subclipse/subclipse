/*******************************************************************************
 * Copyright (c) 2008 svnClientAdapter project and others.
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
 * This interface is used to receive every log message for the log
 * messages found by a ISVNClientAdapter.getLogMessages call.
 *
 * All log messages are returned in a list, which is terminated by an
 * invocation of this callback with the message set to NULL.
 *
 * If the includeMergedRevisions parameter to ISVNClientAdapter.getLogMessages
 * is true, then messages returned through this callback may have the
 * hasChildren parameter set.  This parameter indicates that a separate list,
 * which includes messages for merged revisions, will immediately follow.
 * This list is also terminated with NULL, after which the
 * previous log message list continues.
 *
 * Log message lists may be nested arbitrarily deep, depending on the ancestry
 * of the requested paths.
 */
public interface ISVNLogMessageCallback {
	
    /**
     * The method will be called for every log message.
     *
     * @param message   the log message
     */
    public void singleMessage(ISVNLogMessage message);
}
