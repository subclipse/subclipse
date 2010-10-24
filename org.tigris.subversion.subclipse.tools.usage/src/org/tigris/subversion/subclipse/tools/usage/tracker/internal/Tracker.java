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
package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import org.tigris.subversion.subclipse.tools.usage.http.IHttpGetRequest;
import org.tigris.subversion.subclipse.tools.usage.tracker.ILoggingAdapter;
import org.tigris.subversion.subclipse.tools.usage.tracker.ITracker;
import org.tigris.subversion.subclipse.tools.usage.tracker.IURLBuildingStrategy;

/**
 * Reports (tracks) usage
 * 
 * @see based on <a
 *      href="http://jgoogleAnalytics.googlecode.com">http://jgoogleAnalytics
 *      .googlecode.com</a>
 */
public class Tracker implements ITracker {

	private IURLBuildingStrategy urlBuildingStrategy = null;
	private IHttpGetRequest httpRequest;
	private ILoggingAdapter loggingAdapter;

	public Tracker(IURLBuildingStrategy urlBuildingStrategy, IHttpGetRequest httpGetRequest, ILoggingAdapter loggingAdapter) {
		this.httpRequest = httpGetRequest;
		this.loggingAdapter = loggingAdapter;
		this.urlBuildingStrategy = urlBuildingStrategy;
	}

	public void trackSynchronously(IFocusPoint focusPoint) {
		String[] parameters = { focusPoint.getTitle() };
		loggingAdapter
		.logMessage(MessageFormat.format(TrackerMessages.Tracker_Synchronous, parameters));
		try {
			httpRequest.request(getTrackingUrl(focusPoint));
		} catch (Exception e) {
			String[] errorParameters = { e.getMessage() };
			loggingAdapter.logError(MessageFormat.format(TrackerMessages.Tracker_Error, errorParameters));
		}
	}

	protected String getTrackingUrl(IFocusPoint focusPoint) throws UnsupportedEncodingException {
		return urlBuildingStrategy.build(focusPoint);
	}

	public void trackAsynchronously(IFocusPoint focusPoint) {
		String[] parameters = { focusPoint.getTitle() };
		loggingAdapter.logMessage(MessageFormat
				.format(TrackerMessages.Tracker_Asynchronous, parameters));
		new Thread(new TrackingRunnable(focusPoint)).start();
	}

	private class TrackingRunnable implements Runnable {
		private IFocusPoint focusPoint;

		private TrackingRunnable(IFocusPoint focusPoint) {
			this.focusPoint = focusPoint;
		}

		public void run() {
			try {
				httpRequest.request(getTrackingUrl(focusPoint));
			} catch (Exception e) {
				String[] parameters = { e.getMessage() };
				loggingAdapter.logError(MessageFormat.format(TrackerMessages.Tracker_Error, parameters));
			}
		}
	}
}
