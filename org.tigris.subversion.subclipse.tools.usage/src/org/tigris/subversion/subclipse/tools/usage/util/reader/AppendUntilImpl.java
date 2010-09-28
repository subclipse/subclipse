package org.tigris.subversion.subclipse.tools.usage.util.reader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class AppendUntilImpl extends ReadUntilImpl {

	private Writer writer;
	private int numOfWrittenCharacters;

	public AppendUntilImpl(Reader reader, Writer writer, char[] character) {
		super(reader, character);
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