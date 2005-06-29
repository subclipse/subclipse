package org.tigris.subversion.subclipse.core.resources;

import org.eclipse.core.resources.IFile;

public interface ISVNFileModificationValidatorPrompt {
    
    public boolean prompt(IFile[] files, Object context);
    
    public String getComment();
    
    public boolean isStealLock();

}
