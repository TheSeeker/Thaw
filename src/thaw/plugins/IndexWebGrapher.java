package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Random;


import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Logger;

import thaw.plugins.indexWebGrapher.*;



public class IndexWebGrapher implements thaw.core.Plugin, ActionListener {
	public final static int NMB_STEPS = 10;

	private Core core;

	private Hsqldb db;

	private JPanel tabPanel;

	private JScrollPane scrollPane;
	private GraphPanel graphPanel;

	private JButton compute;
	private JButton zoomIn;
	private JButton zoomOut;
	private JProgressBar progressBar;

	private Random random;

	public boolean run(Core core) {
		this.core = core;

		this.random = new Random();

		/** dep **/

		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		db = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");
		db.registerChild(this);


		/** graphics **/

		tabPanel = new JPanel(new BorderLayout(5, 5));

		JPanel southPanel = new JPanel(new BorderLayout(5, 5));

		compute = new JButton(I18n.getMessage("thaw.plugin.indexWebGrapher.compute"));
		compute.addActionListener(this);

		progressBar = new JProgressBar(0, 100);
		progressBar.setString(I18n.getMessage("thaw.plugin.indexWebGrapher.waiting"));
		progressBar.setStringPainted(true);

		JPanel zoomPanel = new JPanel(new GridLayout(1, 2));
		zoomPanel.add( (zoomOut = new JButton("-")) );
		zoomPanel.add( (zoomIn  = new JButton("+")) );

		zoomOut.addActionListener(this);
		zoomIn.addActionListener(this);

		southPanel.add(compute, BorderLayout.WEST);
		southPanel.add(progressBar, BorderLayout.CENTER);
		southPanel.add(zoomPanel, BorderLayout.EAST);


		graphPanel = new GraphPanel(this);
		scrollPane = new JScrollPane(graphPanel,
					     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					     JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);

		tabPanel.add(scrollPane, BorderLayout.CENTER);
		tabPanel.add(southPanel, BorderLayout.SOUTH);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.indexWebGrapher.shortName"),
					    thaw.gui.IconBox.web,
					    tabPanel);

		return true;
	}


	public JScrollPane getScrollPane() {
		return scrollPane;
	}


	public void setProgress(int step) {
		String txt = "";

		if (step == NMB_STEPS) {
			int fanFan = random.nextInt(5);
			txt = I18n.getMessage("thaw.plugin.indexWebGrapher.fanFan."+Integer.toString(fanFan));
		} else {
			txt = I18n.getMessage("thaw.plugin.indexWebGrapher.computing")
				+ "("+Integer.toString(step)+"/"+Integer.toString(NMB_STEPS)+")";
		}

		progressBar.setString(txt);
		progressBar.setValue( (step * 100) / NMB_STEPS );
	}


	public boolean stop() {
		core.getMainWindow().removeTab(tabPanel);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.indexWebGrapher");
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.web;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == compute) {
			GraphBuilder builder = new GraphBuilder(this, graphPanel, db);
			Thread th = new Thread(builder);
			th.start();
		} else if (e.getSource() == zoomIn) {
			graphPanel.zoomIn();
		} else if (e.getSource() == zoomOut) {
			graphPanel.zoomOut();
		}
	}
}
