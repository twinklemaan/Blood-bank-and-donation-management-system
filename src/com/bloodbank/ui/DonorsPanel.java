package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for registering and viewing donors (Bank Staff).
 */
public class DonorsPanel extends BasePanel {
    public DonorsPanel() {
        add(createHeaderPanel("Donor Registration & Search", "Register new donors and check eligibility"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Donors module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
