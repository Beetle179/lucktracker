package com.lucktracker;

import com.google.inject.Inject;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class LuckTrackerPanel extends PluginPanel {

    private final int PAD_LENGTH = 30;

    GridBagConstraints resetButtonConstraints = new GridBagConstraints();
    JButton resetButton = new JButton("Reset Tracker");

    GridBagConstraints avgDmgTextConstraints = new GridBagConstraints();
    JLabel avgDmgText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10.3f", "Average Damage:", 0.0D));

    GridBagConstraints totalDmgTextConstraints = new GridBagConstraints();
    JLabel totalDmgText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10d", "Total Damage:", 0));

    @Inject
    LuckTrackerPanel(final LuckTrackerPlugin plugin) {
        JPanel p = new JPanel(new GridBagLayout());

        resetButtonConstraints.anchor = GridBagConstraints.CENTER;
        resetButtonConstraints.insets = new Insets(10, 10, 10, 10);
        resetButtonConstraints.gridx = 0;
        resetButtonConstraints.gridy = 0;
        resetButton.addActionListener(e -> plugin.resetStats());

        avgDmgTextConstraints.anchor = GridBagConstraints.LINE_START;
        avgDmgTextConstraints.insets = new Insets(10, 10, 10, 10);
        avgDmgTextConstraints.gridx = 0;
        avgDmgTextConstraints.gridy = 2;

        totalDmgTextConstraints.anchor = GridBagConstraints.LINE_START;
        totalDmgTextConstraints.insets = new Insets(10, 10, 10, 10);
        totalDmgTextConstraints.gridx = 0;
        totalDmgTextConstraints.gridy = 1;


        p.add(resetButton, resetButtonConstraints);
        p.add(avgDmgText, avgDmgTextConstraints);
        p.add(totalDmgText, totalDmgTextConstraints);
        add(p);
    }

    public void updatePanelStats(int totalDamage, double avgDmg) {
        totalDmgText.setText(String.format("%-" + PAD_LENGTH + "s %10d", "Total Damage:", totalDamage));
        avgDmgText.setText(String.format("%-" + PAD_LENGTH + "s %10.3f", "Average Damage:", avgDmg));
    }
}
