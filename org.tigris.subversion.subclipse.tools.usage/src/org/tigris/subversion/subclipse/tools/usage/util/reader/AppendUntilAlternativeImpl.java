package org.tigris.subversion.subclipse.tools.usage.util.reader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class AppendUntilAlternativeImpl extends ReadUntilAlternativesImpl {

	private Writer writer;
	private int numOfWrittenCharacters;

	public AppendUntilAlternativeImpl(Reader reader, Writer writer, String[] alternatives) {
		super(reader, alternatives);
		this.writer = writer;
	}

	protected boolean doContinueRead(char character, int numberOfCharactersRead) throws IOException {
		if (super.doContinueRead(character, numberOfCharactersRead)) {
			if (!isMatching()) {
				// don't append matching characters
				writer.write(character);
				numOfWrittenCharacters++;
			}
			return true;
		} else {
			return false;
		}
	}

	public int getNumOfWrittenCharacters() {
		return numOfWrittenCharacters;
	}
}