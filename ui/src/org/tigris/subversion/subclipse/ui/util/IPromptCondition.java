/*
 * Created on Feb 18, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.tigris.subversion.subclipse.ui.util;

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.eclipse.core.resources.IResource;

/**
 * Input to a confirm prompt
 * 
 * @see PromptingDialog
 */
public interface IPromptCondition {
	/**
	 * Answers <code>true</code> if a prompt is required for this resource and
	 * false otherwise.
	 */
	public boolean needsPrompt(IResource resource);
	
	/**
	 * Answers the message to include in the prompt.
	 */
	public String promptMessage(IResource resource);
}

