package thaw.plugins.webOfTrust;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;

public class WebOfTrustGraphPanel implements ActionListener {
	private JPanel panel;
	
	private WebOfTrustGraph graph;
	private JScrollPane sp;
	
	private JButton zoomMore;
	private JButton zoomLess;
	
	public WebOfTrustGraphPanel(Hsqldb db) {
		graph = new WebOfTrustGraph(db);
		sp = new JScrollPane(graph);
		sp.getVerticalScrollBar().setUnitIncrement(15);
		graph.setScrollPane(sp);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		zoomMore = new JButton("+");
		zoomMore.addActionListener(this);
		buttonPanel.add(zoomMore);
		zoomLess = new JButton("-");
		zoomLess.addActionListener(this);
		buttonPanel.add(zoomLess);
		
		JPanel northPanel = new JPanel(new BorderLayout());
		
		northPanel.add(new JLabel(I18n.getMessage("thaw.plugin.wot")), BorderLayout.WEST);
		northPanel.add(new JLabel(""), BorderLayout.CENTER);
		northPanel.add(buttonPanel, BorderLayout.EAST);

		panel = new JPanel(new BorderLayout(5, 5));
		panel.add(northPanel, BorderLayout.NORTH);
		panel.add(sp, BorderLayout.CENTER);
	}
	
	public void refresh(Identity i) {
		graph.redraw(i);
	}
	
	public void setVisible(boolean v) {
		graph.setVisible(v);
	}
	
	public JPanel getPanel() {
		return panel;		
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == zoomMore) {
			graph.setZoom(graph.getZoom()*2);
		} else if (e.getSource() == zoomLess) {
			graph.setZoom(graph.getZoom()/2);
		}
	}
}
