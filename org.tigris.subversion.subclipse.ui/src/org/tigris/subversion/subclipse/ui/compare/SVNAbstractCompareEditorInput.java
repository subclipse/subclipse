package org.tigris.subversion.subclipse.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public abstract class SVNAbstractCompareEditorInput extends CompareEditorInput {

	public SVNAbstractCompareEditorInput(CompareConfiguration configuration) {
		super(configuration);
	}
	
	public Control createContents(Composite parent) {
		if (getCompareResult() instanceof String) {		
			
			setMessage("Testing");
			
			Composite composite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));			
			Label iconLabel = new Label(composite, SWT.WRAP);
			iconLabel.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));			
			Label errorLabel = new Label(composite, SWT.WRAP);
			GridData gd = new GridData();
			gd.widthHint = 500;
			errorLabel.setLayoutData(gd);
			errorLabel.setText((String)getCompareResult());
			parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			composite.setBackground(parent.getBackground());
			errorLabel.setBackground(parent.getBackground());
			iconLabel.setBackground(parent.getBackground());
			return composite;
		}
		return super.createContents(parent);
	}

}
