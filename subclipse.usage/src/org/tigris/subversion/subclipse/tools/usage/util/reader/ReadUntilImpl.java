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

import java.io.IOException;
import java.io.Reader;

public class ReadUntilImpl implements ReaderVisitor {

	/** returned by a stream if the end of the stream is reached. */
	private static final char EOS = (char) -1;

	private Reader reader;
	private boolean matched = false;
	private int numberOfCharactersRead;
	private char[] characters;
	private int matchingCharactersIndex = 0;

	public ReadUntilImpl(Reader reader, char[] characters) {
		this.reader = reader;
		this.numberOfCharactersRead = 0;
		this.characters = characters;
	}

	public final boolean continueRead(char character, int numberOfCharactersRead) throws IOException {
		this.numberOfCharactersRead = numberOfCharactersRead;
		return doContinueRead(character, numberOfCharactersRead);
	}

	protected boolean doContinueRead(char character, int numberOfCharactersRead) throws IOException {
		boolean continueRead = false;
		boolean matches = doesMatch(character);
		if (!matches) {
			continueRead = !matches;
		} else {
			int matchingIndex = getMatchingIndex() + 1;
			setMatchingIndex(matchingIndex);
			continueRead = matches
					&& matchingIndex < getNumberOfCharactersToMatch();
		}

		setMatches(matches);
		return continueRead;
	}
	
	public boolean isMatching() {
		return matched;
	}

	public int getNumberOfCharactersRead() {
		return this.numberOfCharactersRead;
	}

	public void read() throws IOException {
		char character = 0;
		while ((character = (char) reader.read()) != EOS) {
			if (!continueRead(character, ++numberOfCharactersRead)) {
				return;
			}
		}
	}
	
	protected void setMatchingIndex(int index) {
		this.matchingCharactersIndex = index;
	}
	
	protected int getMatchingIndex() {
		return matchingCharactersIndex;
	}
	
	protected boolean doesMatch(char character) {
		return characters[matchingCharactersIndex] == character;
	}

	protected int getNumberOfCharactersToMatch() {
		return characters.length;
	}
	
	protected char[] getCharactersToMatch() {
		return characters;
	}

	protected void setMatches(boolean matches) {
		matched = matches;
	}
}