package org.tigris.subversion.subclipse.core.resources;

public interface ISVNFileModificationValidatorPrompt {
    
    public boolean prompt(Object context);
    
    public String getComment();
    
    public boolean isStealLock();

}
