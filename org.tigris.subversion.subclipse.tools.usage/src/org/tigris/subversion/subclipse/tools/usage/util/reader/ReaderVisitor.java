package org.tigris.subversion.subclipse.tools.usage.util.reader;

public interface ReaderVisitor {
	public boolean continueRead(char character, int numberOfCharactersRead) throws Exception;
	public int getNumberOfCharactersRead();
}
