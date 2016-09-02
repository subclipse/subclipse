/*******************************************************************************
 * Copyright (c) 2004, 2006 svnClientAdapter project and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class has been taken from SVNKit 
 */
public class ReaderThread extends Thread {
    
    private final InputStream myInputStream;
    private final OutputStream myOutputStream;

    public ReaderThread(InputStream is, OutputStream os) {
        myInputStream = is;
        myOutputStream = os;
        setDaemon(true);            
    }

    public void run() {
        try {
            while(true) {
                int read = myInputStream.read();
                if (read < 0) {
                    return;
                }
                myOutputStream.write(read);
            }
        } catch (IOException e) {
        } finally {
            try {
            	myInputStream.close();
                myOutputStream.flush();
            } catch (IOException e) {
            	//Just ignore. Stream closing.
            }
        }
    }
}
