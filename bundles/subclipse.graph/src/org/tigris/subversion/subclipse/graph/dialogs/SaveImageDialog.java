package org.tigris.subversion.subclipse.graph.dialogs;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.graph.Activator;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;

public class SaveImageDialog extends TrayDialog {
	private RevisionGraphEditor editor;
	private Combo fileTypeCombo;
	private Text fileText;
	private Button browseButton;
	private int lastOutput;
	
	private IDialogSettings settings = Activator.getDefault().getDialogSettings();
	
	private Button okButton;
	
	private static final int BMP = 0;
	private static final int JPEG = 1;
	private static final int PNG = 2;
	private final static String LAST_OUTPUT = "SaveImageDialog.lastOutput";

	public SaveImageDialog(Shell parentShell, RevisionGraphEditor editor) {
		super(parentShell);
		this.editor = editor;
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
		try {
			lastOutput = settings.getInt(LAST_OUTPUT);
		} catch (Exception e) {}
	}

	protected Control createDialogArea(Composite parent) {
		getShell().setText("Save Image to File");
		
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label typeLabel = new Label(composite, SWT.NONE);
		typeLabel.setText("Save as file type:");
		fileTypeCombo = new Combo(composite, SWT.READ_ONLY);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fileTypeCombo.setLayoutData(gd);
		fileTypeCombo.add("BMP");
		fileTypeCombo.add("JPEG");
		fileTypeCombo.add("PNG");

		Label fileLabel = new Label(composite, SWT.NONE);
		fileLabel.setText("Save to file:");
		fileText = new Text(composite, SWT.BORDER);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		gd.widthHint = 300;
		fileText.setLayoutData(gd);
		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				selectFile();
			}			
		});
		
		switch (lastOutput) {
		case BMP:
			fileTypeCombo.setText("BMP");
			break;
		case JPEG:
			fileTypeCombo.setText("JPEG");
			break;
		case PNG:
			fileTypeCombo.setText("PNG");
			break;				
		default:
			fileTypeCombo.setText("BMP");
			break;
		}
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				okButton.setEnabled(canFinish());
			}		
		};
		fileText.addModifyListener(modifyListener);
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		fileText.addFocusListener(focusListener);
		
		return composite;
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
			okButton.setEnabled(false);
		}
        return button;
    }
	
	protected void okPressed() {
		settings.put(LAST_OUTPUT, fileTypeCombo.getSelectionIndex());
		
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				GraphicalViewer viewer = editor.getViewer();
				ScalableRootEditPart rootEditPart = (ScalableRootEditPart)viewer.getEditPartRegistry().get(LayerManager.ID);
				IFigure rootFigure = ((LayerManager)rootEditPart).getLayer(LayerConstants.PRINTABLE_LAYERS);
				Rectangle rootFigureBounds = rootFigure.getBounds();	
				Control figureCanvas = viewer.getControl();
				GC figureCanvasGC = new GC(figureCanvas);
				Image img = new Image(null, rootFigureBounds.width, rootFigureBounds.height);
				GC imageGC = new GC(img);
				imageGC.setBackground(figureCanvasGC.getBackground());
				imageGC.setForeground(figureCanvasGC.getForeground());
				imageGC.setFont(figureCanvasGC.getFont());
				imageGC.setLineStyle(figureCanvasGC.getLineStyle());
				imageGC.setLineWidth(figureCanvasGC.getLineWidth());
//				imageGC.setXORMode(figureCanvasGC.getXORMode());
				Graphics imgGraphics = new SWTGraphics(imageGC);

				rootFigure.paint(imgGraphics);

				ImageData[] imgData = new ImageData[1];
				imgData[0] = img.getImageData();

				ImageLoader imgLoader = new ImageLoader();
				imgLoader.data = imgData;
				String extension;
				if (fileTypeCombo.getText().equals("JPEG")) extension = "jpg";
				else extension = fileTypeCombo.getText().toLowerCase();
				String fileName = null;
				if (!fileText.getText().trim().endsWith("." + extension))
					fileName = fileText.getText().trim() + "." + extension;
				else
					fileName = fileText.getText().trim();
				int imageType = SWT.IMAGE_BMP;
				switch (fileTypeCombo.getSelectionIndex()) {
				case BMP:
					imageType = SWT.IMAGE_BMP;
					break;
				case JPEG:
					imageType = SWT.IMAGE_JPEG;
					break;	
				case PNG:
					imageType = SWT.IMAGE_PNG;
					break;					
				default:
					break;
				}				
				imgLoader.save(fileName, imageType);

				figureCanvasGC.dispose();
				imageGC.dispose();
				img.dispose();
			}		
		});		
		
		super.okPressed();
	}

	private boolean canFinish() {
		if (fileText.getText().trim().length() == 0) return false;
		File file = new File(fileText.getText().trim());
		return isValidFile(file);
	}
	
	private boolean isValidFile(File file) {
		if (!file.isAbsolute()) return false;
		if (file.isDirectory()) return false;
		File parent = file.getParentFile();
		if (parent==null) return false;
		if (!parent.exists()) return false;
		if (!parent.isDirectory()) return false;
		return true;
	}
	
	private void selectFile() {
		String extension;
		if (fileTypeCombo.getText().equals("JPEG")) extension = "jpg";
		else extension = fileTypeCombo.getText().toLowerCase();
		FileDialog d = new FileDialog(getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
		d.setText("Save Revision Graph As");
		d.setFileName(editor.getEditorInput().getName() + "." + extension);
		String file = d.open();
		if(file!=null) {
			IPath path = new Path(file);
			fileText.setText(path.toOSString());
		}						
	}

}
