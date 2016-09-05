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
package org.tigris.subversion.subclipse.tools.usage.util.reader;

import java.io.Reader;

public class ReadUntilAlternativesImpl extends ReadUntilImpl {

	private char[][] allAlternatives;
	private char[] currentAlternative;
	private int alternativesIndex = -1;

	public ReadUntilAlternativesImpl(Reader reader, String[] stringAlternatives) {
		super(reader, new char[0]);
		initAlternativesCharSequences(stringAlternatives);
	}

	private void initAlternativesCharSequences(String[] stringAlternatives) {
		this.allAlternatives = new char[stringAlternatives.length][];
		for (int i = 0; i < stringAlternatives.length; i++) {
			this.allAlternatives[i] = stringAlternatives[i].toCharArray();
		}
	}

	protected int getNumberOfCharactersToMatch() {
		if (currentAlternative != null) {
			return currentAlternative.length;
		} else {
			return 0;
		}
	}

	protected boolean doesMatch(char character) {
		if (currentAlternative == null || currentAlternative[getMatchingIndex()] != character) {
			// current alternative does not match new character, select a new
			// alternative
			boolean newAlternativeSelected = matchAlternative(character);
			if (!newAlternativeSelected) {
				// no alternative matches current character + new one
				setMatchingIndex(0);
			}
			return newAlternativeSelected;
		} else {
			return true;
		}
	}

	/**
	 * Returns whether the given character matches an alternative (in other
	 * words the given character matches an alternative at the current matching
	 * index).
	 * 
	 * @param character
	 *            the character
	 * @return true, if successful
	 */
	private boolean matchAlternative(char character) {
		for (int i = alternativesIndex + 1; i < allAlternatives.length; i++) {
			char[] alternative = allAlternatives[i];
			if (doesMatch(character, alternative)) {
				this.currentAlternative = alternative;
				this.alternativesIndex = i;
				return true;
			}

		}
		this.currentAlternative = null;
		this.alternativesIndex = -1;
		return false;
	}

	/**
	 * Returns whether the given potentially matching alternative (String)
	 * matches the currently selected alternative and the additional character.
	 * 
	 * @param character
	 * @param potentiallyMatchingAlternative
	 *            the new alternative that could match
	 * @return
	 */
	private boolean doesMatch(char character, char[] potentiallyMatchingAlternative) {
		int currentMatchingIndex = getMatchingIndex();
		for (int j = 0; j < currentMatchingIndex; j++) {
			if (potentiallyMatchingAlternative[j] != currentAlternative[j]) {
				return false;
			}
		}
		return potentiallyMatchingAlternative[currentMatchingIndex] == character;
	}

	protected char[] getCharactersToMatch() {
		return currentAlternative;
	}

	public String getAlternative() {
		if (alternativesIndex >= 0) {
			return new String(allAlternatives[alternativesIndex]);
		} else {
			return null;
		}
	}
}