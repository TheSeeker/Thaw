/*
 * Panel.java
 *
 * Created on Четвртак, 2004, Мај 13, 22.19
 */

package com.birosoft.liquid.util;

import java.awt.*;

/**
 *
 * @author  mikeai
 */
public class Panel extends javax.swing.JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6486746209584612751L;
	Image image;
    static Color buttonBg = new Color(215, 231, 249);
    static Color bg = new Color(246, 245, 244);
    
    
    /** Creates a new instance of Panel */
    public Panel(Image i) {
        image = i;
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(image, 10, 100, null);
        //drawIt(g, 10, 10, 48, 48, bg, bg);
    }
    
}
