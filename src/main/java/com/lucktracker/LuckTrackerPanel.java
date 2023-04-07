package com.lucktracker;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;

public class LuckTrackerPanel extends PluginPanel {
    void init() {
        JPanel p = new JPanel();
        JButton resetButton = new JButton("Reset Tracker");

        p.add(resetButton);
        add(p);
    }
}
