package com.lucktracker;

import com.google.inject.Inject;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;

public class LuckTrackerPanel extends PluginPanel {

    @Inject
    LuckTrackerPanel(final LuckTrackerPlugin plugin) {
        JPanel p = new JPanel();
        JButton resetButton = new JButton("Reset Tracker");

        resetButton.addActionListener(e -> plugin.resetRunningHitDist());

        p.add(resetButton);
        add(p);

    }
}
