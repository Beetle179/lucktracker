package com.lucktracker;

import com.google.inject.Inject;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class LuckTrackerPanel extends PluginPanel {

    private final int PAD_LENGTH = 18;

    GridBagConstraints resetButtonConstraints = new GridBagConstraints();
    JButton resetButton = new JButton("Reset Tracker");

    GridBagConstraints avgDmgTextConstraints = new GridBagConstraints();
    JLabel avgDmgText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10.3f", "Average Damage:", 0.0D));

    GridBagConstraints totalDmgTextConstraints = new GridBagConstraints();
    JLabel totalDmgText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10d", "Total Damage:", 0));

    GridBagConstraints cdfTextConstraints = new GridBagConstraints();
    JLabel cdfText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10.3f", "CDF:", 0.0D));

    GridBagConstraints tenPercTextConstraints = new GridBagConstraints();
    JLabel tenPercText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10.3f", "10%:", 0.0D));

    GridBagConstraints ninetyPercTextConstraints = new GridBagConstraints();
    JLabel ninetyPercText = new JLabel(String.format("%-" + PAD_LENGTH + "s %10.3f", "90%:", 0.0D));



    @Inject
    LuckTrackerPanel(final LuckTrackerPlugin plugin) {
        JPanel p = new JPanel(new GridBagLayout());

        resetButtonConstraints.anchor = GridBagConstraints.CENTER;
        resetButtonConstraints.insets = new Insets(5, 5, 5, 5);
        resetButtonConstraints.gridx = 0;
        resetButtonConstraints.gridy = 0;
        resetButtonConstraints.gridwidth = 2;
        resetButton.addActionListener(e -> plugin.resetStats());

        totalDmgTextConstraints.anchor = GridBagConstraints.LINE_START;
        totalDmgTextConstraints.insets = new Insets(5, 5, 5, 5);
        totalDmgTextConstraints.gridx = 0;
        totalDmgTextConstraints.gridy = 1;
        totalDmgTextConstraints.gridwidth = 2;

        avgDmgTextConstraints.anchor = GridBagConstraints.LINE_START;
        avgDmgTextConstraints.insets = new Insets(5, 5, 5, 5);
        avgDmgTextConstraints.gridx = 0;
        avgDmgTextConstraints.gridy = 2;
        avgDmgTextConstraints.gridwidth = 2;


        cdfTextConstraints.anchor = GridBagConstraints.LINE_START;
        cdfTextConstraints.insets = new Insets(5, 5, 5, 5);
        cdfTextConstraints.gridx = 0;
        cdfTextConstraints.gridy = 3;
        cdfTextConstraints.gridwidth = 2;

        tenPercTextConstraints.anchor = GridBagConstraints.LINE_START;
        tenPercTextConstraints.insets = new Insets(0,0,0,0);
        tenPercTextConstraints.gridx = 0;
        tenPercTextConstraints.gridy = 4;

        ninetyPercTextConstraints.anchor = GridBagConstraints.LINE_START;
        ninetyPercTextConstraints.insets = new Insets(0,0,0,0);
        ninetyPercTextConstraints.gridx = 0;
        ninetyPercTextConstraints.gridy = 5;

        p.add(resetButton, resetButtonConstraints);
        p.add(totalDmgText, totalDmgTextConstraints);
        p.add(avgDmgText, avgDmgTextConstraints);
        p.add(tenPercText, tenPercTextConstraints);
        p.add(ninetyPercText, ninetyPercTextConstraints);
        p.add(cdfText, cdfTextConstraints);
        add(p);
    }

    public void updatePanelStats(int totalDamage, double avgDmg, double cdf, int tenPercDmg, int ninetyPercDmg) {
        totalDmgText.setText(String.format("%-" + PAD_LENGTH + "s %10d", "Total Damage:", totalDamage));
        avgDmgText.setText(String.format("%-" + PAD_LENGTH + "s %10.3f", "Average Damage:", avgDmg));
        cdfText.setText(String.format("%-" + PAD_LENGTH + "s %10.3f", "CDF:", cdf));
        tenPercText.setText(String.format("%-" + PAD_LENGTH / 2 + "s %10d", "10%:", tenPercDmg));
        ninetyPercText.setText(String.format("%-" + PAD_LENGTH / 2 + "s %10d", "90%:", ninetyPercDmg));
    }
}
