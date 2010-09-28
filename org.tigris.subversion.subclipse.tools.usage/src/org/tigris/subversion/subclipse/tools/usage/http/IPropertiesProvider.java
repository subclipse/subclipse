package org.tigris.subversion.subclipse.tools.usage.http;

import java.io.IOException;
import java.util.Map;

public interface IPropertiesProvider {

	public Map getMap() throws IOException;

}