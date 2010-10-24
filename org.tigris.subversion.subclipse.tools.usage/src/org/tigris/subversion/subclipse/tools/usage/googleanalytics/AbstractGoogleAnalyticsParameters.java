/*******************************************************************************
 * Copyright (c) 2010 Subclipse project and others.
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

public abstract class AbstractGoogleAnalyticsParameters implements IGoogleAnalyticsParameters {

	private String accountName;
	private String hostName;
	private String referral;
	private String userDefined;

	public AbstractGoogleAnalyticsParameters(String accountName, String hostName, String referral) {
		this(accountName, hostName, referral, null);
	}

	public AbstractGoogleAnalyticsParameters(String accountName, String hostName, String referral, String userDefined) {
		this.accountName = accountName;
		this.hostName = hostName;
		this.referral = referral;
		this.userDefined = userDefined;
	}

	public String getReferral() {
		return referral;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getHostname() {
		return hostName;
	}
	
	public String getUserDefined() {
		return userDefined;
	}
}