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

    GridBagConstraints cdfTextConstraints = new GridBagConstraints();
    JLabel cdfText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10.3f", "CDF:", 0.0D));

    @Inject
    LuckTrackerPanel(final LuckTrackerPlugin plugin) {
        JPanel p = new JPanel(new GridBagLayout());

        resetButtonConstraints.anchor = GridBagConstraints.CENTER;
        resetButtonConstraints.insets = new Insets(10, 10, 10, 10);
        resetButtonConstraints.gridx = 0;
        resetButtonConstraints.gridy = 0;
        resetButton.addActionListener(e -> plugin.resetStats());

        totalDmgTextConstraints.anchor = GridBagConstraints.LINE_START;
        totalDmgTextConstraints.insets = new Insets(10, 10, 10, 10);
        totalDmgTextConstraints.gridx = 0;
        totalDmgTextConstraints.gridy = 1;

        avgDmgTextConstraints.anchor = GridBagConstraints.LINE_START;
        avgDmgTextConstraints.insets = new Insets(10, 10, 10, 10);
        avgDmgTextConstraints.gridx = 0;
        avgDmgTextConstraints.gridy = 2;


        cdfTextConstraints.anchor = GridBagConstraints.LINE_START;
        cdfTextConstraints.insets = new Insets(10, 10, 10, 10);
        cdfTextConstraints.gridx = 0;
        cdfTextConstraints.gridy = 3;


        p.add(resetButton, resetButtonConstraints);
        p.add(totalDmgText, totalDmgTextConstraints);
        p.add(avgDmgText, avgDmgTextConstraints);
        p.add(cdfText, cdfTextConstraints);
        add(p);
    }

    public void updatePanelStats(int totalDamage, double avgDmg, double cdf) {
        totalDmgText.setText(String.format("%-" + PAD_LENGTH + "s %10d", "Total Damage:", totalDamage));
        avgDmgText.setText(String.format("%-" + PAD_LENGTH + "s %10.3f", "Average Damage:", avgDmg));
        cdfText.setText(String.format("%-" + PAD_LENGTH + "s %10.3f", "CDF:", cdf));
    }
}
