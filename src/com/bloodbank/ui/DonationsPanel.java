package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for recording blood donations (Bank Staff).
 */
public class DonationsPanel extends BasePanel {
    public DonationsPanel() {
        add(createHeaderPanel("Record Blood Donations", "Record donation intake with eligibility and health metrics verification"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Donations module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
