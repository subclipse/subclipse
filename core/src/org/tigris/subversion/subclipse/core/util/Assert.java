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
 * <code>Assert</code> is useful for for embedding runtime sanity checks
 * in code.
 * The predicate methods all test a condition and throw some
 * type of unchecked exception if the condition does not hold.
 * <p>
 * Assertion failure exceptions, like most runtime exceptions, are
 * thrown when something is misbehaving. Assertion failures are invariably
 * unspecified behavior; consequently, clients should never rely on
 * these being thrown (and certainly should not being catching them
 * specifically).
 * </p>
 */
public final class Assert {
	/* This class is not intended to be instantiated. */
	private Assert() {
	}
	/** Asserts that an argument is legal. If the given boolean is
	 * not <code>true</code>, an <code>IllegalArgumentException</code>
	 * is thrown.
	 *
	 * @param expression the outcode of the check
	 * @return <code>true</code> if the check passes (does not return
	 *    if the check fails)
	 * @exception IllegalArgumentException if the legality test failed
	 */
	public static boolean isLegal(boolean expression) {
		return isLegal(expression, ""); //$NON-NLS-1$
	}
	/** Asserts that an argument is legal. If the given boolean is
	 * not <code>true</code>, an <code>IllegalArgumentException</code>
	 * is thrown.
	 * The given message is included in that exception, to aid debugging.
	 *
	 * @param expression the outcode of the check
	 * @param message the message to include in the exception
	 * @return <code>true</code> if the check passes (does not return
	 *    if the check fails)
	 * @exception IllegalArgumentException if the legality test failed
	 */
	public static boolean isLegal(boolean expression, String message) {
		if (!expression)
			throw new IllegalArgumentException(message);
		return expression;
	}
	/** Asserts that the given object is not <code>null</code>. If this
	 * is not the case, some kind of unchecked exception is thrown.
	 * 
	 * @param object the value to test
	 * @exception IllegalArgumentException if the object is <code>null</code>
	 */
	public static void isNotNull(Object object) {
		isNotNull(object, ""); //$NON-NLS-1$
	}
	/** Asserts that the given object is not <code>null</code>. If this
	 * is not the case, some kind of unchecked exception is thrown.
	 * The given message is included in that exception, to aid debugging.
	 *
	 * @param object the value to test
	 * @param message the message to include in the exception
	 * @exception IllegalArgumentException if the object is <code>null</code>
	 */
	public static void isNotNull(Object object, String message) {
		if (object == null)
			throw new AssertionFailedException("null argument:" + message); //$NON-NLS-1$
	}
	/** Asserts that the given boolean is <code>true</code>. If this
	 * is not the case, some kind of unchecked exception is thrown.
	 *
	 * @param expression the outcode of the check
	 * @return <code>true</code> if the check passes (does not return
	 *    if the check fails)
	 */
	public static boolean isTrue(boolean expression) {
		return isTrue(expression, ""); //$NON-NLS-1$
	}
	/** Asserts that the given boolean is <code>true</code>. If this
	 * is not the case, some kind of unchecked exception is thrown.
	 * The given message is included in that exception, to aid debugging.
	 *
	 * @param expression the outcode of the check
	 * @param message the message to include in the exception
	 * @return <code>true</code> if the check passes (does not return
	 *    if the check fails)
	 */
	public static boolean isTrue(boolean expression, String message) {
		if (!expression)
			throw new AssertionFailedException("assertion failed: " + message); //$NON-NLS-1$
		return expression;
	}
}
