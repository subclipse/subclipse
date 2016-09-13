package org.tigris.subversion.subclipse.ui.authentication;

import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.util.ListContentProvider;

public class SSLClientCertificatesMSCapi extends ListDialog {
	protected String alias;

	/**
     *
	 * @param parent
     * @param url : the url from which we want to get the root url
	 */

	public SSLClientCertificatesMSCapi(Shell parent, String realm) {
		super(parent);
		// List<String[]> list = new ArrayList<String[]>();
		List list = new ArrayList();
		Provider pmscapi = Security.getProvider("SunMSCAPI"); //$NON-NLS-1$
		Provider pjacapi = Security.getProvider("CAPI"); //$NON-NLS-1$
		try {
			KeyStore keyStore = null;
			//use JACAPI
			if (pmscapi != null) {
				keyStore = KeyStore.getInstance("Windows-MY",pmscapi); //$NON-NLS-1$
				pmscapi.setProperty("Signature.SHA1withRSA","sun.security.mscapi.RSASignature$SHA1"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (pjacapi != null) {
				keyStore = KeyStore.getInstance("CAPI"); //$NON-NLS-1$
			}
	        if (keyStore != null) {
	            keyStore.load(null, null);
	            //for (Enumeration<String> aliasEnumeration = keyStore.aliases();aliasEnumeration.hasMoreElements();) {
	            for (Enumeration aliasEnumeration = keyStore.aliases();aliasEnumeration.hasMoreElements();) {
	            	String alias = (String) aliasEnumeration.nextElement();
	            	String issuer = ""; //$NON-NLS-1$
	            	Certificate cert = keyStore.getCertificate(alias);
	            	if (cert instanceof X509Certificate) {
	            		issuer = ((X509Certificate) cert).getIssuerDN().getName();
	            	}
	            	list.add(new String[]{alias,issuer});
	            	//keyStore.getCertificate(alias)
	            }
	        }
		} catch (Exception e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
        setTitle(Policy.bind("SSLClientCertificatesMSCapi.0"));
        setAddCancelButton(true);
        LabelProvider lp = new LabelProvider(){
        	public String getText(Object element) {
        		if (element == null) {
        			return ""; //$NON-NLS-1$
        		} else if (element instanceof String[] && ((String[]) element).length > 1) {
        			return ((String[]) element)[0] + " | issued by: " + ((String[]) element)[1]; //$NON-NLS-1$
        		} else {
        			return element.toString();
        		}
        	}
        };
        setLabelProvider(lp);
        setMessage(Policy.bind("SSLClientCertificatesMSCapi.1"));
        setContentProvider(new ListContentProvider());
        setInput(list);
	}


	public String getAlias() {
		if (getResult() != null && getResult().length>0) {
			Object result = getResult()[0];
			if (result instanceof String[]) {
				this.alias = ((String[]) result)[0];
			} else {
				this.alias = (String) result;
			}
		}
		return alias;
	}
}
