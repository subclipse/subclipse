package org.tigris.subversion.subclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;

public class RuleredText extends StyledText {
    private int width = 80;

    public RuleredText(Composite parent, int style) {
        super(parent, style); 
        setFont();
    }

    public RuleredText(Composite parent, int style, int width) {
        this(parent, style); 
        this.width = width;
        addPaintListener(getPaintListener());
    }
    
    private void setFont() {
        Font commentFont = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(ISVNUIConstants.SVN_COMMENT_FONT);
        if (commentFont != null) setFont(commentFont);
    }

    private PaintListener getPaintListener() {
        PaintListener listener = new PaintListener() {
            public void paintControl(PaintEvent e) {
                FontMetrics fm = e.gc.getFontMetrics (); 
                Rectangle rect = getClientArea();
                int x1 = rect.x + (fm.getAverageCharWidth() * width) - (fm.getAverageCharWidth() * getHorizontalIndex());
                int y1 = rect.y;
                int x2 = x1;
                int y2 = y1 + rect.height;
                Color saveColor = e.gc.getForeground();
                e.gc.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY));
                e.gc.drawLine(x1, y1, x2, y2);
                e.gc.setForeground(saveColor);
            }           
        };
        return listener;
    }

}
