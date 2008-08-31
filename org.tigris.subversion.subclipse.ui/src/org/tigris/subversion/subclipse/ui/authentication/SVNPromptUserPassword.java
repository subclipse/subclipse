/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.authentication;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword;

public class SVNPromptUserPassword implements ISVNPromptUserPassword {
    private String username;
    private String password;
    private boolean allowedSave;
    private String realm;
    private boolean maySave;
    private boolean rtnCode;
    private int trust;
    private String info;
    private boolean allowPermanently;
    private boolean yesNoAnswer;
    private String question;
    private boolean yesIsDefault;
    private String answer;
    private boolean showAnswer;
    private int sshPort;
    private String keyFile;
    private String passPhrase;

    public SVNPromptUserPassword() {
        super();
    }

    public boolean askYesNo(String realm, String askQuestion, boolean askYesIsDefault) {
		question = askQuestion;
		yesIsDefault = askYesIsDefault;
        SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        int defaultButton = 0;
		        if (!yesIsDefault) defaultButton = 1;
		        MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
		                Policy.bind("SVNPromptUserPassword.authentication"), //$NON-NLS-1$
		                null,
		                question,
		                MessageDialog.QUESTION,
		                new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
		                defaultButton);
		        yesNoAnswer = (dialog.open() == 0);				
			}
		});        
		return yesNoAnswer;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int askTrustSSLServer(String trustInfo, boolean trustAllowPermanently) {
        info = trustInfo;
        allowPermanently = trustAllowPermanently;
		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        TrustSSLServerDialog dialog = new TrustSSLServerDialog(Display.getCurrent().getActiveShell(),
		                info, allowPermanently);
		        switch (dialog.open()) {
		        case TrustSSLServerDialog.REJECT:
		            trust =  ISVNPromptUserPassword.Reject;
		        	break;
		        case TrustSSLServerDialog.TEMPORARY:
		            trust = ISVNPromptUserPassword.AcceptTemporary;
		        	break;
		        case TrustSSLServerDialog.PERMANENT:
		            trust = ISVNPromptUserPassword.AcceptPermanently;
		        	break;
		        default:
		            trust = TrustSSLServerDialog.REJECT;
		        }				
			}
		});        
        return trust;
    }

    public boolean prompt(String promptRealm, String promptUsername, boolean promptMaySave) {
        rtnCode = false;
        username = promptUsername;
        realm = promptRealm;
        maySave = promptMaySave;
   		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        PasswordPromptDialog dialog = new PasswordPromptDialog(SVNUIPlugin.getStandardDisplay().getActiveShell(),
		                realm, username, maySave);
		        if (dialog.open() == PasswordPromptDialog.OK) {
		            username = dialog.getUsername();
		            password = dialog.getPassword();
		            allowedSave = dialog.isSave();
		            rtnCode = true;
		        }				
			}
		});      
        return rtnCode;
    }

    public boolean promptUser(String promptRealm, String promptUsername, boolean promptMaySave) {
        rtnCode = false;
        username = promptUsername;
        realm = promptRealm;
        maySave = promptMaySave;
   		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        UserPromptDialog dialog = new UserPromptDialog(SVNUIPlugin.getStandardDisplay().getActiveShell(),
		                realm, username, maySave);
		        if (dialog.open() == UserPromptDialog.OK) {
		            username = dialog.getUsername();
		            allowedSave = dialog.isSave();
		            rtnCode = true;
		        }				
			}
		});      
        return rtnCode;
    }

    public String askQuestion(String askRealm, String askQuestion, boolean askShowAnswer,
            boolean askMaySave) {
        answer = null;
        realm = askRealm;
        maySave = askMaySave;
        showAnswer = askShowAnswer;
        question = askQuestion;
        if (question.indexOf("certificate") > 0)
        	maySave = false;
		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        QuestionDialog dialog = new QuestionDialog(Display.getCurrent().getActiveShell(),
		                realm, question, showAnswer, maySave);
		        if (dialog.open() == QuestionDialog.OK) {
		            allowedSave = dialog.isSave();
		            answer = dialog.getAnswer();
		        }				
			}
		});        
        return answer;
    }

    public boolean userAllowedSave() {
        return allowedSave;
    }

    public int getSSHPort() {
        return sshPort;
    }
    public String getSSHPrivateKeyPassphrase() {
        return passPhrase;
    }
    public String getSSHPrivateKeyPath() {
        return keyFile;
    }
    public boolean promptSSH(String promptRealm, String promptUsername, int promptPort, boolean promptMaySave) {
        rtnCode = false;
        username = promptUsername;
        realm = promptRealm;
        maySave = promptMaySave;
        sshPort = promptPort;
   		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        SSHPromptDialog dialog = new SSHPromptDialog(SVNUIPlugin.getStandardDisplay().getActiveShell(),
		                realm, username, sshPort, maySave);
		        if (dialog.open() == PasswordPromptDialog.OK) {
		            username = dialog.getUsername();
		            password = dialog.getPassword();
		            sshPort = dialog.getSshPort();
		            keyFile = dialog.getKeyFile();
		            passPhrase = dialog.getPassphrase();
		            allowedSave = dialog.isSave();
		            rtnCode = true;
		        }				
			}
		});      
        return rtnCode;
    }
    public String getSSLClientCertPassword() {
        return passPhrase;
    }
    public String getSSLClientCertPath() {
        return keyFile;
    }
    public boolean promptSSL(String promptRealm, boolean promptMaySave) {
        rtnCode = false;
        realm = promptRealm;
        maySave = promptMaySave;
   		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		        SSLClientCertificate dialog = new SSLClientCertificate(SVNUIPlugin.getStandardDisplay().getActiveShell(),
		                realm, maySave);
		        if (dialog.open() == PasswordPromptDialog.OK) {
		            keyFile = dialog.getKeyFile();
		            passPhrase = dialog.getPassphrase();
		            allowedSave = dialog.isSave();
		            rtnCode = true;
		        }				
			}
		});      
        return rtnCode;
    }
}
