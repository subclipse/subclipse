package org.tigris.subversion.subclipse.tools.usage.util.reader;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class ReaderUtils {

	/**
	 * Reads from the given input stream until a signal is found. The signal
	 * itself is not appended to the string that is returned.
	 * <p>
	 * This method does not read any further as the maximum number of bytes
	 * given and the end of the stream is reached. If the signal's not found
	 * <tt>null</tt> is returned.
	 * 
	 * @param signal
	 *            the signal
	 * @param maxRead
	 *            the max number of bytes to read
	 * @param reader
	 *            the reader
	 * @return the string that holds the bytes read
	 * @throws Exception
	 */
	public static String readStringUntil(Reader reader, char[] signal) throws IOException {
		Writer writer = new CharArrayWriter();
		AppendUntilImpl visitor = new AppendUntilImpl(reader, writer, signal);
		try {
			visitor.read();
			if (!visitor.isMatching()
					|| visitor.getNumOfWrittenCharacters() == 0) {
				return null;
			}
			writer.flush();
			return writer.toString();
		} finally {
			writer.close();
		}
	}

	public static String readStringUntil(Reader reader, String signal) throws IOException {
		return readStringUntil(reader, signal.toCharArray());
	}

	public static String readStringUntil(Reader reader, String[] alternatives) throws IOException {
		Writer writer = new CharArrayWriter();
		AppendUntilAlternativeImpl visitor = new AppendUntilAlternativeImpl(reader, writer, alternatives);
		try {
			visitor.read();
			if (!visitor.isMatching()
					|| visitor.getNumOfWrittenCharacters() == 0) {
				return null;
			}
			writer.flush();
			return writer.toString();
		} finally {
			writer.close();
		}
	}

	/**
	 * Skips the characters in the given reader until one of the given
	 * alternatives is found.
	 * 
	 * @param maxRead
	 *            the max number of characters to read
	 * @param reader
	 *            the reader to read from
	 * @param alternatives
	 *            the strings to find (alternatively) in the reader
	 * @return the index of the alternative that was found or <tt>-1</tt> if
	 *         none was found
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static String skipUntil(Reader reader, String[] alternatives) throws IOException {
		ReadUntilAlternativesImpl visitor = new ReadUntilAlternativesImpl(reader, alternatives);
		visitor.read();
		return visitor.getAlternative();
	}
}
