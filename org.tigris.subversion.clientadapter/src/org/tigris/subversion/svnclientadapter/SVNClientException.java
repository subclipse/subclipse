/*******************************************************************************
 * Copyright (c) 2003, 2006 svnClientAdapter project and others.
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

import java.lang.reflect.InvocationTargetException;

/**
 * A generic exception thrown from any {@link ISVNClientAdapter} methods
 *  
 * @author philip schatz
 */
public class SVNClientException extends Exception {
	private int aprError = NONE;

	private static final long serialVersionUID = 1L;
	
	public static final int NONE = -1;
	public static final int MERGE_CONFLICT = 155015;
	public static final int UNSUPPORTED_FEATURE = 200007;
	public static final String OPERATION_INTERRUPTED = "operation was interrupted";

	/**
     * Constructs a new exception with <code>null</code> as its detail message.
	 */
	public SVNClientException() {
		super();
	}

	/**
     * Constructs a new exception with the specified detail message.
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
	 */
	public SVNClientException(String message) {
		super(message);
	}

	/**
     * Constructs a new exception with the specified detail message and
     * cause.
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
	 */
	public SVNClientException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
     * Constructs a new exception with the specified cause.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
	 */
	public SVNClientException(Throwable cause) {
		super(cause);
	}

	/**
	 * Facorty method for creating a delegating/wrapping exception.
	 * @param e exception to wrap SVNClientException around
	 * @return an SVNClientException instance
	 */
	public static SVNClientException wrapException(Exception e) {
		Throwable t = e;
		if (e instanceof InvocationTargetException) {
			Throwable target = ((InvocationTargetException) e).getTargetException();
			if (target instanceof SVNClientException) {
				return (SVNClientException) target;
			}
			t = target;
		}
		return new SVNClientException(t);
	}

	public int getAprError() {
		return aprError;
	}

	public void setAprError(int aprError) {
		this.aprError = aprError;
	}
	
	public boolean operationInterrupted() {
		return getMessage() != null && getMessage().indexOf(OPERATION_INTERRUPTED) != -1;
	}

}
