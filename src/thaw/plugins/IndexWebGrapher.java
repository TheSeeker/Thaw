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
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;

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
	private JButton refresh;
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

		JPanel zoomPanel = new JPanel(new GridLayout(1, 3));
		zoomPanel.add( (refresh = new JButton("", thaw.gui.IconBox.minRefreshAction)) );
		zoomPanel.add( (zoomOut = new JButton("-")) );
		zoomPanel.add( (zoomIn  = new JButton("+")) );

		zoomOut.addActionListener(this);
		zoomIn.addActionListener(this);
		refresh.addActionListener(this);

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


	public void stop() {
		core.getMainWindow().removeTab(tabPanel);
		db.unregisterChild(this);
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.indexWebGrapher");
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.web;
	}

	private GraphBuilder lastBuilder = null;

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == compute) {
			if (lastBuilder == null || lastBuilder.isFinished()) {
				compute.setText(I18n.getMessage("thaw.plugin.indexWebGrapher.faster"));

				lastBuilder = new GraphBuilder(this, graphPanel, db);
				Thread th = new ThawThread(lastBuilder, "Web graph optimizer", this);
				th.start();
			} else {
				if (!lastBuilder.fasterFlag()) {
					compute.setText(I18n.getMessage("thaw.plugin.indexWebGrapher.stop"));
					lastBuilder.setFasterFlag(true);
				} else {
					compute.setText(I18n.getMessage("thaw.plugin.indexWebGrapher.compute"));
					lastBuilder.stop();
				}
			}

		} else if (e.getSource() == zoomIn) {

			graphPanel.zoomIn();

			scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getValue()*2);
			scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getValue()*2);

		} else if (e.getSource() == zoomOut) {

			scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getValue()/2);
			scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getValue()/2);

			graphPanel.zoomOut();

		} else if (e.getSource() == refresh) {
			graphPanel.refresh();
		}
	}


	public void endOfProcess() {
		compute.setText(I18n.getMessage("thaw.plugin.indexWebGrapher.compute"));
	}
}
