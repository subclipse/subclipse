/*******************************************************************************
 * Copyright (c) 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter.utils;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A helper class used for binding NLS supported message strings
 *
 */
public class Messages {
	
    private static final String BUNDLE_NAME = "org.tigris.subversion.svnclientadapter.messages"; //$NON-NLS-1$
	protected static ResourceBundle bundle = null;

	private static ResourceBundle getResourceBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
        }
        return bundle;
    }
	
	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given string.
	 * @param id
	 * @param binding
	 * @return the message with substitutions applied
	 */
	public static String bind(String id, String binding) {
		return bind(id, new String[] { binding });
	}
	
	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given strings.
	 * @param id
	 * @param binding1
	 * @param binding2
	 * @return the message with substitutions applied
	 */
	public static String bind(String id, String binding1, String binding2) {
		return bind(id, new String[] { binding1, binding2 });
	}
	
	/**
	 * Gets a string from the resource bundle. We don't want to crash because of a missing String.
	 * @param key
	 * @return string from the resource bundle or the key if not found.
	 */
	public static String bind(String key) {
		try {
			return getResourceBundle().getString(key);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!"; //$NON-NLS-1$  //$NON-NLS-2$
		}
	}
	
	/**
	 * Gets a string from the resource bundle and binds it with the given arguments. If the key is 
	 * not found, return the key.
	 * @param key
	 * @param args
	 * @return string with substitutions from the resource bundle or the key if not found.
	 */
	public static String bind(String key, Object[] args) {
		try {
			return MessageFormat.format(bind(key), args);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";  //$NON-NLS-1$  //$NON-NLS-2$
		}
	}

}
