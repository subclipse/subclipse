package org.tigris.subversion.subclipse.core;

public interface IMessageHandler {
	public static final int INFO = 0;
	public static final int WARNING = 1;
	public static final int ERROR = 2;
	
	public void handleMessage(String title, String message, int severity);
	
	public boolean handleQuestion(String title, String question);

}
