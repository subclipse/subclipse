/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.util;


/**
 * <code>AssertionFailedException</code> is a runtime exception thrown
 * by some of the methods in <code>Assert</code>.
 * <p>
 * This class is not declared public to prevent some misuses; programs that catch 
 * or otherwise depend on assertion failures are susceptible to unexpected
 * breakage when assertions in the code are added or removed.
 * </p>
 */
/* package */
class AssertionFailedException extends RuntimeException {
/** Constructs a new exception.
 */
public AssertionFailedException() {
}
/** Constructs a new exception with the given message.
 */
public AssertionFailedException(String detail) {
	super(detail);
}
}
